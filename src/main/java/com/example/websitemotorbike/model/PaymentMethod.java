package com.example.websitemotorbike.model;

import lombok.Getter;

/** Phương thức thanh toán (SRS 3.10). VNPAY chỉ dùng được khi đã cấu hình vnpay.properties (xem VNPayConfig). */
@Getter
public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng (COD)"),
    VNPAY("VNPay Sandbox (trực tuyến)");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }
}
