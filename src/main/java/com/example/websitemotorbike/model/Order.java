package com.example.websitemotorbike.model;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Đơn hàng. "Order" là từ khóa SQL/JPQL nên tên entity là "Orders" và bảng là "orders".
 * Chỉ giữ accountId kiểu String, không đặt @ManyToOne User (quy ước README mục 3).
 */
@Entity(name = "Orders")
@Table(name = "orders")
@Cacheable(false)
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String orderId;

    private String accountId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod;

    private String receiverName;

    private String receiverPhone;

    @Column(length = 500)
    private String shippingAddress;

    private BigDecimal totalAmount;

    /** Khách đã xác nhận đọc chính sách miễn đổi trả khi đặt hàng. */
    private boolean returnPolicyAccepted;

    private LocalDateTime createdAt;

    /** Ngày giờ đặt dạng dd/MM/yyyy HH:mm để hiển thị trên JSP. */
    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /** Mã ngắn hiển thị cho khách (8 ký tự đầu của UUID). */
    public String getShortCode() {
        return orderId == null ? "" : orderId.substring(0, Math.min(8, orderId.length())).toUpperCase();
    }
}
