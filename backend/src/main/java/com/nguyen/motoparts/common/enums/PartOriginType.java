package com.nguyen.motoparts.common.enums;

import lombok.Getter;

/** Nguồn gốc phụ tùng (đặc tả: OEM / Aftermarket; code dùng GENUINE / AFTERMARKET như WebsiteMotorbike). */
@Getter
public enum PartOriginType {
    GENUINE("Chính hãng"),
    AFTERMARKET("Phụ tùng bên thứ 3");

    private final String label;

    PartOriginType(String label) {
        this.label = label;
    }
}
