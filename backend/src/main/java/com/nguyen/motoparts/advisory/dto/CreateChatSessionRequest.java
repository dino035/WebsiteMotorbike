package com.nguyen.motoparts.advisory.dto;

/** Tạo cuộc trò chuyện mới; profileId là hồ sơ xe làm ngữ cảnh, bỏ trống nếu khách hỏi chung. */
public record CreateChatSessionRequest(String profileId) {
}
