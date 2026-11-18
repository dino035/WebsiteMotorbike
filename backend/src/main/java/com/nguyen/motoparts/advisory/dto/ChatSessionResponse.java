package com.nguyen.motoparts.advisory.dto;

import java.time.LocalDateTime;
import java.util.List;

/** vehicle là hồ sơ xe mà cuộc trò chuyện đang tư vấn cho; null nếu hỏi chung hoặc hồ sơ đã bị xóa. */
public record ChatSessionResponse(String sessionId, LocalDateTime startedAt, List<ChatMessageResponse> messages,
                                  VehicleProfileResponse vehicle) {
}
