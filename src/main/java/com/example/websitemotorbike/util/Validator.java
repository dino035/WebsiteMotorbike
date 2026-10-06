package com.example.websitemotorbike.util;

public class Validator {

    public static void requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " không được để trống");
        }
    }
}