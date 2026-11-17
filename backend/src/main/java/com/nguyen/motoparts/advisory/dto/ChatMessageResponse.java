package com.nguyen.motoparts.advisory.dto;

import com.nguyen.motoparts.catalog.dto.ProductSummaryResponse;

import java.time.LocalDateTime;
import java.util.List;

/** Một tin nhắn: người gửi (CUSTOMER hoặc ASSISTANT), nội dung, trạng thái trả lời (chỉ có ở câu hỏi của khách), sản phẩm gợi ý. */
public record ChatMessageResponse(String messageId, String sender, String content, LocalDateTime sentAt,
                                  String status, List<ProductSummaryResponse> products) {
}
