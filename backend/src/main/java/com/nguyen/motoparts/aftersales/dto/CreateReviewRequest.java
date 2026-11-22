package com.nguyen.motoparts.aftersales.dto;

import jakarta.validation.constraints.*;

/** Khách gửi đánh giá cho một sản phẩm đã mua. */
public record CreateReviewRequest(

        @NotNull(message = "Vui lòng chọn số sao.")
        @Min(value = 1, message = "Điểm đánh giá phải từ 1 đến 5.")
        @Max(value = 5, message = "Điểm đánh giá phải từ 1 đến 5.")
        Integer rating,

        @NotBlank(message = "Vui lòng nhập nhận xét.")
        @Size(max = 1000, message = "Nhận xét tối đa 1000 ký tự.")
        String comment
) {
}