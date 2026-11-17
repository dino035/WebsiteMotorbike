package com.nguyen.motoparts.advisory.llm;

/** LLM không dùng được lúc này (chưa cấu hình khóa, hết hạn mức, lỗi mạng, phản hồi rỗng hoặc sai dạng). */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message) {
        super(message);
    }

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
