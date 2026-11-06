package com.nguyen.motoparts.auth;

import com.nguyen.motoparts.account.entity.User;
import com.nguyen.motoparts.account.repository.AccountLockRepository;
import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.auth.dto.AuthResponse;
import com.nguyen.motoparts.auth.dto.LoginRequest;
import com.nguyen.motoparts.auth.dto.RegisterRequest;
import com.nguyen.motoparts.auth.dto.UserInfo;
import com.nguyen.motoparts.common.enums.UserRole;
import com.nguyen.motoparts.common.exception.AccountLockedException;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private static final String BAD_LOGIN = "Email hoặc mật khẩu không đúng.";

    private final UserRepository userRepository;
    private final AccountLockRepository accountLockRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Băm giả để thời gian xử lý giống nhau dù email có tồn tại hay không. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, AccountLockRepository accountLockRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.accountLockRepository = accountLockRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /** Đăng ký tài khoản khách (CUSTOMER). Admin không đăng ký qua API công khai. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email này đã được đăng ký.");
        }
        User user = createUser(email, req.password(), blankToNull(req.phone()), UserRole.CUSTOMER);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Hai yêu cầu đăng ký cùng email chạy song song: ràng buộc unique chặn bản thứ hai.
            throw new ConflictException("Email này đã được đăng ký.");
        }
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(normalize(req.email())).orElse(null);
        // Luôn so khớp mật khẩu (kể cả khi không có user) và dùng một thông báo chung để không lộ email nào tồn tại.
        boolean ok = passwordEncoder.matches(req.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !ok) {
            throw new BadCredentialsException(BAD_LOGIN);
        }
        if (accountLockRepository.existsById(user.getAccountId())) {
            throw new AccountLockedException("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserInfo me(String accountId) {
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        return toInfo(user);
    }

    /** Dùng chung cho đăng ký và tạo admin mẫu lúc khởi động. */
    public User createUser(String email, String rawPassword, String phone, UserRole role) {
        User user = new User();
        user.setAccountId(UUID.randomUUID().toString());
        user.setEmail(normalize(email));
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setUserRole(role);
        return user;
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer",
                jwtService.getExpiration().toSeconds(), toInfo(user));
    }

    private static UserInfo toInfo(User u) {
        return new UserInfo(u.getAccountId(), u.getEmail(), u.getPhone(), u.getUserRole());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
