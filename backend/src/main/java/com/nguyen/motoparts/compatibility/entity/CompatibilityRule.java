package com.nguyen.motoparts.compatibility.entity;

import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.vehicle.entity.VehicleModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Luật tương thích: phụ tùng này lắp được cho dòng xe kia (đặc tả mục Compatibility).
 * Là bảng trung gian giải quyết quan hệ nhiều – nhiều SparePart ↔ VehicleModel:
 * Association 0..* — 1 tới VehicleModel và 0..* — 1 tới SparePart.
 * Mỗi cặp (dòng xe, phụ tùng) chỉ có một luật. Sửa luật có hiệu lực ngay, không hồi tố đơn cũ.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"vehicle_model_id", "spare_part_id"}))
@Getter
@Setter
public class CompatibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String ruleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_model_id")
    private VehicleModel vehicleModel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spare_part_id")
    private SparePart sparePart;

    private LocalDateTime updatedAt;

    /** Mã tài khoản Admin sửa luật gần nhất. */
    private String updatedBy;

    /** Tự đặt thời điểm mỗi lần thêm hoặc sửa. */
    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }
}
