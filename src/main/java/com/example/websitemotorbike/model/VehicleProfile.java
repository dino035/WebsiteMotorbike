package com.example.websitemotorbike.model;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Hồ sơ xe mà khách đang sử dụng (SRS 3.15): biệt danh và hãng, dòng xe. Trợ lý AI dùng làm ngữ cảnh khi
 * kiểm tra phụ tùng có tương thích không. Chỉ giữ accountId kiểu String, không đặt @ManyToOne User
 * (quy ước README mục 3). Không liên quan tới entity Motorbike (xe cửa hàng đang bán).
 */
@Entity
@Cacheable(false)
@Getter
@Setter
public class VehicleProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String profileId;

    private String accountId;

    /** Tên gọi do khách đặt, ví dụ "Xe đi làm". */
    private String nickname;

    private String brand;

    private String model;

    private LocalDateTime createdAt;
}
