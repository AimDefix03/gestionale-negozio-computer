package it.giovannidefilippo.gestionale.order;

import it.giovannidefilippo.gestionale.common.BusinessTime;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String code,
        String customerCode,
        String customer,
        Long customerAccountId,
        Long partnerId,
        OrderCustomerType customerType,
        String customerTypeLabel,
        OrderOwnershipStatus ownershipStatus,
        String ownershipStatusLabel,
        OffsetDateTime timestamp,
        String paymentMethod,
        PaymentResponse payment,
        List<OrderItemResponse> items,
        List<OrderReturnResponse> returns,
        BigDecimal total,
        OrderStatus status,
        String statusLabel,
        OffsetDateTime statusChangedAt,
        String cancellationReference,
        String cancellationReason,
        OffsetDateTime canceledAt,
        String canceledBy,
        String canceledByRole,
        OrderCapabilities capabilities
) {
    static OrderResponse from(CustomerOrder order) {
        return from(order, OrderCapabilities.none());
    }

    static OrderResponse from(CustomerOrder order, OrderCapabilities capabilities) {
        return new OrderResponse(
                order.getId(),
                order.getCode(),
                order.getCustomerCode(),
                order.getCustomer(),
                order.getCustomerAccountId(),
                order.getPartnerId(),
                order.getCustomerType(),
                order.getCustomerType().getLabel(),
                order.getOwnershipStatus(),
                order.getOwnershipStatus().getLabel(),
                BusinessTime.utcOffset(order.getTimestamp()),
                order.getPaymentMethod(),
                PaymentResponse.from(order.getPayment()),
                order.getItems().stream()
                        .map(item -> OrderItemResponse.from(item, order.returnedOrReservedQuantity(item.getProductCode())))
                        .toList(),
                order.getReturns().stream()
                        .map(orderReturn -> OrderReturnResponse.from(orderReturn, order.getPayment().getTransactions()))
                        .toList(),
                order.getTotal(),
                order.getStatus(),
                order.getStatus().getLabel(),
                BusinessTime.utcOffset(order.getStatusChangedAt()),
                order.getCancellationReference(),
                order.getCancellationReason(),
                BusinessTime.utcOffset(order.getCanceledAt()),
                order.getCanceledBy(),
                order.getCanceledByRole(),
                capabilities
        );
    }

    public record OrderItemResponse(
            String productCode,
            String productName,
            String productDescription,
            int quantity,
            int returnedOrReservedQuantity,
            int returnableQuantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
        static OrderItemResponse from(OrderItem item, int returnedOrReservedQuantity) {
            return new OrderItemResponse(
                    item.getProductCode(),
                    item.getProductName(),
                    item.getProductDescription(),
                    item.getQuantity(),
                    returnedOrReservedQuantity,
                    Math.max(0, item.getQuantity() - returnedOrReservedQuantity),
                    item.getUnitPrice(),
                    item.getLineTotal()
            );
        }
    }

    public record PaymentResponse(
            Long id,
            PaymentMethod method,
            String methodLabel,
            String methodDetails,
            PaymentStatus status,
            String statusLabel,
            BigDecimal requestedAmount,
            BigDecimal paidAmount,
            BigDecimal refundedAmount,
            BigDecimal netPaidAmount,
            BigDecimal outstandingAmount,
            BigDecimal refundableAmount,
            String currency,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            boolean reconciliationRequired,
            OffsetDateTime reconciledAt,
            String reconciledBy,
            String reconciledByRole,
            String reconciliationReference,
            String reconciliationReason,
            List<PaymentTransactionResponse> transactions
    ) {
        static PaymentResponse from(OrderPayment payment) {
            boolean reconciliationRequired = payment.isReconciliationRequired();
            return new PaymentResponse(
                    payment.getId(),
                    payment.getMethod(),
                    payment.getMethod().getLabel(),
                    payment.getMethodDetails(),
                    payment.getStatus(),
                    payment.getStatus().getLabel(),
                    payment.getRequestedAmount(),
                    reconciliationRequired ? null : payment.getPaidAmount(),
                    reconciliationRequired ? null : payment.getRefundedAmount(),
                    reconciliationRequired ? null : payment.getNetPaidAmount(),
                    reconciliationRequired ? null : payment.getOutstandingAmount(),
                    reconciliationRequired ? null : payment.getRefundableAmount(),
                    payment.getCurrency(),
                    BusinessTime.utcOffset(payment.getCreatedAt()),
                    BusinessTime.utcOffset(payment.getUpdatedAt()),
                    reconciliationRequired,
                    BusinessTime.utcOffset(payment.getReconciledAt()),
                    payment.getReconciledBy(),
                    payment.getReconciledByRole(),
                    payment.getReconciliationReference(),
                    payment.getReconciliationReason(),
                    payment.getTransactions().stream().map(PaymentTransactionResponse::from).toList()
            );
        }
    }

    public record PaymentTransactionResponse(
            Long id,
            String code,
            PaymentTransactionType type,
            String typeLabel,
            BigDecimal amount,
            String reference,
            String reason,
            String returnCode,
            Long returnId,
            Long cancellationOrderId,
            Long reconciliationPaymentId,
            OffsetDateTime recordedAt,
            String recordedBy,
            String recordedByRole
    ) {
        static PaymentTransactionResponse from(PaymentTransaction transaction) {
            return new PaymentTransactionResponse(
                    transaction.getId(),
                    transaction.getCode(),
                    transaction.getType(),
                    transaction.getType().getLabel(),
                    transaction.getAmount(),
                    transaction.getReference(),
                    transaction.getReason(),
                    transaction.getReturnCode(),
                    transaction.getReturnId(),
                    transaction.getCancellationOrderId(),
                    transaction.getReconciliationPaymentId(),
                    BusinessTime.utcOffset(transaction.getRecordedAt()),
                    transaction.getRecordedBy(),
                    transaction.getRecordedByRole()
            );
        }
    }

    public record OrderReturnResponse(
            Long id,
            String code,
            OrderReturnStatus status,
            String statusLabel,
            String reason,
            List<OrderReturnItemResponse> items,
            BigDecimal totalAmount,
            BigDecimal refundedAmount,
            BigDecimal refundableAmount,
            OffsetDateTime requestedAt,
            String requestedBy,
            String requestedByRole,
            OffsetDateTime reviewedAt,
            String reviewedBy,
            String reviewNote,
            OffsetDateTime receivedAt,
            String receivedBy,
            OffsetDateTime updatedAt,
            List<PaymentTransactionResponse> refundTransactions
    ) {
        static OrderReturnResponse from(OrderReturn orderReturn, List<PaymentTransaction> paymentTransactions) {
            return new OrderReturnResponse(
                    orderReturn.getId(),
                    orderReturn.getCode(),
                    orderReturn.getStatus(),
                    orderReturn.getStatus().getLabel(),
                    orderReturn.getReason(),
                    orderReturn.getItems().stream().map(OrderReturnItemResponse::from).toList(),
                    orderReturn.getTotalAmount(),
                    orderReturn.getRefundedAmount(),
                    orderReturn.getRefundableAmount(),
                    BusinessTime.utcOffset(orderReturn.getRequestedAt()),
                    orderReturn.getRequestedBy(),
                    orderReturn.getRequestedByRole(),
                    BusinessTime.utcOffset(orderReturn.getReviewedAt()),
                    orderReturn.getReviewedBy(),
                    orderReturn.getReviewNote(),
                    BusinessTime.utcOffset(orderReturn.getReceivedAt()),
                    orderReturn.getReceivedBy(),
                    BusinessTime.utcOffset(orderReturn.getUpdatedAt()),
                    paymentTransactions.stream()
                            .filter(transaction -> belongsToReturn(transaction, orderReturn))
                            .map(PaymentTransactionResponse::from)
                            .toList()
            );
        }

        private static boolean belongsToReturn(PaymentTransaction transaction, OrderReturn orderReturn) {
            if (transaction.getType() != PaymentTransactionType.REFUND) {
                return false;
            }
            Long returnId = orderReturn.getId();
            Long transactionReturnId = transaction.getReturnId();
            if (returnId != null && transactionReturnId != null) {
                return returnId.equals(transactionReturnId);
            }
            return transactionReturnId == null
                    && transaction.getReturnCode() != null
                    && transaction.getReturnCode().equalsIgnoreCase(orderReturn.getCode());
        }
    }

    public record OrderReturnItemResponse(String productCode, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        static OrderReturnItemResponse from(OrderReturnItem item) {
            return new OrderReturnItemResponse(item.getProductCode(), item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
        }
    }
}
