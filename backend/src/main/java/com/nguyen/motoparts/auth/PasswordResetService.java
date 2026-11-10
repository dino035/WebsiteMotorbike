package com.nguyen.motoparts.auth;

import com.nguyen.motoparts.account.entity.PasswordResetToken;
import com.nguyen.motoparts.account.entity.User;
import com.nguyen.motoparts.account.repository.PasswordResetTokenRepository;
import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.mail.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Quên mật khẩu (SRS 3.4): gửi liên kết một lần qua email rồi đặt mật khẩu mới.
 * Chỉ lưu SHA-256 của mã (tokenHash), mã gốc chỉ nằm trong email. Mã dùng một lần và hết hạn.
 */
@Service
public class PasswordResetService {

    public static final String INVALID_LINK_MESSAGE = "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendUrl;
    private final int expireMinutes;
    private final int maxPerHour;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder, EmailService emailService,
                                @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
                                @Value("${app.reset.expire-minutes:30}") int expireMinutes,
                                @Value("${app.reset.max-per-hour:5}") int maxPerHour) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        this.expireMinutes = expireMinutes;
        this.maxPerHour = maxPerHour;
    }

    public int getExpireMinutes() {
        return expireMinutes;
    }

    /**
     * Gửi liên kết đặt lại mật khẩu nếu email có tài khoản. Email không có tài khoản hoặc đã yêu cầu quá nhiều
     * lần thì im lặng không làm gì, để người ngoài không dò ra được email nào đã đăng ký.
     * Gửi email lỗi thì ném EmailSendException và giao dịch rollback (mã vừa tạo không được lưu).
     */
    @Transactional
    public void requestReset(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT)).orElse(null);
        if (user == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.deleteOlderThan(now.minusDays(1));
        if (tokenRepository.countByAccountIdAndCreatedAtAfter(user.getAccountId(), now.minusHours(1)) >= maxPerHour) {
            return;
        }
        tokenRepository.invalidateAll(user.getAccountId(), now);   // mã mới thay các mã cũ chưa dùng

        String rawToken = newRawToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setTokenHash(hash(rawToken));
        token.setAccountId(user.getAccountId());
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusMinutes(expireMinutes));
        tokenRepository.save(token);

        String link = frontendUrl + "/reset-password?token=" + rawToken;
        String body = "Xin chào,\n\n"
                + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản " + user.getEmail() + ".\n"
                + "Bấm vào liên kết dưới đây để đặt mật khẩu mới (có hiệu lực trong " + expireMinutes
                + " phút, chỉ dùng được một lần):\n\n"
                + link + "\n\n"
                + "Nếu bạn không yêu cầu, hãy bỏ qua email này; mật khẩu của bạn không bị thay đổi.\n";
        emailService.send(user.getEmail(), "Đặt lại mật khẩu - MotoParts", body);
    }

    /** Mã còn dùng được không (tồn tại, chưa dùng, chưa hết hạn). */
    @Transactional(readOnly = true)
    public boolean isValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        return tokenRepository.existsByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
                hash(rawToken.trim()), LocalDateTime.now());
    }

    /**
     * Đặt mật khẩu mới. Mã dùng được một lần: xong thì mã này và mọi mã còn lại của tài khoản bị vô hiệu.
     *
     * @throws BadRequestException mã không hợp lệ, đã dùng hoặc hết hạn
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = rawToken == null ? null
                : tokenRepository.findUsableForUpdate(hash(rawToken.trim()), now).orElse(null);
        if (token == null) {
            throw new BadRequestException(INVALID_LINK_MESSAGE);
        }
        User user = userRepository.findById(token.getAccountId())
                .orElseThrow(() -> new BadRequestException(INVALID_LINK_MESSAGE));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.invalidateAll(token.getAccountId(), now);
    }

    /** 32 byte ngẫu nhiên mật mã học, Base64 an toàn cho URL (khoảng 43 ký tự). */
    private static String newRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
