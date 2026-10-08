package com.example.websitemotorbike.model;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Tài khoản đang bị khóa (SRS 3.23): có một dòng ở đây nghĩa là tài khoản bị khóa, mở khóa thì xóa dòng.
 * Dùng bảng riêng thay vì thêm cột vào bảng users, vì JPA (schema-generation = create) không tự thêm cột
 * vào bảng đã có trên CSDL dùng chung; bảng mới thì được tự tạo, không cần ALTER TABLE.
 * Chỉ giữ mã tài khoản kiểu String, không đặt @ManyToOne User (quy ước README mục 3).
 */
@Entity
@Cacheable(false)
@Getter
@Setter
public class AccountLock {

    /** Mã tài khoản bị khóa (khóa chính, mỗi tài khoản tối đa một dòng). */
    @Id
    private String accountId;

    @Column(length = 200)
    private String reason;

    /** Mã tài khoản Admin đã khóa. */
    private String lockedBy;

    private LocalDateTime lockedAt;

    public String getLockedAtText() {
        return lockedAt == null ? "" : lockedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
