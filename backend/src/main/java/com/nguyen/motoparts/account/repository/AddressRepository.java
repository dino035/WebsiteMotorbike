package com.nguyen.motoparts.account.repository;

import com.nguyen.motoparts.account.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {

    /** Sổ địa chỉ của tài khoản: địa chỉ mặc định lên đầu, rồi theo thứ tự tạo. */
    List<Address> findByUser_AccountIdOrderByDefaultAddressDescCreatedAtAsc(String accountId);

    /** Chỉ trả địa chỉ nếu thuộc về accountId (chống xem hoặc sửa địa chỉ của người khác). */
    Optional<Address> findByAddressIdAndUser_AccountId(String addressId, String accountId);

    Optional<Address> findFirstByUser_AccountIdOrderByCreatedAtAsc(String accountId);

    long countByUser_AccountId(String accountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Address a set a.defaultAddress = false where a.user.accountId = :accountId")
    int clearDefault(@Param("accountId") String accountId);
}
