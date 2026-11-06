package com.nguyen.motoparts.order.repository;

import com.nguyen.motoparts.order.entity.OnlinePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnlinePaymentRepository extends JpaRepository<OnlinePayment, String> {
}
