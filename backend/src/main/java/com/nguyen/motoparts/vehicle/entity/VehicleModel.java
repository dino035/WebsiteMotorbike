package com.nguyen.motoparts.vehicle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Dòng xe tham chiếu dùng chung (đặc tả mục Vehicle reference), KHÔNG có accountId.
 * Dùng cho CustomerVehicleProfile (Aggregation) và CompatibilityRule (Association).
 * Motorbike KHÔNG nối tới class này (quyết định của giảng viên).
 * Một dòng xe = hãng + model + khoảng năm sản xuất, nên bộ bốn này không được trùng.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"brand", "model", "year_from", "year_to"}))
@Getter
@Setter
public class VehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String modelId;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    /** Năm đầu và năm cuối sản xuất của dòng xe. */
    @Column(name = "year_from", nullable = false)
    private int yearFrom;

    @Column(name = "year_to", nullable = false)
    private int yearTo;

    /** Năm đăng ký của một chiếc xe có nằm trong khoảng sản xuất của dòng xe này không. */
    public boolean covers(int registrationYear) {
        return registrationYear >= yearFrom && registrationYear <= yearTo;
    }
}
