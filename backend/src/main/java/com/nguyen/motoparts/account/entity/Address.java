package com.nguyen.motoparts.account.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Địa chỉ trong sổ địa chỉ của khách.
 * Theo đặc tả: houseNumber, street, district, city; không có accountId, thuộc về User qua Association.
 * Giữ thêm từ WebsiteMotorbike: người nhận, số điện thoại, địa chỉ mặc định, thời điểm tạo.
 */
@Entity
@Getter
@Setter
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String addressId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private User user;

    private String receiverName;

    private String receiverPhone;

    private String houseNumber;

    private String street;

    private String district;

    private String city;

    /** Tối đa một địa chỉ mặc định mỗi tài khoản (service bảo đảm). */
    private boolean defaultAddress;

    private LocalDateTime createdAt;

    /** Ghép thành một dòng để chụp vào đơn hàng hoặc hiển thị. */
    public String getFullAddress() {
        return String.join(", ", houseNumber, street, district, city);
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
