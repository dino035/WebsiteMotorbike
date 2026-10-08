package com.example.websitemotorbike.model;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Yêu cầu bảo hành của khách cho một sản phẩm đã mua (một dòng OrderItem).
 * Chỉ giữ accountId kiểu String, không đặt @ManyToOne User (quy ước README mục 3).
 * evidenceUrl là đường dẫn ảnh/video minh chứng (chưa hỗ trợ tải tệp lên).
 */
@Entity
@Cacheable(false)
@Getter
@Setter
public class WarrantyRequest {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String warrantyId;

    private String accountId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "orderItemId")
    private OrderItem orderItem;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String evidenceUrl;

    @Enumerated(EnumType.STRING)
    private WarrantyStatus status;

    /** Lý do từ chối (bắt buộc khi từ chối) hoặc ghi chú khi duyệt. */
    @Column(length = 500)
    private String adminNote;

    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    /** Mã ngắn hiển thị (8 ký tự đầu của UUID). */
    public String getShortCode() {
        return warrantyId == null ? "" : warrantyId.substring(0, Math.min(8, warrantyId.length())).toUpperCase();
    }

    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(DATE_TIME);
    }

    public String getResolvedAtText() {
        return resolvedAt == null ? "" : resolvedAt.format(DATE_TIME);
    }
}
