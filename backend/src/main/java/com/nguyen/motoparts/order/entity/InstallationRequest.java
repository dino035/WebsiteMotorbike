package com.nguyen.motoparts.order.entity;

import com.nguyen.motoparts.common.enums.InstallationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Yêu cầu lắp đặt phụ tùng tại cửa hàng, mỗi đơn tối đa một yêu cầu (Composition 1 — 0..1 từ Order).
 * Theo đặc tả: requestId, preferredDate, laborFee, status. Đặc tả ghi preferredDate: DateTime; code dùng LocalDate
 * vì khách chỉ chọn ngày (như WebsiteMotorbike).
 * Giữ thêm: accountId (để Admin/khách tra nhanh), createdAt.
 */
@Entity
@Getter
@Setter
public class InstallationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String requestId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", unique = true)
    private Order order;

    private String accountId;

    private LocalDate preferredDate;

    /** Phí công, đã cộng vào tổng tiền của đơn. */
    private BigDecimal laborFee;

    @Enumerated(EnumType.STRING)
    private InstallationStatus status;

    private LocalDateTime createdAt;
}
