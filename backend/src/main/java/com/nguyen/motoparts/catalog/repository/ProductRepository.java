package com.nguyen.motoparts.catalog.repository;

import com.nguyen.motoparts.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    /** Đếm sản phẩm theo loại và trạng thái: mỗi dòng {lớp con, trạng thái, số lượng}. */
    @Query("select type(p), p.status, count(p) from Product p group by type(p), p.status")
    List<Object[]> countByTypeAndStatus();
}
