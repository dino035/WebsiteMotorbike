package com.nguyen.motoparts.advisory.service;

import com.nguyen.motoparts.advisory.dto.AdminChatQuestionRow;
import com.nguyen.motoparts.advisory.dto.AdminChatSessionResponse;
import com.nguyen.motoparts.advisory.entity.ChatMessage;
import com.nguyen.motoparts.advisory.entity.ChatSession;
import com.nguyen.motoparts.advisory.repository.ChatMessageRepository;
import com.nguyen.motoparts.advisory.repository.ChatSessionRepository;
import com.nguyen.motoparts.common.dto.PageResponse;
import com.nguyen.motoparts.common.enums.AnswerStatus;
import com.nguyen.motoparts.common.enums.SenderType;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Admin xem lại các câu hỏi của khách (lọc UNANSWERED để biết khách đang cần gì mà cửa hàng chưa có) và đọc cuộc trò chuyện. */
@Service
public class ChatMessageService {

    private final ChatMessageRepository messages;
    private final ChatSessionRepository sessions;
    private final ChatSessionService sessionService;

    public ChatMessageService(ChatMessageRepository messages, ChatSessionRepository sessions, ChatSessionService sessionService) {
        this.messages = messages;
        this.sessions = sessions;
        this.sessionService = sessionService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminChatQuestionRow> listQuestions(String statusText, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<ChatMessage> result;
        if (statusText == null || statusText.isBlank()) {
            result = messages.findQuestions(SenderType.CUSTOMER, pageable);
        } else {
            AnswerStatus status;
            try {
                status = AnswerStatus.valueOf(statusText.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái không hợp lệ.");
            }
            result = messages.findQuestionsByStatus(SenderType.CUSTOMER, status, pageable);
        }
        return PageResponse.from(result.map(m -> new AdminChatQuestionRow(
                m.getMessageId(), m.getSession().getSessionId(), m.getSession().getUser().getEmail(), m.getContent(),
                m.getStatus() == null ? null : m.getStatus().name(), m.getSentAt())));
    }

    @Transactional(readOnly = true)
    public AdminChatSessionResponse getSession(String sessionId) {
        ChatSession session = sessions.findWithUser(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện này."));
        return new AdminChatSessionResponse(session.getUser().getEmail(), sessionService.toResponse(session));
    }
}
