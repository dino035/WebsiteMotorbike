package com.nguyen.motoparts.advisory.entity;

import com.nguyen.motoparts.common.enums.AnswerStatus;
import com.nguyen.motoparts.common.enums.SenderType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Một tin nhắn trong cuộc trò chuyện.
 * status chỉ có nghĩa với câu hỏi của khách: UNANSWERED khi chat box không tìm được câu trả lời,
 * để Admin lọc và xem lại.
 */
@Entity
@Getter
@Setter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderType sender;

    @Column(length = 4000, nullable = false)
    private String content;

    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    private AnswerStatus status;

    @PrePersist
    protected void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
}
