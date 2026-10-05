package com.example.websitemotorbike.model;


import lombok.Getter;

@Getter
public enum ProductStatus {
    AVAILABLE("Có sẵn"),
    SOLD("Hết hàng");

    private final String label;

    ProductStatus(String label){
        this.label = label;
    }
}
