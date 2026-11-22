package com.nguyen.motoparts.order.repository;

import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.order.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    boolean existsByProductId(String productId);

    /** Khách đã mua sản phẩm này trong một đơn có trạng thái cho trước (dùng cho Verified Purchase). */
    boolean existsByProductIdAndOrder_AccountIdAndOrder_Status(
            String productId, String accountId, OrderStatus status);

    /** Các sản phẩm khách đã mua trong đơn có trạng thái này (thường là Hoàn thành), mới nhất trước. */
    @Query("select i from OrderItem i join fetch i.order o "
            + "where o.accountId = :accountId and o.status = :status order by o.orderDate desc, i.productName")
    List<OrderItem> findByAccountAndOrderStatus(@Param("accountId") String accountId,
                                                @Param("status") OrderStatus status);

    /** Dòng đơn với điều kiện thuộc về accountId (chống gửi bảo hành cho đơn của người khác). */
    @Query("select i from OrderItem i join fetch i.order o where i.orderItemId = :id and o.accountId = :accountId")
    Optional<OrderItem> findOwned(@Param("id") String orderItemId, @Param("accountId") String accountId);

    /** Sản phẩm bán chạy: mỗi dòng {productId, productName, tổng số lượng, tổng tiền}. */
    @Query("select i.productId, i.productName, sum(i.quantity), sum(i.unitPrice * i.quantity) "
            + "from OrderItem i join i.order o "
            + "where o.status = :status and o.orderDate >= :from and o.orderDate < :to "
            + "group by i.productId, i.productName "
            + "order by sum(i.quantity) desc")
    List<Object[]> findTopProducts(@Param("status") OrderStatus status,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   Pageable pageable);
}
