package com.example.websitemotorbike.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
public class SparePart extends Product {

    private String partType;

    @Enumerated(EnumType.STRING)
    private PartOriginType partOrigin;

    @OneToMany(mappedBy = "sparePart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SparePartPhoto> photos = new ArrayList<>();

    public void addPhoto(SparePartPhoto photo) {
        photos.add(photo);
        photo.setSparePart(this);
    }

    public void removePhoto(SparePartPhoto photo) {
        photos.remove(photo);
        photo.setSparePart(null);
    }

    @Override
    public String getProductType(){
        return "Phụ tùng";
    }

    @Override
    public String getDetail(){
        String origin = (partOrigin == null) ? "" : " (" + partOrigin.getLabel() + ")";
        return partType + origin;
    }

    @Override
    public String getTypeCode(){
        return "SparePart";
    }
}

