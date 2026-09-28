package it.giovannidefilippo.gestionale.inventory;

import it.giovannidefilippo.gestionale.common.BusinessTime;

import java.time.OffsetDateTime;
import java.util.List;

public record PhysicalInventorySessionResponse(
        Long id,
        long version,
        String code,
        PhysicalInventoryStatus status,
        String statusLabel,
        String reason,
        OffsetDateTime createdAt,
        String createdBy,
        String createdByRole,
        OffsetDateTime submittedAt,
        String submittedBy,
        String submittedByRole,
        OffsetDateTime approvedAt,
        String approvedBy,
        String approvedByRole,
        String approvalReason,
        OffsetDateTime canceledAt,
        String canceledBy,
        String cancellationReason,
        int itemCount,
        int countedItems,
        int differenceItems,
        int totalAbsoluteDifference,
        PhysicalInventoryCapabilities capabilities,
        List<PhysicalInventoryItemResponse> items
) {
    static PhysicalInventorySessionResponse from(PhysicalInventorySession session, PhysicalInventoryCapabilities capabilities, boolean includeItems) {
        List<PhysicalInventoryItemResponse> items = includeItems
                ? session.getItems().stream().map(PhysicalInventoryItemResponse::from).toList()
                : List.of();
        int counted = (int) session.getItems().stream().filter(PhysicalInventoryItem::isCounted).count();
        int differences = (int) session.getItems().stream()
                .filter(item -> item.getDifferenceQuantity() != null && item.getDifferenceQuantity() != 0)
                .count();
        int absoluteDifference = session.getItems().stream()
                .filter(item -> item.getDifferenceQuantity() != null)
                .mapToInt(item -> Math.abs(item.getDifferenceQuantity()))
                .sum();
        return new PhysicalInventorySessionResponse(
                session.getId(), session.getVersion(), session.getCode(), session.getStatus(), session.getStatus().getLabel(),
                session.getReason(), BusinessTime.utcOffset(session.getCreatedAt()), session.getCreatedBy(), session.getCreatedByRole(),
                offset(session.getSubmittedAt()), session.getSubmittedBy(), session.getSubmittedByRole(), offset(session.getApprovedAt()),
                session.getApprovedBy(), session.getApprovedByRole(), session.getApprovalReason(), offset(session.getCanceledAt()),
                session.getCanceledBy(), session.getCancellationReason(), session.getItems().size(), counted, differences,
                absoluteDifference, capabilities, items
        );
    }

    private static OffsetDateTime offset(java.time.LocalDateTime value) {
        return value == null ? null : BusinessTime.utcOffset(value);
    }
}
