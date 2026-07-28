package it.giovannidefilippo.gestionale.common;

import it.giovannidefilippo.gestionale.document.FiscalDocument;
import it.giovannidefilippo.gestionale.document.FiscalDocumentLine;
import it.giovannidefilippo.gestionale.order.CustomerOrder;
import it.giovannidefilippo.gestionale.order.OrderItem;
import it.giovannidefilippo.gestionale.order.OrderPayment;
import it.giovannidefilippo.gestionale.order.OrderReturn;
import it.giovannidefilippo.gestionale.order.OrderReturnItem;
import it.giovannidefilippo.gestionale.order.PaymentTransaction;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.annotations.BatchSize;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class JpaFetchStrategyTest {
    @Test
    void orderAndDocumentCollectionsAreLazyAndBatched() throws Exception {
        assertLazyOneToManyWithBatch(CustomerOrder.class.getDeclaredField("items"));
        assertLazyOneToManyWithBatch(CustomerOrder.class.getDeclaredField("returns"));
        assertLazyOneToManyWithBatch(OrderPayment.class.getDeclaredField("transactions"));
        assertLazyOneToManyWithBatch(OrderReturn.class.getDeclaredField("items"));
        assertLazyOneToManyWithBatch(FiscalDocument.class.getDeclaredField("lines"));
    }

    @Test
    void backReferencesAreLazy() throws Exception {
        assertLazyManyToOne(OrderItem.class.getDeclaredField("order"));
        assertLazyManyToOne(FiscalDocumentLine.class.getDeclaredField("document"));
        assertLazyManyToOne(OrderReturn.class.getDeclaredField("order"));
        assertLazyManyToOne(OrderReturnItem.class.getDeclaredField("orderReturn"));
        assertLazyManyToOne(PaymentTransaction.class.getDeclaredField("payment"));
    }

    @Test
    void orderPaymentRelationshipIsLazyInBothDirections() throws Exception {
        assertLazyOneToOne(CustomerOrder.class.getDeclaredField("payment"));
        assertLazyOneToOne(OrderPayment.class.getDeclaredField("order"));
    }

    private static void assertLazyOneToManyWithBatch(Field field) {
        OneToMany mapping = field.getAnnotation(OneToMany.class);
        BatchSize batchSize = field.getAnnotation(BatchSize.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(batchSize).isNotNull();
        assertThat(batchSize.size()).isGreaterThanOrEqualTo(25);
    }

    private static void assertLazyManyToOne(Field field) {
        ManyToOne mapping = field.getAnnotation(ManyToOne.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.fetch()).isEqualTo(FetchType.LAZY);
    }

    private static void assertLazyOneToOne(Field field) {
        OneToOne mapping = field.getAnnotation(OneToOne.class);

        assertThat(mapping).isNotNull();
        assertThat(mapping.fetch()).isEqualTo(FetchType.LAZY);
    }
}
