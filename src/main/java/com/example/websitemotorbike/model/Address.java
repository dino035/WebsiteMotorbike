package com.example.websitemotorbike.model;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Địa chỉ nhận hàng đã lưu trong sổ địa chỉ của khách (README: entity Address của Nhóm 1).
 * Chỉ giữ accountId kiểu String, không đặt @ManyToOne User (quy ước README mục 3).
 * Đơn hàng KHÔNG tham chiếu tới Address mà chụp lại họ tên, số điện thoại và địa chỉ lúc đặt,
 * nên sửa hoặc xóa địa chỉ không làm đổi các đơn cũ.
 */
@Entity
@Cacheable(false)
@Getter
@Setter
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String addressId;

    private String accountId;

    private String receiverName;

    private String receiverPhone;

    @Column(length = 500)
    private String addressLine;

    /** Địa chỉ mặc định của tài khoản (tối đa một địa chỉ mỗi tài khoản). */
    private boolean defaultAddress;

    private LocalDateTime createdAt;
}
