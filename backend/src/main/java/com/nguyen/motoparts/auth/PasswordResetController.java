package com.nguyen.motoparts.auth;

import com.nguyen.motoparts.auth.dto.ForgotPasswordRequest;
import com.nguyen.motoparts.auth.dto.ResetPasswordRequest;
import com.nguyen.motoparts.auth.dto.TokenCheckRequest;
import com.nguyen.motoparts.common.exception.BadRequestException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Quên mật khẩu (SRS 3.4). Các đường này không cần đăng nhập. */
@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    /** Bước 1: nhập email. Luôn trả cùng một thông báo, dù email có tài khoản hay không. */
    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        service.requestReset(request.email());
        return Map.of("message", "Nếu email đã đăng ký, chúng tôi đã gửi liên kết đặt lại mật khẩu đến email đó. "
                + "Liên kết có hiệu lực " + service.getExpireMinutes() + " phút.");
    }

    /** React gọi khi mở trang đặt lại mật khẩu, để báo liên kết hết hạn trước khi khách điền biểu mẫu. */
    @PostMapping("/reset-password/validate")
    public Map<String, Boolean> validate(@RequestBody TokenCheckRequest request) {
        return Map.of("valid", service.isValid(request.token()));
    }

    /** Bước 2: nhập mật khẩu mới kèm mã trong liên kết. */
    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp.");
        }
        service.resetPassword(request.token(), request.password());
        return Map.of("message", "Đặt lại mật khẩu thành công, hãy đăng nhập bằng mật khẩu mới.");
    }
}
