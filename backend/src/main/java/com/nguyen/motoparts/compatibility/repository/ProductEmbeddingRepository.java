package com.nguyen.motoparts.compatibility.repository;

import com.nguyen.motoparts.compatibility.entity.ProductEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductEmbeddingRepository extends JpaRepository<ProductEmbedding, String> {
}
