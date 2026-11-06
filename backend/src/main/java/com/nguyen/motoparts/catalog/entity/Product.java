package com.nguyen.motoparts.catalog.entity;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

/**
 * Tham chiếu: tài liệu "class-diagram-final-spec.md", mục "Product hierarchy".
 * TODO (bước thiết kế schema kế tiếp): khai báo @Id, các thuộc tính và quan hệ
 * (Association / Composition / Aggregation) đúng theo đặc tả đã chốt.
 */
// Lớp cha abstract — dùng @Inheritance khi thiết kế schema (SINGLE_TABLE hoặc JOINED).
@Entity
@Getter
@Setter
public class Product {

    // TODO: fields

}
