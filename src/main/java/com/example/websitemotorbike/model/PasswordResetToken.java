package com.example.websitemotorbike.model;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Mã đặt lại mật khẩu (SRS 3.4). Chỉ lưu SHA-256 của mã (tokenHash), không lưu chính mã gửi trong email,
 * nên người xem được CSDL cũng không dùng được. Mã dùng một lần (usedAt) và hết hạn sau expiresAt.
 * Chỉ giữ accountId kiểu String, không đặt @ManyToOne User (quy ước README mục 3).
 */
@Entity
@Cacheable(false)
@Getter
@Setter
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String tokenId;

    @Column(length = 64)
    private String tokenHash;

    private String accountId;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    /** Thời điểm đã dùng hoặc bị thay thế bởi mã mới hơn; null nghĩa là còn dùng được (nếu chưa hết hạn). */
    private LocalDateTime usedAt;
}
