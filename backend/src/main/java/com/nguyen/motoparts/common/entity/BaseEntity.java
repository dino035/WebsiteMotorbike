package com.nguyen.motoparts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Mốc thời gian dùng chung cho các thực thể cần theo dõi. Không phải một bảng riêng:
 * @MappedSuperclass nghĩa là hai cột này nằm trong bảng của lớp con.
 */
@MappedSuperclass
@Getter
public abstract class BaseEntity {

    /** Hibernate tự điền khi INSERT. Dòng dữ liệu cũ sẽ là null. */
    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    /** Hibernate tự cập nhật mỗi lần UPDATE. */
    @UpdateTimestamp
    private Instant updatedAt;
}