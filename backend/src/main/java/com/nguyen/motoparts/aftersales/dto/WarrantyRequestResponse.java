package com.nguyen.motoparts.aftersales.dto;

import java.time.LocalDateTime;

/** Một yêu cầu bảo hành, như khách thấy. decisionReason là lý do từ chối hoặc ghi chú khi duyệt. */
public record WarrantyRequestResponse(
        String requestId,
        String shortCode,
        String orderItemId,
        String productName,
        String orderCode,
        String description,
        String attachmentUrl,
        String status,
        String statusLabel,
        LocalDateTime submittedAt,
        String decisionReason,
        LocalDateTime resolvedAt) {
}
