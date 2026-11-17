package com.nguyen.motoparts.advisory.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatSessionResponse(String sessionId, LocalDateTime startedAt, List<ChatMessageResponse> messages) {
}
