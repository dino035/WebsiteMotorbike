package com.example.websitemotorbike.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;


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
