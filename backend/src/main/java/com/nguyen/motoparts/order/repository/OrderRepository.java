package com.nguyen.motoparts.order.repository;

import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    long countByAccountIdAndStatusIn(String accountId, Collection<OrderStatus> statuses);

    /** Số đơn theo trạng thái của từng tài khoản trong danh sách; mỗi dòng là {accountId, số đơn}. */
    @Query("select o.accountId, count(o) from Order o "
            + "where o.accountId in :accountIds and o.status in :statuses group by o.accountId")
    List<Object[]> countByAccountIds(@Param("accountIds") Collection<String> accountIds,
                                     @Param("statuses") Collection<OrderStatus> statuses);
}
