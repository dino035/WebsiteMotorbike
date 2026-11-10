package com.nguyen.motoparts.aftersales.entity;

import com.nguyen.motoparts.common.enums.WarrantyStatus;
import com.nguyen.motoparts.order.entity.OrderItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Yêu cầu bảo hành cho một sản phẩm đã mua.
 * Theo đặc tả: requestId, accountId, description, attachmentUrl, status, submittedAt, decisionReason.
 * Giữ thêm từ WebsiteMotorbike: nối tới OrderItem (biết bảo hành món nào, tính hạn bảo hành), resolvedAt.
 */
@Entity
@Getter
@Setter
public class WarrantyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String requestId;

    @Column(nullable = false)
    private String accountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(length = 1000)
    private String description;

    /** Đường dẫn ảnh/video minh chứng (WebsiteMotorbike gọi là evidenceUrl). */
    @Column(length = 500)
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarrantyStatus status;

    private LocalDateTime submittedAt;

    /** Lý do từ chối (bắt buộc khi từ chối) hoặc ghi chú khi duyệt (WebsiteMotorbike gọi là adminNote). */
    @Column(length = 500)
    private String decisionReason;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}
