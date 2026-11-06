package com.nguyen.motoparts.order.repository;

import com.nguyen.motoparts.order.entity.InstallationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstallationRequestRepository extends JpaRepository<InstallationRequest, String> {
}
