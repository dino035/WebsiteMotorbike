package com.nguyen.motoparts.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tài khoản đang bị khóa (SRS 3.23): có một dòng nghĩa là bị khóa, mở khóa thì xóa dòng.
 * Giữ từ WebsiteMotorbike (class ngoài đặc tả, sẽ bổ sung vào class diagram).
 */
@Entity
@Getter
@Setter
public class AccountLock {

    /** Mã tài khoản bị khóa (mỗi tài khoản tối đa một dòng). */
    @Id
    private String accountId;

    @Column(length = 200)
    private String reason;

    /** Mã tài khoản Admin đã khóa. */
    private String lockedBy;

    private LocalDateTime lockedAt;
}
