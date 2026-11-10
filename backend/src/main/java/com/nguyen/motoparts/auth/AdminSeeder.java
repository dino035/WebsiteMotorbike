package com.nguyen.motoparts.auth;

import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.common.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Tạo tài khoản ADMIN đầu tiên khi khởi động nếu đặt ADMIN_EMAIL và ADMIN_PASSWORD (biến môi trường).
 * Không đặt thì bỏ qua. Đã có tài khoản cùng email thì không làm gì, không ghi đè mật khẩu.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AuthService authService;
    private final UserRepository userRepository;
    private final String email;
    private final String password;

    public AdminSeeder(AuthService authService, UserRepository userRepository,
                       @Value("${app.admin.email:}") String email,
                       @Value("${app.admin.password:}") String password) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (password.length() < 8) {
            log.warn("ADMIN_PASSWORD qua ngan (can it nhat 8 ky tu), bo qua viec tao admin.");
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            return;
        }
        userRepository.save(authService.createUser(email, password, null, UserRole.ADMIN));
        log.info("Da tao tai khoan ADMIN {}", email.trim().toLowerCase());
    }
}
