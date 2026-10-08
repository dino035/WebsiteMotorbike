package com.example.websitemotorbike.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SparePartPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String photoId;

    @Column(length = 500)
    private String imageUrl;

    private String caption;

    // Đường dẫn file trong bucket, dùng khi cần xóa file trên Supabase
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sparepart_id")
    private SparePart sparePart;
}