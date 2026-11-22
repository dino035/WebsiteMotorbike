package com.nguyen.motoparts.auth;

/** Một email đặt lại mật khẩu cần gửi. Chỉ được gửi sau khi giao dịch đã lưu mã commit xong. */
public record PasswordResetMail(String to, String subject, String body) {
}
