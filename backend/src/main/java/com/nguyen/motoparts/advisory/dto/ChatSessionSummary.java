package com.nguyen.motoparts.advisory.dto;

import java.time.LocalDateTime;

/** Một dòng trong danh sách cuộc trò chuyện: title là câu hỏi đầu tiên của khách (cắt gọn), null nếu chưa hỏi gì. */
public record ChatSessionSummary(String sessionId, LocalDateTime startedAt, String title, int messageCount) {
}
