package com.nguyen.motoparts.vehicle.repository;

import com.nguyen.motoparts.vehicle.entity.VehiclePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto, String> {
}
