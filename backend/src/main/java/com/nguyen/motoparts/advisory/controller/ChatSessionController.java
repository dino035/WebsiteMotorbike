package com.nguyen.motoparts.advisory.controller;

import com.nguyen.motoparts.advisory.dto.CreateChatSessionRequest;
import com.nguyen.motoparts.advisory.dto.ChatSessionResponse;
import com.nguyen.motoparts.advisory.dto.ChatSessionSummary;
import com.nguyen.motoparts.advisory.dto.SendMessageRequest;
import com.nguyen.motoparts.advisory.dto.SendMessageResponse;
import com.nguyen.motoparts.advisory.service.ChatSessionService;
import com.nguyen.motoparts.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Chat box của chính người đang đăng nhập (accountId lấy từ token). */
@RestController
@RequestMapping("/api/chat-sessions")
public class ChatSessionController {

    private final ChatSessionService service;

    public ChatSessionController(ChatSessionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatSessionResponse create(@AuthenticationPrincipal AuthUser user,
                                      @RequestBody(required = false) CreateChatSessionRequest request) {
        return service.create(user.accountId(), request == null ? null : request.profileId());
    }

    @GetMapping
    public List<ChatSessionSummary> list(@AuthenticationPrincipal AuthUser user) {
        return service.listMine(user.accountId());
    }

    @GetMapping("/{sessionId}")
    public ChatSessionResponse detail(@AuthenticationPrincipal AuthUser user, @PathVariable String sessionId) {
        return service.getMine(user.accountId(), sessionId);
    }

    @PostMapping("/{sessionId}/messages")
    public SendMessageResponse send(@AuthenticationPrincipal AuthUser user, @PathVariable String sessionId,
                                    @Valid @RequestBody SendMessageRequest request) {
        return service.send(user.accountId(), sessionId, request.content());
    }
}
