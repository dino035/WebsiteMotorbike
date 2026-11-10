package com.nguyen.motoparts.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Một dòng của đơn. Giữ productId kiểu String (đặc tả).
 * Giữ thêm từ WebsiteMotorbike: productName, unitPrice là bản chụp lúc đặt, để đơn không đổi khi sản phẩm bị sửa hay xóa.
 */
@Entity
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String orderItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private String productId;

    private String productName;

    private BigDecimal unitPrice;

    private int quantity;

    public BigDecimal getLineTotal() {
        return unitPrice == null ? BigDecimal.ZERO : unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
