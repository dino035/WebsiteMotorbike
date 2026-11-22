package com.nguyen.motoparts.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Thiếu mã đặt lại mật khẩu.")
        String token,

        @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
        @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự.")   // giới hạn trên tính theo byte: PasswordPolicy
        String password,

        @NotBlank(message = "Vui lòng nhập lại mật khẩu mới.")
        String confirmPassword) {
}
