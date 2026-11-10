package com.nguyen.motoparts.account.repository;

import com.nguyen.motoparts.account.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    /** Mã còn dùng được (chưa dùng, chưa hết hạn), khóa dòng để hai yêu cầu cùng mã không dùng được cùng lúc. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PasswordResetToken t where t.tokenHash = :hash and t.usedAt is null and t.expiresAt > :now")
    Optional<PasswordResetToken> findUsableForUpdate(@Param("hash") String hash, @Param("now") LocalDateTime now);

    boolean existsByTokenHashAndUsedAtIsNullAndExpiresAtAfter(String tokenHash, LocalDateTime now);

    long countByAccountIdAndCreatedAtAfter(String accountId, LocalDateTime since);

    /** Vô hiệu mọi mã chưa dùng của tài khoản. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PasswordResetToken t set t.usedAt = :now where t.accountId = :accountId and t.usedAt is null")
    int invalidateAll(@Param("accountId") String accountId, @Param("now") LocalDateTime now);

    /** Dọn các mã đã quá cũ cho gọn bảng. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PasswordResetToken t where t.createdAt < :limit")
    int deleteOlderThan(@Param("limit") LocalDateTime limit);
}
