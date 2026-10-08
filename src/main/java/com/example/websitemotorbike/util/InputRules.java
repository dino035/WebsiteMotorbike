package com.example.websitemotorbike.util;

import java.util.regex.Pattern;

/** Quy tắc kiểm tra dữ liệu dùng chung cho thông tin người nhận (thanh toán và sổ địa chỉ). */
public class InputRules {

    public static final int MAX_NAME = 100;
    public static final int MAX_ADDRESS = 500;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+ ().-]{8,15}$");

    /** Họ tên và số điện thoại người nhận: không trống, độ dài và định dạng hợp lệ. */
    public static void validateReceiver(String name, String phone) {
        Validator.requireText(name, "Họ tên người nhận");
        Validator.requireText(phone, "Số điện thoại");
        if (name.trim().length() > MAX_NAME) {
            throw new IllegalArgumentException("Họ tên người nhận quá dài (tối đa " + MAX_NAME + " ký tự)");
        }
        if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        }
    }

    /** Địa chỉ nhận hàng: không trống và không quá dài. */
    public static void validateAddress(String address) {
        Validator.requireText(address, "Địa chỉ giao hàng");
        if (address.trim().length() > MAX_ADDRESS) {
            throw new IllegalArgumentException("Địa chỉ giao hàng quá dài (tối đa " + MAX_ADDRESS + " ký tự)");
        }
    }
}
