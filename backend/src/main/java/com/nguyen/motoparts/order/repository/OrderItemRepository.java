package com.nguyen.motoparts.order.repository;

import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

    /** Các sản phẩm khách đã mua trong đơn có trạng thái này (thường là Hoàn thành), mới nhất trước. */
    @Query("select i from OrderItem i join fetch i.order o "
            + "where o.accountId = :accountId and o.status = :status order by o.orderDate desc, i.productName")
    List<OrderItem> findByAccountAndOrderStatus(@Param("accountId") String accountId,
                                                @Param("status") OrderStatus status);

    /** Dòng đơn với điều kiện thuộc về accountId (chống gửi bảo hành cho đơn của người khác). */
    @Query("select i from OrderItem i join fetch i.order o where i.orderItemId = :id and o.accountId = :accountId")
    Optional<OrderItem> findOwned(@Param("id") String orderItemId, @Param("accountId") String accountId);
}
