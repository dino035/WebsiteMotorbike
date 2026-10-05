package com.example.websitemotorbike.model;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;


@Entity
@Getter
@Setter
public class SparePart extends Product {

    private String partType;

    @Enumerated(EnumType.STRING)
    private PartOriginType partOrigin;

    @Override
    public String getProductType(){
        return "Phụ tùng";
    }

    @Override
    public String getDetail(){
        return partType + " (" + partOrigin +")";
    }

    @Override
    public String getTypeCode(){
        return "SparePart";
    }
    }
