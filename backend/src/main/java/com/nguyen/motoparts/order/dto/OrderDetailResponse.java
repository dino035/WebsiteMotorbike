package com.nguyen.motoparts.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Chi tiết một đơn: dòng hàng (giá và tên chụp lúc đặt), thanh toán, lịch lắp đặt nếu có. */
public record OrderDetailResponse(
        String orderId,
        String shortCode,
        LocalDateTime orderDate,
        String status,
        String statusLabel,
        String deliveryMethod,
        String deliveryMethodLabel,
        String receiverName,
        String receiverPhone,
        /** Null khi nhận tại cửa hàng hoặc lắp đặt. */
        String shippingAddress,
        BigDecimal totalAmount,
        boolean returnPolicyAccepted,
        List<Item> items,
        PaymentInfo payment,
        /** Null khi đơn không có lắp đặt. */
        InstallationInfo installation) {

    public record Item(String productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
    }

    /** canPayOnline: đơn chọn VNPay, chưa trả được tiền và chưa hủy, nên khách được bấm "Thanh toán" hoặc đổi sang COD. */
    public record PaymentInfo(String method, String methodLabel, String status, String statusLabel,
                              BigDecimal amount, LocalDateTime paidAt, boolean canPayOnline) {
    }

    public record InstallationInfo(LocalDate preferredDate, BigDecimal laborFee, String status, String statusLabel) {
    }
}
