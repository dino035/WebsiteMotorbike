package com.nguyen.motoparts.vehicle.repository;

import com.nguyen.motoparts.vehicle.entity.VehicleConditionAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleConditionAssessmentRepository extends JpaRepository<VehicleConditionAssessment, String> {
}
