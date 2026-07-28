package it.giovannidefilippo.gestionale.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

@Service
public class BusinessCodeGenerator {
    @PersistenceContext
    private EntityManager entityManager;

    public String nextOrderCode() {
        return format("ORD", nextValue("order_code_seq"));
    }

    public String nextInvoiceCode() {
        return format("FS", nextValue("invoice_code_seq"));
    }

    public String nextCreditNoteCode() {
        return format("NC", nextValue("credit_note_code_seq"));
    }

    public String nextPaymentTransactionCode() {
        return format("PAY", nextValue("payment_transaction_code_seq"));
    }

    public String nextOrderReturnCode() {
        return format("RES", nextValue("order_return_code_seq"));
    }

    private long nextValue(String sequenceName) {
        Number value = (Number) entityManager.createNativeQuery("select nextval('" + sequenceName + "')").getSingleResult();
        return value.longValue();
    }

    private static String format(String prefix, long value) {
        return prefix + "-" + String.format("%04d", value);
    }
}
