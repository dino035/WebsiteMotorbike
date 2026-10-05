package com.example.websitemotorbike.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected String productId;

    protected String name;
    protected BigDecimal price;

    @Enumerated(EnumType.STRING)
    protected ProductStatus status;

    public abstract String getProductType();
    public abstract String getDetail();
    public abstract String getTypeCode();
}
