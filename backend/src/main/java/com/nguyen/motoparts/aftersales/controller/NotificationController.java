package com.nguyen.motoparts.aftersales.controller;

import com.nguyen.motoparts.aftersales.dto.NotificationResponse;
import com.nguyen.motoparts.aftersales.service.NotificationService;
import com.nguyen.motoparts.common.dto.PageResponse;
import com.nguyen.motoparts.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Thông báo của chính người đang đăng nhập (accountId lấy từ token). */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return service.listMine(user.accountId(), Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    /** Số thông báo chưa đọc, cho chuông trên menu. */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthUser user) {
        return Map.of("count", service.unreadCount(user.accountId()));
    }

    @PostMapping("/{notificationId}/read")
    public NotificationResponse markRead(@AuthenticationPrincipal AuthUser user, @PathVariable String notificationId) {
        return service.markRead(user.accountId(), notificationId);
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal AuthUser user) {
        return Map.of("updated", service.markAllRead(user.accountId()));
    }
}
