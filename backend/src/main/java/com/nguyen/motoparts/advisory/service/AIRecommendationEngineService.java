package com.nguyen.motoparts.advisory.service;

import com.nguyen.motoparts.advisory.llm.LlmClient;
import com.nguyen.motoparts.advisory.llm.LlmUnavailableException;
import com.nguyen.motoparts.advisory.llm.ProductFact;
import com.nguyen.motoparts.advisory.llm.SearchCriteria;
import com.nguyen.motoparts.common.enums.AnswerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Bộ máy gợi ý của chat box. Ba bước, LLM chỉ làm bước 1 và 3:
 * <ol>
 *   <li>LLM đọc câu hỏi thành tiêu chí có cấu trúc ({@link SearchCriteria});</li>
 *   <li>code lọc sản phẩm thật trong CSDL ({@link ChatProductSearch}): chỉ hàng còn bán, phụ tùng theo luật tương thích;</li>
 *   <li>LLM viết lời tư vấn dựa trên đúng các sản phẩm đã lọc.</li>
 * </ol>
 * Không chạy trong giao dịch CSDL (gọi LLM có thể mất vài giây). Câu hỏi không trả lời được (ngoài phạm vi, không có
 * sản phẩm phù hợp, LLM không dùng được) có trạng thái UNANSWERED để Admin xem lại.
 */
@Service
public class AIRecommendationEngineService {

    private static final Logger log = LoggerFactory.getLogger(AIRecommendationEngineService.class);

    static final String REPLY_GREETING = "Xin chào! Mình là trợ lý của MotoParts. Bạn đang tìm xe máy cũ hay phụ tùng? "
            + "Cứ cho mình biết hãng xe, đời xe và ngân sách nhé.";
    static final String REPLY_OUT_OF_SCOPE = "Mình chỉ tư vấn xe máy cũ và phụ tùng của cửa hàng. Bạn có thể hỏi kiểu: "
            + "\"nhớt cho Honda Wave 2015\" hoặc \"xe số dưới 20 triệu\". Câu hỏi của bạn đã được ghi lại để cửa hàng xem.";
    static final String REPLY_NO_PRODUCT = "Hiện cửa hàng chưa có sản phẩm còn hàng khớp với yêu cầu của bạn. "
            + "Câu hỏi đã được ghi lại để cửa hàng xem và bổ sung. Bạn thử đổi tiêu chí (giá, đời xe, loại phụ tùng) nhé.";
    static final String REPLY_UNKNOWN_VEHICLE = "Mình chưa có dữ liệu về dòng xe này nên chưa gợi ý được phụ tùng phù hợp. "
            + "Câu hỏi đã được ghi lại để cửa hàng bổ sung. Bạn thử nêu hãng, dòng và năm đăng ký xe khác nhé.";
    static final String REPLY_BUSY = "Hiện hệ thống tư vấn đang bận nên mình chưa trả lời được câu này. "
            + "Câu hỏi của bạn đã được ghi lại để cửa hàng xem và hỗ trợ sau.";

    /** Kết quả một lượt hỏi: trạng thái, lời đáp, và mã các sản phẩm được gợi ý (có thể rỗng). */
    public record Recommendation(AnswerStatus status, String text, List<String> productIds) {
    }

    private final LlmClient llm;
    private final ChatProductSearch search;

    public AIRecommendationEngineService(LlmClient llm, ChatProductSearch search) {
        this.llm = llm;
        this.search = search;
    }

    public Recommendation recommend(String question, List<LlmClient.ChatTurn> history) {
        SearchCriteria criteria;
        try {
            criteria = llm.extractCriteria(question, history).sanitized();
        } catch (LlmUnavailableException e) {
            log.warn("Khong doc duoc cau hoi bang LLM: {}", e.getMessage());
            return new Recommendation(AnswerStatus.UNANSWERED, REPLY_BUSY, List.of());
        }

        switch (criteria.intent()) {
            case GREETING:
                return new Recommendation(AnswerStatus.ANSWERED, REPLY_GREETING, List.of());
            case OTHER:
                return new Recommendation(AnswerStatus.UNANSWERED, REPLY_OUT_OF_SCOPE, List.of());
            default:
                break;
        }

        ChatProductSearch.Found found = search.search(criteria);
        if (found.productIds().isEmpty()) {
            return new Recommendation(AnswerStatus.UNANSWERED,
                    found.vehicleUnknown() ? REPLY_UNKNOWN_VEHICLE : REPLY_NO_PRODUCT, List.of());
        }

        String text;
        try {
            text = llm.writeAnswer(question, found.facts());
        } catch (LlmUnavailableException e) {
            log.warn("Khong viet duoc loi tu van bang LLM, dung mau co san: {}", e.getMessage());
            text = null;
        }
        if (text == null || text.isBlank()) {
            text = fallbackText(found.facts());
        }
        return new Recommendation(AnswerStatus.ANSWERED, text, found.productIds());
    }

    /** Lời đáp dựng sẵn khi LLM không viết được: vẫn có sản phẩm thật nên câu hỏi vẫn được coi là đã trả lời. */
    static String fallbackText(List<ProductFact> facts) {
        StringBuilder sb = new StringBuilder("Mình tìm được ").append(facts.size()).append(" sản phẩm phù hợp:");
        for (ProductFact f : facts) {
            sb.append("\n- ").append(f.name()).append(": ")
                    .append(String.format(Locale.US, "%,d", f.price()).replace(',', '.')).append(" đồng");
        }
        return sb.toString();
    }
}
