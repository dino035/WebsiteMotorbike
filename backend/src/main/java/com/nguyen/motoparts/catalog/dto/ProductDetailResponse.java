package com.nguyen.motoparts.catalog.dto;

import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.common.enums.PartOriginType;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.common.enums.RatingLevel;
import com.nguyen.motoparts.vehicle.entity.VehicleConditionAssessment;

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

    public record ConditionInfo(
            RatingLevel engineRating, RatingLevel brakeRating, RatingLevel tireRating,
            RatingLevel chainRating, RatingLevel electricalRating,
            Double coefficient, String verdict, List<String> repairAdvice) {
    }

    public record MotorbikeInfo(String brand, String model, Integer registrationYear,
                                Integer ownerCount, String accidentHistory, Integer odometerAtIntake,
                                ConditionInfo condition) {
    }


    public record SparePartInfo(String partType, String partOriginLabel, PartOriginType partOrigin) {
    }

    public static ProductDetailResponse from(Product p) {
        List<ProductDetailResponse.PhotoResponse> photos = List.of();
        ProductDetailResponse.MotorbikeInfo motorbike = null;
        SparePartInfo sparePart = null;

        if (p instanceof Motorbike m) {
            photos = m.getPhotos().stream()
                    .map(ph -> new ProductDetailResponse.PhotoResponse(ph.getPhotoId(), ph.getImageUrl(), ph.getCaption()))
                    .toList();
            ConditionInfo condition = null;
            VehicleConditionAssessment a = m.getConditionAssessment();
            if (a != null) {
                condition = new ConditionInfo(a.getEngineRating(), a.getBrakeRating(), a.getTireRating(),
                        a.getChainRating(), a.getElectricalRating(),
                        a.getAdjustmentCoefficient(), a.getVerdict(), a.getRepairAdvice());
            }
            motorbike = new ProductDetailResponse.MotorbikeInfo(m.getBrand(), m.getModel(), m.getRegistrationYear(), m.getOwnerCount(), m.getAccidentHistory(), m.getOdometerAtIntake(), condition);
        } else if (p instanceof SparePart s) {
            photos = s.getPhotos().stream()
                    .map(ph -> new PhotoResponse(ph.getPhotoId(), ph.getImageUrl(), ph.getCaption()))
                    .toList();
            sparePart = new SparePartInfo(s.getPartType(), s.getPartOrigin() == null ? null : s.getPartOrigin().getLabel(), s.getPartOrigin());
        }

        return new ProductDetailResponse(p.getProductId(), p.getName(), p.getPrice(), p.getStatus(), p.getStatus().getLabel(), p.getTypeCode(), p.getProductType(), p.getDetail(), photos, motorbike, sparePart);
    }
}
