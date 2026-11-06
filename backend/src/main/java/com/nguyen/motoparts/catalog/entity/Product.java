package com.nguyen.motoparts.catalog.entity;

import com.nguyen.motoparts.common.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Sản phẩm (abstract), lớp cha của Motorbike và SparePart (đặc tả mục Product hierarchy).
 * Kế thừa JOINED như WebsiteMotorbike: bảng product giữ phần chung, bảng motorbike / spare_part giữ phần riêng,
 * nối bằng cùng khóa chính product_id.
 * Thuộc tính embedding: ProductEmbedding trong đặc tả thuộc giai đoạn Khóa luận nên chưa khai báo ở đây.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected String productId;

    @Column(nullable = false)
    protected String name;

    @Column(nullable = false)
    protected BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    protected ProductStatus status;

    /** Chữ hiển thị cho người xem ("Xe máy" / "Phụ tùng"). */
    public abstract String getProductType();

    /** Mô tả ngắn của sản phẩm. */
    public abstract String getDetail();

    /** Mã loại cố định để code so sánh ("Motorbike" / "SparePart"). */
    public abstract String getTypeCode();
}
