package com.nguyen.motoparts.advisory.entity;

import com.nguyen.motoparts.vehicle.entity.VehicleModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Hồ sơ xe của khách (một hồ sơ = một xe, một khách có nhiều hồ sơ). Chat box dùng làm ngữ cảnh.
 * Theo đặc tả: accountId (String), nickname, Aggregation 0..* — 1 tới VehicleModel, registrationYear.
 * WebsiteMotorbike lưu brand/model dạng chuỗi; ở đây đổi sang nối VehicleModel cho đúng đặc tả.
 */
@Entity
@Getter
@Setter
public class CustomerVehicleProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String profileId;

    @Column(nullable = false)
    private String accountId;

    /** Tên gọi do khách đặt, ví dụ "Xe đi làm". */
    private String nickname;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_model_id")
    private VehicleModel vehicleModel;

    /** Năm đăng ký xe của khách, so với yearFrom–yearTo của luật tương thích. */
    private int registrationYear;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
