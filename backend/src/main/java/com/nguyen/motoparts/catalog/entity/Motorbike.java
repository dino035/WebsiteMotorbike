package com.nguyen.motoparts.catalog.entity;

import com.nguyen.motoparts.vehicle.entity.VehicleConditionAssessment;
import com.nguyen.motoparts.vehicle.entity.VehiclePhoto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Xe máy cũ, một chiếc cụ thể của cửa hàng (đặc tả mục Product hierarchy).
 * Composition tới VehiclePhoto (1 — 1..*) và VehicleConditionAssessment (1 — 1).
 * KHÔNG nối tới VehicleModel (quyết định của giảng viên): brand/model là chuỗi, chọn từ autocomplete ở giao diện.
 */
@Entity
@Getter
@Setter
public class Motorbike extends Product {

    private String brand;
    private String model;
    private Integer registrationYear;
    private Integer ownerCount;
    private String accidentHistory;
    private String chassisNumber;
    private String engineNumber;
    private Integer odometerAtIntake;

    @OneToMany(mappedBy = "motorbike", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VehiclePhoto> photos = new ArrayList<>();

    @OneToOne(mappedBy = "motorbike", cascade = CascadeType.ALL, orphanRemoval = true)
    private VehicleConditionAssessment conditionAssessment;

    /** Gắn bảng đánh giá ở cả hai phía (Lombok không sinh setter này vì đã viết tay). */
    public void setConditionAssessment(VehicleConditionAssessment assessment) {
        this.conditionAssessment = assessment;
        if (assessment != null) {
            assessment.setMotorbike(this);
        }
    }

    /** Thêm ảnh vào danh sách và cho ảnh biết nó thuộc xe này, để hai phía khớp nhau. */
    public void addPhoto(VehiclePhoto photo) {
        photos.add(photo);
        photo.setMotorbike(this);
    }

    public void removePhoto(VehiclePhoto photo) {
        photos.remove(photo);
        photo.setMotorbike(null);
    }

    @Override
    public String getProductType() {
        return "Xe máy";
    }

    @Override
    public String getDetail() {
        return brand + " - " + model;
    }

    @Override
    public String getTypeCode() {
        return "Motorbike";
    }
}
