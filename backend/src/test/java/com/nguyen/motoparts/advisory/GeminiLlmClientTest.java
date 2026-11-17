package com.nguyen.motoparts.advisory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.advisory.llm.GeminiLlmClient;
import com.nguyen.motoparts.advisory.llm.LlmClient;
import com.nguyen.motoparts.advisory.llm.LlmUnavailableException;
import com.nguyen.motoparts.advisory.llm.ProductFact;
import com.nguyen.motoparts.advisory.llm.SearchCriteria;
import com.nguyen.motoparts.common.enums.PartOriginType;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử GeminiLlmClient với một máy chủ HTTP giả (không gọi Gemini thật, không cần CSDL): dạng yêu cầu gửi đi,
 * cách đọc kết quả, và mọi kiểu lỗi đều thành LlmUnavailableException.
 */
class GeminiLlmClientTest {

    private static final String KEY = "test-key-not-real";

    private final ObjectMapper json = new ObjectMapper();
    private HttpServer server;
    private String baseUrl;

    // Phản hồi giả: trạng thái và nội dung; yêu cầu cuối cùng nhận được
    private volatile int replyStatus = 200;
    private volatile String replyBody = "";
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastQuery = new AtomicReference<>();
    private final AtomicReference<String> lastKeyHeader = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicInteger calls = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            lastPath.set(exchange.getRequestURI().getPath());
            lastQuery.set(exchange.getRequestURI().getQuery());
            lastKeyHeader.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] out = replyBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(replyStatus, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1beta";
    }

    @AfterEach
    void stopServer() {
        try {
            server.stop(0);   // có test đã tắt máy chủ trước
        } catch (RuntimeException ignored) {
            // không sao
        }
    }

    private GeminiLlmClient client(String key) {
        return new GeminiLlmClient(json, key, "gemini-test-model", baseUrl, 5);
    }

    /** Bọc văn bản thành phản hồi dạng Gemini. */
    private String geminiReply(String text) throws Exception {
        return json.writeValueAsString(json.readTree(
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + json.writeValueAsString(text) + "}]}}]}"));
    }

    @Test
    void extractCriteriaSendsKeyInHeaderAndParsesResult() throws Exception {
        replyBody = geminiReply("""
                {"intent":"SEARCH","kind":"SPARE_PART","keyword":"nhớt","brand":"Honda","model":"Wave Alpha",
                 "vehicleYear":2015,"minPrice":null,"maxPrice":200000,"partType":"nhớt","partOrigin":"GENUINE",
                 "minRegistrationYear":null,"maxRegistrationYear":null}""");

        SearchCriteria c = client(KEY).extractCriteria("Nhớt chính hãng cho Wave Alpha 2015 dưới 200k",
                List.of(new LlmClient.ChatTurn(true, "xin chào"), new LlmClient.ChatTurn(false, "chào bạn")));

        assertEquals(SearchCriteria.Intent.SEARCH, c.intent());
        assertEquals(SearchCriteria.Kind.SPARE_PART, c.kind());
        assertEquals("nhớt", c.keyword());
        assertEquals("Honda", c.brand());
        assertEquals("Wave Alpha", c.model());
        assertEquals(2015, c.vehicleYear());
        assertNull(c.minPrice());
        assertEquals(200000L, c.maxPrice());
        assertEquals(PartOriginType.GENUINE, c.partOrigin());

        // Yêu cầu gửi đi: đúng đường dẫn model, khóa nằm trong header chứ không nằm trên URL
        assertEquals("/v1beta/models/gemini-test-model:generateContent", lastPath.get());
        assertEquals(KEY, lastKeyHeader.get());
        assertTrue(lastQuery.get() == null || !lastQuery.get().contains(KEY), "khóa không được nằm trên URL");
        JsonNode sent = json.readTree(lastBody.get());
        String userText = sent.path("contents").path(0).path("parts").path(0).path("text").asText();
        assertTrue(userText.contains("Nhớt chính hãng cho Wave Alpha 2015 dưới 200k"));
        assertTrue(userText.contains("xin chào") && userText.contains("chào bạn"), "lịch sử phải được gửi kèm");
        assertEquals("application/json", sent.path("generationConfig").path("responseMimeType").asText());
        assertTrue(sent.path("generationConfig").path("responseSchema").path("properties").has("maxPrice"));
        assertFalse(lastBody.get().contains(KEY), "khóa không được nằm trong nội dung gửi đi");
    }

    @Test
    void extractCriteriaToleratesMessyValues() throws Exception {
        // intent lạ, kind sai kiểu, giá âm, năm vô lý, khoảng đảo ngược, chuỗi quá dài
        String longText = "x".repeat(500);
        replyBody = geminiReply("{\"intent\":\"hack\",\"kind\":5,\"keyword\":\"" + longText + "\",\"vehicleYear\":3000,"
                + "\"minPrice\":-5,\"maxPrice\":\"rẻ\",\"minRegistrationYear\":2020,\"maxRegistrationYear\":2010,"
                + "\"partOrigin\":\"NOPE\"}");

        SearchCriteria c = client(KEY).extractCriteria("câu hỏi", List.of());

        assertEquals(SearchCriteria.Intent.OTHER, c.intent(), "intent lạ coi như ngoài phạm vi");
        assertEquals(SearchCriteria.Kind.ANY, c.kind());
        assertEquals(60, c.keyword().length(), "chuỗi bị cắt gọn");
        assertNull(c.vehicleYear());
        assertNull(c.minPrice());
        assertNull(c.maxPrice());
        assertNull(c.partOrigin());
        assertEquals(2010, c.minRegistrationYear(), "khoảng năm đảo ngược được đổi chỗ");
        assertEquals(2020, c.maxRegistrationYear());
    }

    @Test
    void sanitizedSwapsPriceRange() {
        SearchCriteria c = new SearchCriteria(SearchCriteria.Intent.SEARCH, SearchCriteria.Kind.MOTORBIKE, "  ", " Honda ",
                null, null, 20_000_000L, 10_000_000L, null, null, null, null).sanitized();
        assertEquals(10_000_000L, c.minPrice());
        assertEquals(20_000_000L, c.maxPrice());
        assertNull(c.keyword(), "chuỗi toàn khoảng trắng coi như không có");
        assertEquals("Honda", c.brand());
    }

    @Test
    void writeAnswerReturnsTextAndSendsOnlyProductFacts() throws Exception {
        replyBody = geminiReply("  Mình gợi ý Nhớt Castrol vì hợp với xe của bạn.  ");

        String answer = client(KEY).writeAnswer("nhớt cho Wave",
                List.of(new ProductFact("Nhớt Castrol 1L", "Phụ tùng", 125000, "nhớt (Chính hãng)")));

        assertEquals("Mình gợi ý Nhớt Castrol vì hợp với xe của bạn.", answer);
        JsonNode sent = json.readTree(lastBody.get());
        String userText = sent.path("contents").path(0).path("parts").path(0).path("text").asText();
        assertTrue(userText.contains("Nhớt Castrol 1L"));
        assertTrue(userText.contains("125.000"), "giá có dấu chấm ngăn cách hàng nghìn");
        assertFalse(sent.path("generationConfig").has("responseSchema"), "lời tư vấn là văn bản thường, không ép JSON");
    }

    @Test
    void missingKeyFailsWithoutAnyRequest() {
        LlmClient noKey = client("   ");
        assertThrows(LlmUnavailableException.class, () -> noKey.extractCriteria("hỏi", List.of()));
        assertThrows(LlmUnavailableException.class, () -> noKey.writeAnswer("hỏi", List.of()));
        assertEquals(0, calls.get(), "không có khóa thì không được gọi ra ngoài");
    }

    @Test
    void httpErrorsBecomeUnavailable() {
        for (int status : new int[]{400, 403, 429, 500, 503}) {
            replyStatus = status;
            replyBody = "{\"error\":{\"message\":\"quota\"}}";
            assertThrows(LlmUnavailableException.class, () -> client(KEY).extractCriteria("hỏi", List.of()), "HTTP " + status);
        }
    }

    @Test
    void emptyOrBlockedReplyBecomesUnavailable() {
        replyBody = "{\"candidates\":[]}";   // bị chặn bởi bộ lọc an toàn thì không có candidates
        assertThrows(LlmUnavailableException.class, () -> client(KEY).writeAnswer("hỏi", List.of()));
    }

    @Test
    void invalidCriteriaJsonBecomesUnavailable() throws Exception {
        replyBody = geminiReply("không phải JSON {{{");
        assertThrows(LlmUnavailableException.class, () -> client(KEY).extractCriteria("hỏi", List.of()));
        replyBody = geminiReply("[1,2,3]");
        assertThrows(LlmUnavailableException.class, () -> client(KEY).extractCriteria("hỏi", List.of()));
    }

    @Test
    void unreachableServerBecomesUnavailable() {
        server.stop(0);
        assertThrows(LlmUnavailableException.class, () -> client(KEY).extractCriteria("hỏi", List.of()));
    }
}
