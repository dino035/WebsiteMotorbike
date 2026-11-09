package com.nguyen.motoparts.advisory.entity;

import com.nguyen.motoparts.account.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Một cuộc trò chuyện với chat box.
 * Theo đặc tả: Association tới User ("held by"), profileId kiểu String và có thể rỗng (hồ sơ xe làm ngữ cảnh,
 * không có đường tới CustomerVehicleProfile), startedAt, Composition 1 — 0..* tới ChatMessage (chatMessage).
 */
@Entity
@Getter
@Setter
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private User user;

    /** Có thể null khi khách hỏi chung, không chọn hồ sơ xe. */
    private String profileId;

    private LocalDateTime startedAt;

    /** Tên số ít "chatMessage" giữ đúng như class diagram. */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt ASC")
    private List<ChatMessage> chatMessage = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        chatMessage.add(message);
        message.setSession(this);
    }

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
}
