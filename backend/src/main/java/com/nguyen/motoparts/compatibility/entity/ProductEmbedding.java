package com.nguyen.motoparts.compatibility.entity;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

/**
 * Tham chiếu: tài liệu "class-diagram-final-spec.md", mục "Compatibility & AI knowledge base".
 * TODO (bước thiết kế schema kế tiếp): khai báo @Id, các thuộc tính và quan hệ
 * (Association / Composition / Aggregation) đúng theo đặc tả đã chốt.
 */
// Nhớ: KHÔNG thêm field 'product' song song với Association tới Product (đã lưu ý trong tài liệu dự án).
@Entity
@Getter
@Setter
public class ProductEmbedding {

    // TODO: fields

}
