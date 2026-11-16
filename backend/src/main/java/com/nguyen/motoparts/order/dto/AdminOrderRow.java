package com.nguyen.motoparts.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Một dòng trong danh sách đơn hàng của Admin. */
public record AdminOrderRow(
        String orderId,
        String shortCode,
        LocalDateTime orderDate,
        String status,
        String statusLabel,
        BigDecimal totalAmount,
        String customerEmail,
        String receiverName,
        String deliveryMethodLabel,
        String paymentMethod,
        String paymentMethodLabel,
        String paymentStatus,
        String paymentStatusLabel) {
}
