package com.nguyen.motoparts.order.repository;

import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    long countByAccountIdAndStatusIn(String accountId, Collection<OrderStatus> statuses);

    /** Đơn của một khách, mới nhất trước; nạp luôn các dòng hàng để đếm số món mà không gọi thêm truy vấn. */
    @EntityGraph(attributePaths = "items")
    List<Order> findByAccountIdOrderByOrderDateDesc(String accountId);

    /** Chỉ trả đơn nếu thuộc về accountId (chống xem đơn của người khác). */
    Optional<Order> findByOrderIdAndAccountId(String orderId, String accountId);

    /** Số đơn theo trạng thái của từng tài khoản trong danh sách; mỗi dòng là {accountId, số đơn}. */
    @Query("select o.accountId, count(o) from Order o "
            + "where o.accountId in :accountIds and o.status in :statuses group by o.accountId")
    List<Object[]> countByAccountIds(@Param("accountIds") Collection<String> accountIds,
                                     @Param("statuses") Collection<OrderStatus> statuses);
}
