package com.nguyen.motoparts.advisory.dto;

import java.time.LocalDateTime;

/** Một câu hỏi của khách trong danh sách Admin: nội dung, ai hỏi, lúc nào, đã trả lời được chưa. */
public record AdminChatQuestionRow(String messageId, String sessionId, String customerEmail, String content,
                                   String status, LocalDateTime sentAt) {
}
