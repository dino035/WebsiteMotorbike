package com.nguyen.motoparts.catalog.repository;

import com.nguyen.motoparts.catalog.entity.SparePartPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SparePartPhotoRepository extends JpaRepository<SparePartPhoto, String> {
}
