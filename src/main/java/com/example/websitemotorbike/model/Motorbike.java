package com.example.websitemotorbike.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


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

    public void setConditionAssessment(VehicleConditionAssessment assessment) {
        this.conditionAssessment = assessment;
        if (assessment != null) {
            assessment.setMotorbike(this);
        }
    }

    public void addPhoto(VehiclePhoto photo) {
        photos.add(photo);
        photo.setMotorbike(this);
    }

    public void removePhoto(VehiclePhoto photo) {
        photos.remove(photo);
        photo.setMotorbike(null);
    }

    @Override
    public String getProductType(){
        return "Xe máy";
    }

    @Override
    public String getDetail(){
        return brand + " - " + model;
    }

    @Override
    public String getTypeCode(){
        return "Motorbike";
    }
}
