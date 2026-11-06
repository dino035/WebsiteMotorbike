package com.nguyen.motoparts.auth;

import com.nguyen.motoparts.auth.dto.AuthResponse;
import com.nguyen.motoparts.auth.dto.LoginRequest;
import com.nguyen.motoparts.auth.dto.RegisterRequest;
import com.nguyen.motoparts.auth.dto.UserInfo;
import com.nguyen.motoparts.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** React gọi khi tải lại trang để biết token còn dùng được không và ai đang đăng nhập. */
    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal AuthUser principal) {
        return authService.me(principal.accountId());
    }
}
