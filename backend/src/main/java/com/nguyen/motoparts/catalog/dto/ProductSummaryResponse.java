package com.nguyen.motoparts.catalog.dto;

import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.common.enums.ProductStatus;

import java.math.BigDecimal;

public record ProductSummaryResponse(String productId, String name, BigDecimal price, ProductStatus status,
                                     String statusLabel, String typeCode, String productType, String detail,
                                     String thumbnailUrl) {
    public static ProductSummaryResponse from(Product p) {
        String thumb = null;
        if (p instanceof Motorbike m && !m.getPhotos().isEmpty()) {
            thumb = m.getPhotos().get(0).getImageUrl();
        } else if (p instanceof SparePart s && !s.getPhotos().isEmpty()) {
            thumb = s.getPhotos().get(0).getImageUrl();
        }
        return new ProductSummaryResponse(p.getProductId(), p.getName(), p.getPrice(), p.getStatus(), p.getStatus().getLabel(), p.getTypeCode(), p.getProductType(), p.getDetail(), thumb);
    }
}
