package com.example.websitemotorbike.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
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

    public abstract String getProductType();
    public abstract String getDetail();
    public abstract String getTypeCode();
}
