package com.nguyen.motoparts.aftersales.repository;

import com.nguyen.motoparts.aftersales.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
}
