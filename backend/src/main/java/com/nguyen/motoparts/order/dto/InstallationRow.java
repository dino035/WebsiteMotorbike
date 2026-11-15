package com.nguyen.motoparts.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Một lịch lắp đặt trong danh sách của Admin. */
public record InstallationRow(
        String requestId,
        String orderId,
        String orderShortCode,
        String orderStatus,
        String orderStatusLabel,
        String customerEmail,
        String receiverName,
        String receiverPhone,
        LocalDate preferredDate,
        BigDecimal laborFee,
        String status,
        String statusLabel) {
}
