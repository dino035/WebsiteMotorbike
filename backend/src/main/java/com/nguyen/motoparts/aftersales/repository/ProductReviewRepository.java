package com.nguyen.motoparts.aftersales.repository;

import com.nguyen.motoparts.aftersales.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, String> {
    boolean existsByAccountIdAndProduct_ProductId(String accountId, String productId);

    List<ProductReview> findByProduct_ProductIdOrderByCreatedAtDesc(String productId);
}
