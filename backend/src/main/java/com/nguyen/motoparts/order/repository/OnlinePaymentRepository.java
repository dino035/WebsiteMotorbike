package com.nguyen.motoparts.order.repository;

import com.nguyen.motoparts.order.entity.OnlinePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnlinePaymentRepository extends JpaRepository<OnlinePayment, String> {

    Optional<OnlinePayment> findByTxnRef(String txnRef);

    /**
     * Chỉ lấy mã đơn của một giao dịch (không nạp cả bản ghi), để khóa dòng đơn trước rồi mới đọc lại giao dịch:
     * đọc sau khóa thì luôn thấy trạng thái mới nhất, không bị bản cũ trong bộ nhớ đệm của phiên làm việc.
     */
    @Query("select a.orderId from OnlinePayment a where a.txnRef = :txnRef")
    Optional<String> findOrderIdByTxnRef(@Param("txnRef") String txnRef);

    List<OnlinePayment> findByOrderIdOrderByCreatedAtDesc(String orderId);
}
