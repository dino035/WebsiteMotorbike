package com.nguyen.motoparts.advisory.service;

import com.nguyen.motoparts.account.entity.User;
import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.advisory.dto.ChatMessageResponse;
import com.nguyen.motoparts.advisory.dto.ChatSessionResponse;
import com.nguyen.motoparts.advisory.dto.ChatSessionSummary;
import com.nguyen.motoparts.advisory.dto.SendMessageResponse;
import com.nguyen.motoparts.advisory.entity.ChatMessage;
import com.nguyen.motoparts.advisory.entity.ChatSession;
import com.nguyen.motoparts.advisory.llm.LlmClient;
import com.nguyen.motoparts.advisory.repository.ChatMessageRepository;
import com.nguyen.motoparts.advisory.repository.ChatSessionRepository;
import com.nguyen.motoparts.catalog.dto.ProductSummaryResponse;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.AnswerStatus;
import com.nguyen.motoparts.common.enums.SenderType;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.common.exception.TooManyRequestsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cuộc trò chuyện với chat box của khách. Mọi thao tác giới hạn theo accountId lấy từ token.
 * <p>
 * Gửi tin nhắn đi qua ba bước để KHÔNG giữ giao dịch CSDL trong lúc chờ LLM: (1) lưu câu hỏi, (2) nhờ
 * {@link AIRecommendationEngineService} trả lời, (3) lưu câu trả lời và trạng thái ANSWERED/UNANSWERED.
 */
