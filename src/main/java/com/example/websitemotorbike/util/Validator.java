package com.example.websitemotorbike.util;

import java.math.BigDecimal;

public class Validator {

    public static void requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " không được để trống");
        }
    }

    /** Số thập phân không âm (ví dụ giá tiền). Thiếu, sai định dạng hoặc âm thì ném IllegalArgumentException. */
    public static BigDecimal requireDecimal(String value, String label) {
        requireText(value, label);
        BigDecimal number;
        try {
            number = new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " phải là số hợp lệ");
        }
        if (number.signum() < 0) {
            throw new IllegalArgumentException(label + " không được âm");
        }
        return number;
    }

    /** Giá trị enum theo tên. Thiếu hoặc không có trong enum thì ném IllegalArgumentException (thông báo tiếng Việt). */
    public static <E extends Enum<E>> E requireEnum(String value, String label, Class<E> type) {
        requireText(value, label);
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(label + " không hợp lệ");
        }
    }
}