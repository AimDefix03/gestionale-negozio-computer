# Historical database fixtures

These fixtures contain deterministic, synthetic data for migration and reconciliation tests. They do not contain production exports, valid credentials or personal data.

## Composition

- V14: migrations V1-V14, `v14/base.sql`, then the selected volume profile.
- V16: V14 composition, migrations V15-V16, then `v16/delta.sql`.
- V18: V16 composition, migrations V17-V18, then `v18/delta.sql`.
- V22: V18 composition, migrations V19-V22, with stable identity, ownership, payment quarantine and numbering assertions.
- V30: a populated V14, V16 or V18 path upgraded through V22, checked for known canonical collisions, reconciled with the approved synthetic remediation, then migrated through V23-V30. V30 preserves unknown historical sales channels as unresolved evidence and adds the order-line description snapshot field.
- V31: the populated V30 path upgraded with non-destructive account lifecycle state, consistency constraints and an enabled default for historical accounts.
- V32: the populated V31 path upgraded with the company IANA time zone and a conservative UTC snapshot for historical documents whose original time zone is unknown.
- V33: the populated V32 path upgraded with additive supplier-order tables, sequences and constraints. Historical sales and inventory data are deliberately not inferred as purchases.
- V34: the populated V33 path upgraded with receipt posting, inventory-cost coverage and ledger constraints. Historical stock remains explicitly uncosted and no selling price is treated as purchase cost.

The incremental model reproduces real upgrades instead of presenting future tables as if they existed in an older schema.

## Volume profiles

| Profile | Products | Partners | Orders |
|---|---:|---:|---:|
| small | 20 | 10 | 40 |
| medium | 500 | 250 | 5,000 |
| large | 5,000 | 2,500 | 50,000 |

Targeted records in `v14/base.sql` are added to these quantities.

## Verification

From the repository root:

```bash
scripts/db/verify-historical-fixtures.sh small
```

The script starts one uniquely named PostgreSQL container without host ports or named volumes, creates clean databases for each historical target through V22, loads the fixture chain, runs assertions and removes only that container.

To exercise every populated upgrade path through the current schema:

```bash
scripts/db/verify-historical-fixtures.sh small upgrades
```

The V23 canonical-identifier migration intentionally refuses ambiguous historical data. The verifier first proves that the fixture contains the three known collisions, applies `remediation/pre-v23-canonical-resolution.sql`, verifies that stable references and textual snapshots are preserved, and only then continues to V34. This models an explicit operator-approved remediation instead of hiding ambiguity inside a migration.

For the scheduled volumetric gate:

```bash
scripts/db/verify-historical-fixtures.sh large upgrade-v18
```

`SHA256SUMS` protects every fixture, remediation, assertion, profile and manifest used by the verifier.
