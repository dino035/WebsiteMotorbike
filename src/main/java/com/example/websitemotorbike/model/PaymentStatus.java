package com.example.websitemotorbike.model;

import lombok.Getter;

@Getter
public enum PaymentStatus {
    PENDING("Chưa thanh toán"),
    PAID("Đã thanh toán"),
    FAILED("Thanh toán thất bại");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }
}
