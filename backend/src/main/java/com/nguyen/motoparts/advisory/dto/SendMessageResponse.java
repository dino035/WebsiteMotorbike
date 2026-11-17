package com.nguyen.motoparts.advisory.dto;

/** Kết quả một lượt hỏi: câu hỏi của khách (đã có trạng thái ANSWERED/UNANSWERED) và câu trả lời của trợ lý. */
public record SendMessageResponse(ChatMessageResponse customerMessage, ChatMessageResponse assistantMessage) {
}
