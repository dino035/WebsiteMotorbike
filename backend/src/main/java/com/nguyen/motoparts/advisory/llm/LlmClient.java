package com.nguyen.motoparts.advisory.llm;

import java.util.List;

/**
 * Cổng gọi mô hình ngôn ngữ (LLM). Tách thành interface để đổi nhà cung cấp (Gemini, Groq...) mà không phải
 * sửa AIRecommendationEngineService. Chỉ có hai việc:
 * <ul>
 *   <li>{@link #extractCriteria}: đọc câu hỏi của khách thành tiêu chí có cấu trúc (JSON);</li>
 *   <li>{@link #writeAnswer}: viết lời tư vấn từ danh sách sản phẩm THẬT mà code đã lọc trong CSDL.</li>
 * </ul>
 * LLM không bao giờ tự chọn sản phẩm. Chỉ gửi câu hỏi và dữ liệu sản phẩm, không gửi thông tin cá nhân của khách.
 * Gặp lỗi (chưa có khóa, hết hạn mức, mạng) thì ném {@link LlmUnavailableException}.
 */
public interface LlmClient {

    /** Một lượt trong hội thoại trước đó, để hiểu câu hỏi nối tiếp ("còn chiếc rẻ hơn thì sao?"). */
    record ChatTurn(boolean fromCustomer, String text) {
    }

    SearchCriteria extractCriteria(String question, List<ChatTurn> history);

    String writeAnswer(String question, List<ProductFact> products);
}
