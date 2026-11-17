package com.nguyen.motoparts.advisory.controller;

import com.nguyen.motoparts.advisory.dto.AdminChatQuestionRow;
import com.nguyen.motoparts.advisory.dto.AdminChatSessionResponse;
import com.nguyen.motoparts.advisory.service.ChatMessageService;
import com.nguyen.motoparts.common.dto.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Quản trị chat box: xem câu hỏi của khách (lọc chưa trả lời được) và đọc cuộc trò chuyện. Chỉ ADMIN (SecurityConfig chặn /api/admin/**). */
@RestController
@RequestMapping("/api/admin/chat")
public class AdminChatController {

    private final ChatMessageService service;

    public AdminChatController(ChatMessageService service) {
        this.service = service;
    }

    @GetMapping("/questions")
    public PageResponse<AdminChatQuestionRow> questions(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return service.listQuestions(status, Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    @GetMapping("/sessions/{sessionId}")
    public AdminChatSessionResponse session(@PathVariable String sessionId) {
        return service.getSession(sessionId);
    }
}
