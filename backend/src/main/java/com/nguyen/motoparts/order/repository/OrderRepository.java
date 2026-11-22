package com.nguyen.motoparts.order.repository;

import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.common.enums.PaymentMethod;
import com.nguyen.motoparts.common.enums.PaymentStatus;
import com.nguyen.motoparts.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    /** Admin: mọi đơn, mới nhất trước. */
    Page<Order> findAllByOrderByOrderDateDesc(Pageable pageable);

    /** Admin: đơn theo một trạng thái, mới nhất trước. */
    Page<Order> findByStatusOrderByOrderDateDesc(OrderStatus status, Pageable pageable);

    /**
     * Đọc đơn và khóa dòng (SELECT ... FOR UPDATE) để hai Admin không đổi cùng một đơn cùng lúc. Dùng truy vấn JPQL chứ không
     * em.find có khóa, vì em.find nối thêm bảng lắp đặt (LEFT JOIN) mà PostgreSQL không cho FOR UPDATE ở phía có thể rỗng.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId = :orderId")
    Optional<Order> findForUpdate(@Param("orderId") String orderId);

    /**
     * Mã các đơn đang chờ xác nhận, thanh toán bằng VNPay mà chưa trả tiền, đặt từ trước cutoff. Chỉ là danh sách ứng viên
     * (chưa khóa dòng): người gọi khóa từng đơn rồi kiểm tra lại trước khi hủy.
     */
    @Query("select o.orderId from Order o, Payment p where p.orderId = o.orderId and o.status = :status "
            + "and p.method = :method and p.status <> :paid and o.orderDate < :cutoff order by o.orderDate")
    List<String> findUnpaidOrderIds(@Param("status") OrderStatus status, @Param("method") PaymentMethod method,
                                    @Param("paid") PaymentStatus paid, @Param("cutoff") LocalDateTime cutoff);

    /** Số đơn theo trạng thái của từng tài khoản trong danh sách; mỗi dòng là {accountId, số đơn}. */
    @Query("select o.accountId, count(o) from Order o "
            + "where o.accountId in :accountIds and o.status in :statuses group by o.accountId")
    List<Object[]> countByAccountIds(@Param("accountIds") Collection<String> accountIds,
                                     @Param("statuses") Collection<OrderStatus> statuses);

    /** Đơn hoàn tất trong khoảng; chỉ lấy ngày đặt và tổng tiền để tự gộp theo ngày ở Java. */
    @Query("select o.orderDate, o.totalAmount from Order o "
            + "where o.status = :status and o.orderDate >= :from and o.orderDate < :to")
    List<Object[]> findRevenueRows(@Param("status") OrderStatus status,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);
}
