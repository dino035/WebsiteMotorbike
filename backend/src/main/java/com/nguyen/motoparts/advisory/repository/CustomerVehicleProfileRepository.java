package com.nguyen.motoparts.advisory.repository;

import com.nguyen.motoparts.advisory.entity.CustomerVehicleProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerVehicleProfileRepository extends JpaRepository<CustomerVehicleProfile, String> {
}
