package com.nguyen.motoparts.order.entity;

import com.nguyen.motoparts.common.enums.PaymentMethod;
import com.nguyen.motoparts.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thanh toán của một đơn. Theo đặc tả (quyết định 2026-10-06): chỉ giữ orderId kiểu String, không nối Order.
 * (WebsiteMotorbike dùng @OneToOne Order; ở đây đổi lại cho đúng đặc tả.)
 * COD bắt đầu ở PENDING cho tới khi Admin xác nhận đã thu tiền.
 */
@Entity
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String paymentId;

    /** Mỗi đơn một bản ghi thanh toán. */
    @Column(nullable = false, unique = true)
    private String orderId;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private BigDecimal amount;

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;
}
