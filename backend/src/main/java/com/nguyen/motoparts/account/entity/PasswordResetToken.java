package com.nguyen.motoparts.account.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Mã đặt lại mật khẩu (SRS 3.4). Chỉ lưu SHA-256 của mã gửi trong email (tokenHash).
 * Mã dùng một lần (usedAt) và hết hạn sau expiresAt.
 * Giữ từ WebsiteMotorbike (class ngoài đặc tả, sẽ bổ sung vào class diagram).
 */
@Entity
@Getter
@Setter
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String tokenId;

    @Column(length = 64, nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private String accountId;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    /** Thời điểm đã dùng hoặc bị thay bởi mã mới hơn; null nghĩa là còn dùng được (nếu chưa hết hạn). */
    private LocalDateTime usedAt;
}