@Service
public class ChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionService.class);

    private static final int HISTORY_TURNS = 6;
    private static final int MAX_MESSAGES_PER_SESSION = 100;
    private static final int MAX_LISTED_SESSIONS = 20;
    private static final int MAX_TITLE_CHARS = 60;
    private static final int MAX_REPLY_CHARS = 4000;

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final UserRepository users;
    private final ProductRepository products;
    private final AIRecommendationEngineService engine;
    private final TransactionTemplate tx;
    private final int maxQuestionsPerHour;

    public ChatSessionService(ChatSessionRepository sessions, ChatMessageRepository messages, UserRepository users,
                              ProductRepository products, AIRecommendationEngineService engine,
                              PlatformTransactionManager transactionManager,
                              @Value("${app.chat.max-messages-per-hour:30}") int maxQuestionsPerHour) {
        this.sessions = sessions;
        this.messages = messages;
        this.users = users;
        this.products = products;
        this.engine = engine;
        this.tx = new TransactionTemplate(transactionManager);
        this.maxQuestionsPerHour = maxQuestionsPerHour;
    }

    @Transactional
    public ChatSessionResponse create(String accountId) {
        User user = users.findById(accountId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        ChatSession session = new ChatSession();
        session.setUser(user);
        sessions.save(session);
        return new ChatSessionResponse(session.getSessionId(), session.getStartedAt(), List.of());
    }

    @Transactional(readOnly = true)
    public List<ChatSessionSummary> listMine(String accountId) {
        return sessions.findMine(accountId, PageRequest.of(0, MAX_LISTED_SESSIONS)).stream().map(s -> {
            String title = s.getChatMessage().stream()
                    .filter(m -> m.getSender() == SenderType.CUSTOMER)
                    .map(ChatMessage::getContent)
                    .findFirst()
                    .map(t -> t.length() > MAX_TITLE_CHARS ? t.substring(0, MAX_TITLE_CHARS) : t)
                    .orElse(null);
            return new ChatSessionSummary(s.getSessionId(), s.getStartedAt(), title, s.getChatMessage().size());
        }).toList();
    }

    @Transactional(readOnly = true)
    public ChatSessionResponse getMine(String accountId, String sessionId) {
        ChatSession session = sessions.findOwned(sessionId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện này."));
        return toResponse(session);
    }

    /** Dùng chung cho Admin xem: đổi cuộc trò chuyện thành phản hồi (kèm sản phẩm đã gợi ý). Gọi trong giao dịch. */
    @Transactional(readOnly = true)
    public ChatSessionResponse toResponse(ChatSession session) {
        return new ChatSessionResponse(session.getSessionId(), session.getStartedAt(), toMessageResponses(session.getChatMessage()));
    }

    private record Prepared(String questionId, LocalDateTime questionSentAt, List<LlmClient.ChatTurn> history) {
    }

    public SendMessageResponse send(String accountId, String sessionId, String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw new BadRequestException("Vui lòng nhập câu hỏi.");
        }

        // 1) lưu câu hỏi (kiểm tra chủ sở hữu, độ dài cuộc trò chuyện, tần suất hỏi)
        Prepared prepared = tx.execute(status -> {
            ChatSession session = sessions.findOwned(sessionId, accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cuộc trò chuyện này."));
            if (session.getChatMessage().size() >= MAX_MESSAGES_PER_SESSION) {
                throw new BadRequestException("Cuộc trò chuyện đã quá dài, vui lòng bắt đầu cuộc trò chuyện mới.");
            }
            long recent = messages.countSentSince(accountId, SenderType.CUSTOMER, LocalDateTime.now().minusHours(1));
            if (recent >= maxQuestionsPerHour) {
                throw new TooManyRequestsException("Bạn hỏi hơi nhiều trong một giờ, vui lòng thử lại sau ít phút.");
            }
            List<LlmClient.ChatTurn> history = lastTurns(session.getChatMessage());

            ChatMessage question = new ChatMessage();
            question.setSender(SenderType.CUSTOMER);
            question.setContent(content);
            session.addMessage(question);
            messages.saveAndFlush(question);
            return new Prepared(question.getMessageId(), question.getSentAt(), history);
        });

        // 2) trả lời (có thể mất vài giây, không giữ giao dịch)
        AIRecommendationEngineService.Recommendation recommendation;
        try {
            recommendation = engine.recommend(content, prepared.history());
        } catch (RuntimeException e) {
            log.error("Chat AI loi bat ngo", e);
            recommendation = new AIRecommendationEngineService.Recommendation(
                    AnswerStatus.UNANSWERED, AIRecommendationEngineService.REPLY_BUSY, List.of());
        }
        AIRecommendationEngineService.Recommendation answer = recommendation;

        // 3) lưu câu trả lời và trạng thái của câu hỏi
        return tx.execute(status -> {
            ChatMessage question = messages.findById(prepared.questionId()).orElseThrow();
            question.setStatus(answer.status());

            ChatMessage reply = new ChatMessage();
            reply.setSender(SenderType.ASSISTANT);
            String text = answer.text();
            reply.setContent(text.length() > MAX_REPLY_CHARS ? text.substring(0, MAX_REPLY_CHARS) : text);
            if (!answer.productIds().isEmpty()) {
                reply.setProductIds(String.join(",", answer.productIds()));
            }
            LocalDateTime now = LocalDateTime.now();
            // sentAt của trả lời luôn sau câu hỏi để thứ tự hiển thị không bị lẫn
            reply.setSentAt(now.isAfter(prepared.questionSentAt()) ? now : prepared.questionSentAt().plusNanos(1000));
            question.getSession().addMessage(reply);
            messages.saveAndFlush(reply);

            List<ChatMessageResponse> both = toMessageResponses(List.of(question, reply));
            return new SendMessageResponse(both.get(0), both.get(1));
        });
    }

    // ---------------------------------------------------------------- chung

    private static List<LlmClient.ChatTurn> lastTurns(List<ChatMessage> all) {
        int from = Math.max(all.size() - HISTORY_TURNS, 0);
        List<LlmClient.ChatTurn> turns = new ArrayList<>();
        for (ChatMessage m : all.subList(from, all.size())) {
            turns.add(new LlmClient.ChatTurn(m.getSender() == SenderType.CUSTOMER, m.getContent()));
        }
        return turns;
    }

    /** Đổi tin nhắn thành phản hồi, nạp sản phẩm đã gợi ý bằng một truy vấn. Gọi trong giao dịch (ảnh sản phẩm nạp lười). */
    List<ChatMessageResponse> toMessageResponses(List<ChatMessage> list) {
        Set<String> ids = new LinkedHashSet<>();
        for (ChatMessage m : list) {
            ids.addAll(idsOf(m));
        }
        Map<String, Product> byId = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Product p : products.findAllById(ids)) {
                byId.put(p.getProductId(), p);
            }
        }
        List<ChatMessageResponse> out = new ArrayList<>();
        for (ChatMessage m : list) {
            List<ProductSummaryResponse> cards = idsOf(m).stream()
                    .map(byId::get)
                    .filter(p -> p != null)   // sản phẩm đã bị xóa thì bỏ qua
                    .map(ProductSummaryResponse::from)
                    .toList();
            out.add(new ChatMessageResponse(m.getMessageId(), m.getSender().name(), m.getContent(), m.getSentAt(),
                    m.getStatus() == null ? null : m.getStatus().name(), cards));
        }
        return out;
    }

    private static List<String> idsOf(ChatMessage m) {
        if (m.getProductIds() == null || m.getProductIds().isBlank()) {
            return List.of();
        }
        return Arrays.stream(m.getProductIds().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
