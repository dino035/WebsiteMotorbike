package com.nguyen.motoparts.catalog.repository;

import com.nguyen.motoparts.catalog.entity.Motorbike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MotorbikeRepository extends JpaRepository<Motorbike, String> {
}
