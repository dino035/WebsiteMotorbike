package com.nguyen.motoparts.catalog.repository;

import com.nguyen.motoparts.catalog.entity.Motorbike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotorbikeRepository extends JpaRepository<Motorbike, String> {
    boolean existsByChassisNumber(String chassisNumber);

    boolean existsByEngineNumber(String engineNumber);

    boolean existsByChassisNumberAndProductIdNot(String chassisNumber, String productId);

    boolean existsByEngineNumberAndProductIdNot(String engineNumber, String productId);
}
