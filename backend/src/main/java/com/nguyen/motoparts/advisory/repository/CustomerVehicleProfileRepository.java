package com.nguyen.motoparts.advisory.repository;

import com.nguyen.motoparts.advisory.entity.CustomerVehicleProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerVehicleProfileRepository extends JpaRepository<CustomerVehicleProfile, String> {

    /** Hồ sơ xe của một khách, mới nhất trước; kèm dòng xe để không phải truy vấn thêm từng dòng. */
    @Query("select p from CustomerVehicleProfile p join fetch p.vehicleModel "
            + "where p.accountId = :accountId order by p.createdAt desc, p.profileId")
    List<CustomerVehicleProfile> findMine(@Param("accountId") String accountId);

    /** Chỉ trả hồ sơ của đúng tài khoản; của người khác coi như không có. */
    @Query("select p from CustomerVehicleProfile p join fetch p.vehicleModel "
            + "where p.profileId = :id and p.accountId = :accountId")
    Optional<CustomerVehicleProfile> findOwned(@Param("id") String id, @Param("accountId") String accountId);

    long countByAccountId(String accountId);
}
