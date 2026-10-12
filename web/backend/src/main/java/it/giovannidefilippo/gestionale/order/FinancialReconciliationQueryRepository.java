package it.giovannidefilippo.gestionale.order;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
class FinancialReconciliationQueryRepository {
    private final JdbcTemplate jdbcTemplate;

    FinancialReconciliationQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<PaymentSnapshot> payments() {
        return jdbcTemplate.query("""
                select p.id,
                       o.code as order_code,
                       o.status as order_status,
                       p.status as payment_status,
                       p.requested_amount,
                       p.paid_amount,
                       p.refunded_amount,
                       p.created_at,
                       p.updated_at,
                       p.reconciled_at,
                       coalesce(sum(case when t.type in ('RECEIPT', 'RECONCILIATION') then t.amount else 0 end), 0) as ledger_paid,
                       coalesce(sum(case when t.type in ('REFUND', 'REVERSAL') then t.amount else 0 end), 0) as ledger_refunded,
                       coalesce(sum(case when t.recorded_at < p.created_at or t.recorded_at > p.updated_at then 1 else 0 end), 0) as invalid_transaction_times,
                       coalesce(sum(case when t.type = 'REFUND' and (t.return_id is null or r.order_id <> o.id) then 1 else 0 end), 0) as invalid_refund_links,
                       coalesce(sum(case when t.type = 'REVERSAL' and (t.cancellation_order_id is null or t.cancellation_order_id <> o.id or o.status <> 'CANCELED') then 1 else 0 end), 0) as invalid_reversal_links,
                       coalesce(sum(case when t.type = 'RECONCILIATION' and (t.reconciliation_payment_id is null or t.reconciliation_payment_id <> p.id) then 1 else 0 end), 0) as invalid_reconciliation_links
                from order_payments p
                join customer_orders o on o.id = p.order_id
                left join payment_transactions t on t.payment_id = p.id
                left join order_returns r on r.id = t.return_id
                group by p.id, o.code, o.status, p.status, p.requested_amount, p.paid_amount,
                         p.refunded_amount, p.created_at, p.updated_at, p.reconciled_at
                order by p.id
                """, (resultSet, rowNumber) -> paymentSnapshot(resultSet));
    }

    List<ReturnSnapshot> returns() {
        return jdbcTemplate.query("""
                select r.id,
                       r.code,
                       o.code as order_code,
                       r.status,
                       r.total_amount,
                       r.refunded_amount,
                       r.requested_at,
                       r.reviewed_at,
                       r.received_at,
                       r.updated_at,
                       coalesce(sum(case when t.type = 'REFUND' then t.amount else 0 end), 0) as ledger_refunded
                from order_returns r
                join customer_orders o on o.id = r.order_id
                left join payment_transactions t on t.return_id = r.id
                group by r.id, r.code, o.code, r.status, r.total_amount, r.refunded_amount,
                         r.requested_at, r.reviewed_at, r.received_at, r.updated_at
                order by r.id
                """, (resultSet, rowNumber) -> returnSnapshot(resultSet));
    }

    private static PaymentSnapshot paymentSnapshot(ResultSet resultSet) throws SQLException {
        return new PaymentSnapshot(
                resultSet.getLong("id"),
                resultSet.getString("order_code"),
                resultSet.getString("order_status"),
                resultSet.getString("payment_status"),
                resultSet.getBigDecimal("requested_amount"),
                resultSet.getBigDecimal("paid_amount"),
                resultSet.getBigDecimal("refunded_amount"),
                resultSet.getBigDecimal("ledger_paid"),
                resultSet.getBigDecimal("ledger_refunded"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class),
                resultSet.getObject("reconciled_at", LocalDateTime.class),
                resultSet.getLong("invalid_transaction_times"),
                resultSet.getLong("invalid_refund_links") + resultSet.getLong("invalid_reversal_links") + resultSet.getLong("invalid_reconciliation_links")
        );
    }

    private static ReturnSnapshot returnSnapshot(ResultSet resultSet) throws SQLException {
        return new ReturnSnapshot(
                resultSet.getLong("id"),
                resultSet.getString("code"),
                resultSet.getString("order_code"),
                resultSet.getString("status"),
                resultSet.getBigDecimal("total_amount"),
                resultSet.getBigDecimal("refunded_amount"),
                resultSet.getBigDecimal("ledger_refunded"),
                resultSet.getObject("requested_at", LocalDateTime.class),
                resultSet.getObject("reviewed_at", LocalDateTime.class),
                resultSet.getObject("received_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class)
        );
    }

    record PaymentSnapshot(
            Long id,
            String orderCode,
            String orderStatus,
            String status,
            BigDecimal requestedAmount,
            BigDecimal paidAmount,
            BigDecimal refundedAmount,
            BigDecimal ledgerPaid,
            BigDecimal ledgerRefunded,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime reconciledAt,
            long invalidTransactionTimes,
            long invalidLinks
    ) {
    }

    record ReturnSnapshot(
            Long id,
            String code,
            String orderCode,
            String status,
            BigDecimal totalAmount,
            BigDecimal refundedAmount,
            BigDecimal ledgerRefunded,
            LocalDateTime requestedAt,
            LocalDateTime reviewedAt,
            LocalDateTime receivedAt,
            LocalDateTime updatedAt
    ) {
    }
}
