# Product Scope

## Visione

Gestionale Negozio Computer deve evolvere da progetto didattico a gestionale operativo per piccole attivita IT: negozi di computer, assistenza tecnica, rivendita hardware/software e gestione interna di prodotti, ordini e magazzino.

L'obiettivo non e costruire subito un ERP completo, ma un prodotto credibile, modulare e dimostrabile in modo professionale.

## Utenti principali

- Super admin: governa configurazione iniziale e puo creare altri admin.
- Admin: gestisce catalogo, magazzino, ordini, documenti simulati, account e audit secondo permessi assegnati.
- Dipendente: opera su catalogo, magazzino, ordini e documenti operativi.
- Cliente: consulta catalogo e crea ordini/acquisti simulati.

## Perimetro core iniziale

- Autenticazione e gestione sessione.
- Ruoli e permessi lato backend.
- Catalogo prodotti con brand, categoria tecnica, tipo prodotto e utilizzo opzionale.
- Movimenti di magazzino con storico.
- Ordini con righe prodotto e scarico stock.
- Documenti simulati collegati agli ordini, esplicitamente non fiscali.
- Configurazione aziendale e numerazioni documentali simulate per tipo ed esercizio.
- Pagamenti gestionali, rimborsi e resi senza integrazione con PSP o banche.
- Audit log delle operazioni sensibili.
- Dashboard operativa.
- Report interni vendite e magazzino con filtri ed export CSV, Excel e PDF.

## Fuori perimetro nella fase attuale

- Fatturazione elettronica reale.
- Contabilita certificata.
- Pagamenti reali.
- Spedizioni reali.
- Marketplace o integrazioni esterne.
- Multi-azienda e piani SaaS.
- Onboarding clienti, branding white-label, entitlement e provisioning automatico.
- Aggiornamento remoto o orchestrazione di una flotta di installazioni.
- Workflow GDPR, retention generale e legal hold.
- Conservazione elettronica a norma.
- Conformita legale o fiscale dichiarata.
- Business intelligence avanzata, data warehouse o report fiscali certificati.

La possibile evoluzione multi-azienda e multi-tenant e descritta come proposta futura in `MULTI_TENANCY_ARCHITECTURE.md`. Non fa parte delle funzionalita disponibili.

La possibile evoluzione privacy, conservazione e fatturazione elettronica e descritta in `PRIVACY_RETENTION_EINVOICING_ARCHITECTURE.md`. Nessuna delle funzionalita target e disponibile o dichiarata conforme nella versione corrente.

La possibile evoluzione commerciale del lifecycle cliente e descritta in `CUSTOMER_LIFECYCLE_AND_RELEASE_ARCHITECTURE.md`. Onboarding, branding, provisioning e aggiornamenti per coorti non sono disponibili nella versione corrente.

## Differenza rispetto a una demo

Il prodotto deve mostrare regole operative, sicurezza, tracciabilita, dati persistenti, test e documentazione. Non basta avere CRUD funzionanti.

## Differenza rispetto a un ERP

Non deve coprire tutte le aree aziendali. Deve essere un gestionale verticale, focalizzato su catalogo, magazzino, ordini e operazioni IT/commerciali leggere.
