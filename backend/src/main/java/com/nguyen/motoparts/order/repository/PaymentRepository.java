package com.nguyen.motoparts.order.repository;

import com.nguyen.motoparts.order.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /** Mỗi đơn đúng một bản ghi thanh toán (orderId là duy nhất). */
    Optional<Payment> findByOrderId(String orderId);

    List<Payment> findByOrderIdIn(Collection<String> orderIds);
}
