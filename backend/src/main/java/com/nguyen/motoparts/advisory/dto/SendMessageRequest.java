package com.nguyen.motoparts.advisory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Vui lòng nhập câu hỏi.")
        @Size(max = 500, message = "Câu hỏi tối đa 500 ký tự.")
        String content) {
}
