package com.nguyen.motoparts.catalog.dto;

import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.common.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(
        String productId, String name, BigDecimal price, ProductStatus status, String statusLabel,
        String typeCode, String productType, String detail,
        List<PhotoResponse> photos,
        MotorbikeInfo motorbike,
        SparePartInfo sparePart) {

    public record PhotoResponse(String photoId, String imageUrl, String caption) {
    }

    public record MotorbikeInfo(String brand, String model, Integer registrationYear,
                                Integer ownerCount, String accidentHistory, Integer odometerAtIntake) {
    }

    public record SparePartInfo(String partType, String partOriginLabel) {
    }

    public static ProductDetailResponse from(Product p) {
        List<PhotoResponse> photos = List.of();
        MotorbikeInfo motorbike = null;
        SparePartInfo sparePart = null;

        if (p instanceof Motorbike m) {
            photos = m.getPhotos().stream()
                    .map(ph -> new PhotoResponse(ph.getPhotoId(), ph.getImageUrl(), ph.getCaption()))
                    .toList();
            motorbike = new MotorbikeInfo(m.getBrand(), m.getModel(), m.getRegistrationYear(), m.getOwnerCount(), m.getAccidentHistory(), m.getOdometerAtIntake());
        } else if (p instanceof SparePart s) {
            photos = s.getPhotos().stream()
                    .map(ph -> new PhotoResponse(ph.getPhotoId(), ph.getImageUrl(), ph.getCaption()))
                    .toList();
            sparePart = new SparePartInfo(s.getPartType(), s.getPartOrigin() == null ? null : s.getPartOrigin().getLabel());
        }

        return new ProductDetailResponse(p.getProductId(), p.getName(), p.getPrice(), p.getStatus(), p.getStatus().getLabel(), p.getTypeCode(), p.getProductType(), p.getDetail(), photos, motorbike, sparePart);
    }
}
