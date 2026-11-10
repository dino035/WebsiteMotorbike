package com.nguyen.motoparts.catalog.repository;

import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.common.enums.ProductStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String kw = keyword.trim().toLowerCase();
        return (root, query, cb) -> cb.like(cb.lower(root.<String>get("name")), "%" + kw + "%");
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Product> priceAtLeast(BigDecimal min) {
        if (min == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("price"), min);
    }

    public static Specification<Product> priceAtMost(BigDecimal max) {
        if (max == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.<BigDecimal>get("price"), max);
    }

    public static Specification<Product> ofType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        String t = type.trim().toLowerCase();
        if ("motorbike".equals(t)) {
            return (root, query, cb) -> cb.equal(root.type(), Motorbike.class);
        }

        if ("sparepart".equals(t)) {
            return (root, query, cb) -> cb.equal(root.type(), SparePart.class);
        }
        return null;
    }
}