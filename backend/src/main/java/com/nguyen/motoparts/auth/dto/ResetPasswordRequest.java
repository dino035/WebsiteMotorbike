package com.nguyen.motoparts.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Thiếu mã đặt lại mật khẩu.")
        String token,

        @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
        @Size(min = 8, max = 72, message = "Mật khẩu phải từ 8 đến 72 ký tự.")
        String password,

        @NotBlank(message = "Vui lòng nhập lại mật khẩu mới.")
        String confirmPassword) {
}
