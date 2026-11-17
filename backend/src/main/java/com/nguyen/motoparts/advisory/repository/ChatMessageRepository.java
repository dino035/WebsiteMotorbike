package com.nguyen.motoparts.advisory.repository;

import com.nguyen.motoparts.advisory.entity.ChatMessage;
import com.nguyen.motoparts.common.enums.AnswerStatus;
import com.nguyen.motoparts.common.enums.SenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    /** Số câu hỏi khách đã gửi từ một mốc thời gian, để giới hạn tần suất hỏi (bảo vệ hạn mức LLM). */
    @Query("select count(m) from ChatMessage m where m.session.user.accountId = :accountId "
            + "and m.sender = :sender and m.sentAt >= :since")
    long countSentSince(@Param("accountId") String accountId, @Param("sender") SenderType sender,
                        @Param("since") LocalDateTime since);

    @Query(value = "select m from ChatMessage m join fetch m.session s join fetch s.user "
            + "where m.sender = :sender order by m.sentAt desc, m.messageId desc",
            countQuery = "select count(m) from ChatMessage m where m.sender = :sender")
    Page<ChatMessage> findQuestions(@Param("sender") SenderType sender, Pageable pageable);

    @Query(value = "select m from ChatMessage m join fetch m.session s join fetch s.user "
            + "where m.sender = :sender and m.status = :status order by m.sentAt desc, m.messageId desc",
            countQuery = "select count(m) from ChatMessage m where m.sender = :sender and m.status = :status")
    Page<ChatMessage> findQuestionsByStatus(@Param("sender") SenderType sender, @Param("status") AnswerStatus status,
                                            Pageable pageable);
}
