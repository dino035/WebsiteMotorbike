package com.nguyen.motoparts.security;

import com.nguyen.motoparts.account.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Tạo và kiểm tra JWT (HS256). Khóa ký đọc từ biến môi trường JWT_SECRET (tối thiểu 32 byte).
 * Nếu chưa đặt, tạo khóa ngẫu nhiên khi khởi động: chạy máy cá nhân được,
 * nhưng mọi token mất hiệu lực khi khởi động lại (không dùng cách này khi deploy).
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expiration-minutes:120}") long expirationMinutes) {
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            this.key = Keys.hmacShaKeyFor(random);
            log.warn("JWT_SECRET chua duoc dat: dung khoa ngau nhien, token mat hieu luc khi khoi dong lai. "
                    + "Hay dat JWT_SECRET (>= 32 ky tu) khi deploy.");
        } else if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET phai dai it nhat 32 byte.");
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public Duration getExpiration() {
        return expiration;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getAccountId())
                .claim("email", user.getEmail())
                .claim("role", user.getUserRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /** Trả về accountId nếu token hợp lệ (đúng chữ ký, chưa hết hạn), ngược lại rỗng. */
    public Optional<String> parseAccountId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
