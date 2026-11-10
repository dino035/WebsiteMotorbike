package com.nguyen.motoparts.common.enums;

import lombok.Getter;

/** Phương thức thanh toán (SRS 3.10). */
@Getter
public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng (COD)"),
    VNPAY("VNPay Sandbox (trực tuyến)");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }
}
