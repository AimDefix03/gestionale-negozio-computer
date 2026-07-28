package it.giovannidefilippo.gestionale.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String code,
        String customerCode,
        String customer,
        LocalDateTime timestamp,
        String paymentMethod,
        PaymentResponse payment,
        List<OrderItemResponse> items,
        List<OrderReturnResponse> returns,
        BigDecimal total,
        OrderStatus status,
        String statusLabel,
        LocalDateTime statusChangedAt
) {
    static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getCode(),
                order.getCustomerCode(),
                order.getCustomer(),
                order.getTimestamp(),
                order.getPaymentMethod(),
                PaymentResponse.from(order.getPayment()),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getReturns().stream().map(OrderReturnResponse::from).toList(),
                order.getTotal(),
                order.getStatus(),
                order.getStatus().getLabel(),
                order.getStatusChangedAt()
        );
    }

    public record OrderItemResponse(String productCode, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.getProductCode(), item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
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
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<PaymentTransactionResponse> transactions
    ) {
        static PaymentResponse from(OrderPayment payment) {
            return new PaymentResponse(
                    payment.getId(),
                    payment.getMethod(),
                    payment.getMethod().getLabel(),
                    payment.getMethodDetails(),
                    payment.getStatus(),
                    payment.getStatus().getLabel(),
                    payment.getRequestedAmount(),
                    payment.getPaidAmount(),
                    payment.getRefundedAmount(),
                    payment.getNetPaidAmount(),
                    payment.getOutstandingAmount(),
                    payment.getRefundableAmount(),
                    payment.getCurrency(),
                    payment.getCreatedAt(),
                    payment.getUpdatedAt(),
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
            LocalDateTime recordedAt,
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
                    transaction.getRecordedAt(),
                    transaction.getRecordedBy(),
                    transaction.getRecordedByRole()
            );
        }
    }

    public record OrderReturnResponse(
            String code,
            OrderReturnStatus status,
            String statusLabel,
            String reason,
            List<OrderReturnItemResponse> items,
            BigDecimal totalAmount,
            BigDecimal refundedAmount,
            BigDecimal refundableAmount,
            LocalDateTime requestedAt,
            String requestedBy,
            String requestedByRole,
            LocalDateTime reviewedAt,
            String reviewedBy,
            String reviewNote,
            LocalDateTime receivedAt,
            String receivedBy,
            LocalDateTime updatedAt
    ) {
        static OrderReturnResponse from(OrderReturn orderReturn) {
            return new OrderReturnResponse(
                    orderReturn.getCode(),
                    orderReturn.getStatus(),
                    orderReturn.getStatus().getLabel(),
                    orderReturn.getReason(),
                    orderReturn.getItems().stream().map(OrderReturnItemResponse::from).toList(),
                    orderReturn.getTotalAmount(),
                    orderReturn.getRefundedAmount(),
                    orderReturn.getRefundableAmount(),
                    orderReturn.getRequestedAt(),
                    orderReturn.getRequestedBy(),
                    orderReturn.getRequestedByRole(),
                    orderReturn.getReviewedAt(),
                    orderReturn.getReviewedBy(),
                    orderReturn.getReviewNote(),
                    orderReturn.getReceivedAt(),
                    orderReturn.getReceivedBy(),
                    orderReturn.getUpdatedAt()
            );
        }
    }

    public record OrderReturnItemResponse(String productCode, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        static OrderReturnItemResponse from(OrderReturnItem item) {
            return new OrderReturnItemResponse(item.getProductCode(), item.getProductName(), item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
        }
    }
}
