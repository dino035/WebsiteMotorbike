package com.nguyen.motoparts.advisory.repository;

import com.nguyen.motoparts.advisory.entity.ChatSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    /** Chỉ trả cuộc trò chuyện của đúng tài khoản; của người khác coi như không có. */
    @Query("select s from ChatSession s where s.sessionId = :id and s.user.accountId = :accountId")
    Optional<ChatSession> findOwned(@Param("id") String id, @Param("accountId") String accountId);

    @Query("select s from ChatSession s where s.user.accountId = :accountId order by s.startedAt desc, s.sessionId desc")
    List<ChatSession> findMine(@Param("accountId") String accountId, Pageable pageable);

    /** Cho Admin xem: kèm người dùng để lấy email. */
    @Query("select s from ChatSession s join fetch s.user where s.sessionId = :id")
    Optional<ChatSession> findWithUser(@Param("id") String id);
}
