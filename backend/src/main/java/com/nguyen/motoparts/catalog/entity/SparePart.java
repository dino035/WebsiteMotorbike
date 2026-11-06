package com.nguyen.motoparts.catalog.entity;

import com.nguyen.motoparts.common.enums.PartOriginType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Phụ tùng (đặc tả mục Product hierarchy): partType, partOrigin, Composition tới SparePartPhoto (1 — 1..*).
 * Phụ tùng nào lắp được cho dòng xe nào nằm ở CompatibilityRule, không lưu trên class này.
 */
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
    public String getProductType() {
        return "Phụ tùng";
    }

    @Override
    public String getDetail() {
        String origin = (partOrigin == null) ? "" : " (" + partOrigin.getLabel() + ")";
        return partType + origin;
    }

    @Override
    public String getTypeCode() {
        return "SparePart";
    }
}
