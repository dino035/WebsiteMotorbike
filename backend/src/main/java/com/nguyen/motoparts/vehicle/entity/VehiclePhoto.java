package com.nguyen.motoparts.vehicle.entity;

import com.nguyen.motoparts.catalog.entity.Motorbike;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Ảnh xe (đặc tả: photoId, imageUrl, caption; Composition 1 — 1..* từ Motorbike).
 * Giữ thêm từ WebsiteMotorbike: storagePath (đường dẫn file trong bucket, cần để xóa file trên Supabase).
 */
@Entity
@Getter
@Setter
public class VehiclePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String photoId;

    @Column(length = 500, nullable = false)
    private String imageUrl;

    private String caption;

    @Column(length = 500)
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorbike_id")
    private Motorbike motorbike;
}
