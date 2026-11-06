package com.nguyen.motoparts.aftersales.repository;

import com.nguyen.motoparts.aftersales.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, String> {
}
