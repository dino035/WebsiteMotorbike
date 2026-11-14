package com.nguyen.motoparts.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Một dòng trong danh sách "Đơn hàng của tôi". */
public record OrderSummaryResponse(
        String orderId,
        String shortCode,
        LocalDateTime orderDate,
        String status,
        String statusLabel,
        BigDecimal totalAmount,
        int itemCount,
        String paymentMethod,
        String paymentMethodLabel,
        String paymentStatus,
        String paymentStatusLabel) {
}
