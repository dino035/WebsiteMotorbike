package com.example.websitemotorbike.util;

import java.util.Properties;

/**
 * Cấu hình cổng thanh toán VNPay Sandbox, đọc từ vnpay.properties (xem vnpay.example.properties).
 * Tệp này chứa mã bí mật HashSecret nên không nằm trong Git; mỗi máy tự điền thông tin tài khoản sandbox của mình.
 *
 * Chưa có tệp, hoặc còn giá trị mẫu YOUR_..., thì VNPay được xem là CHƯA CẤU HÌNH: trang thanh toán khóa lựa chọn
 * VNPay và chỉ cho thanh toán COD.
 */
public class VNPayConfig {

    public static final String DEFAULT_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    private static final Properties CONFIG = ConfigFile.load("vnpay.properties");

    public static String tmnCode() {
        return ConfigFile.get(CONFIG, "vnpay.tmnCode");
    }

    public static String hashSecret() {
        return ConfigFile.get(CONFIG, "vnpay.hashSecret");
    }

    public static String payUrl() {
        String url = ConfigFile.get(CONFIG, "vnpay.payUrl");
        return url.isEmpty() ? DEFAULT_PAY_URL : url;
    }

    /** Địa chỉ đầy đủ của /vnpay/return trên máy này. Cấu hình cố định, không lấy từ yêu cầu của người dùng. */
    public static String returnUrl() {
        return ConfigFile.get(CONFIG, "vnpay.returnUrl");
    }

    /** Thời gian khách được trả tiền trên cổng VNPay (phút). */
    public static int expireMinutes() {
        try {
            int minutes = Integer.parseInt(ConfigFile.get(CONFIG, "vnpay.expireMinutes"));
            return minutes >= 5 && minutes <= 60 ? minutes : 15;
        } catch (NumberFormatException e) {
            return 15;
        }
    }

    public static boolean isConfigured() {
        return isFilled(tmnCode()) && isFilled(hashSecret()) && isFilled(returnUrl());
    }

    private static boolean isFilled(String value) {
        return !value.isEmpty() && !value.startsWith("YOUR_");
    }
}
