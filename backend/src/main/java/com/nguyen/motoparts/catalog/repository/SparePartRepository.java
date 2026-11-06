package com.nguyen.motoparts.catalog.repository;

import com.nguyen.motoparts.catalog.entity.SparePart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SparePartRepository extends JpaRepository<SparePart, String> {
}
