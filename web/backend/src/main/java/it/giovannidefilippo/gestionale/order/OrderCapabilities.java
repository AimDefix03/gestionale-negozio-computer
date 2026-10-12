package it.giovannidefilippo.gestionale.order;

import java.util.List;

public record OrderCapabilities(
        boolean canConfirm,
        boolean canFulfill,
        boolean canCancel,
        boolean canRecordReceipt,
        boolean canRequestReturn,
        List<ReturnCapabilities> returns
) {
    static OrderCapabilities none() {
        return new OrderCapabilities(false, false, false, false, false, List.of());
    }

    public record ReturnCapabilities(
            String returnCode,
            boolean canApprove,
            boolean canReject,
            boolean canReceive,
            boolean canRefund
    ) {
    }
}
