package com.nguyen.motoparts.vehicle.repository;

import com.nguyen.motoparts.vehicle.entity.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, String> {
}
