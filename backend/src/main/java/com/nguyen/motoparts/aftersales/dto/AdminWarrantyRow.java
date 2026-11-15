package com.nguyen.motoparts.aftersales.dto;

import java.time.LocalDateTime;

/** Một dòng trong danh sách yêu cầu bảo hành của Admin. */
public record AdminWarrantyRow(
        String requestId,
        String shortCode,
        LocalDateTime submittedAt,
        String status,
        String statusLabel,
        String productName,
        String orderCode,
        String customerEmail) {
}
