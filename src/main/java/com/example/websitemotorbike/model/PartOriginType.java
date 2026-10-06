package com.example.websitemotorbike.model;

import lombok.Getter;

@Getter
public enum PartOriginType {
    GENUINE("Chính hãng"),
    AFTERMARKET("Phụ tùng bên thứ 3");

    private final String label;

    PartOriginType(String label) {
        this.label = label;
    }
}