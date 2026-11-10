package com.nguyen.motoparts.account.repository;

import com.nguyen.motoparts.account.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Khóa dòng tài khoản (SELECT ... FOR UPDATE) để các thao tác trên sổ địa chỉ của một khách chạy lần lượt. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.accountId = :id")
    Optional<User> findByIdForUpdate(@Param("id") String id);

    /**
     * Danh sách tài khoản khách hàng cho admin. q: từ khóa trong email (đã thoát % _ \, rỗng = tất cả);
     * status: ALL, ACTIVE (chưa bị khóa) hoặc LOCKED (đang bị khóa).
     */
    @Query("""
            select u from User u
            where u.userRole = com.nguyen.motoparts.common.enums.UserRole.CUSTOMER
              and lower(u.email) like lower(concat('%', :q, '%')) escape '\\'
              and (:status = 'ALL'
                   or (:status = 'LOCKED' and exists (select 1 from AccountLock l where l.accountId = u.accountId))
                   or (:status = 'ACTIVE' and not exists (select 1 from AccountLock l where l.accountId = u.accountId)))
            """)
    Page<User> searchCustomers(@Param("q") String q, @Param("status") String status, Pageable pageable);
}
