package com.nguyen.motoparts.catalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Ảnh phụ tùng (giảng viên đã xác nhận). Theo đặc tả: photoId, imageUrl, caption; cài đặt giống VehiclePhoto.
 * Giữ thêm từ WebsiteMotorbike: storagePath (đường dẫn file trong bucket, cần để xóa file trên Supabase).
 */
@Entity
@Getter
@Setter
public class SparePartPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String photoId;

    @Column(length = 500, nullable = false)
    private String imageUrl;

    private String caption;

    @Column(length = 500)
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spare_part_id")
    private SparePart sparePart;
}
