package com.nguyen.motoparts.cart.repository;

import com.nguyen.motoparts.catalog.entity.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Khóa dòng sản phẩm (SELECT ... FOR UPDATE) để hai khách thêm cùng một chiếc xe vào giỏ cùng lúc chạy lần lượt.
 * Dùng truy vấn thuần vào bảng product vì khóa qua JPA trên lớp cha có kế thừa JOINED sinh ra LEFT JOIN,
 * mà PostgreSQL không cho FOR UPDATE ở phía có thể rỗng của LEFT JOIN.
 */
public interface ProductLockRepository extends Repository<Product, String> {

    @Query(value = "select p.product_id from product p where p.product_id = :id for update", nativeQuery = true)
    List<String> lockById(@Param("id") String id);
}
