package com.nguyen.motoparts.advisory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.advisory.llm.LlmClient;
import com.nguyen.motoparts.advisory.llm.LlmUnavailableException;
import com.nguyen.motoparts.advisory.llm.ProductFact;
import com.nguyen.motoparts.advisory.llm.SearchCriteria;
import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.PartOriginType;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.compatibility.entity.CompatibilityRule;
import com.nguyen.motoparts.compatibility.repository.CompatibilityRuleRepository;
import com.nguyen.motoparts.vehicle.entity.VehicleModel;
import com.nguyen.motoparts.vehicle.repository.VehicleModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Kiểm thử chat box AI trên toàn bộ ứng dụng + PostgreSQL thật. LLM được thay bằng bản giả (không gọi Gemini):
 * điều cần chứng minh là code lọc đúng sản phẩm thật trong CSDL, lưu hội thoại, đánh dấu câu hỏi chưa trả lời được.
 * Chạy khi có TEST_DB_URL (xem AuthSecurityTest).
 */
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@SpringBootTest(
        classes = MotoPartsApplication.class,
        properties = {
                "spring.datasource.url=${TEST_DB_URL}",
                "spring.datasource.username=${TEST_DB_USERNAME:postgres}",
                "spring.datasource.password=${TEST_DB_PASSWORD:}",
                "spring.jpa.hibernate.ddl-auto=update",
                "spring.jpa.show-sql=false",
                "app.jwt.secret=test-secret-test-secret-test-secret-0123456789",
                "app.admin.email=admin-test@example.com",
                "app.admin.password=AdminPass123",
                "app.cart.hold-minutes=30",
                "app.chat.max-messages-per-hour=6"
        })
@AutoConfigureMockMvc
class ChatTest {

    private static final String PASSWORD = "Passw0rd!x";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProductRepository productRepository;
    @Autowired VehicleModelRepository vehicleModelRepository;
    @Autowired CompatibilityRuleRepository ruleRepository;
    @MockBean LlmClient llm;

    // ---------------------------------------------------------------- helpers

    private record Customer(String accountId, String token) {
    }

    private Customer register() throws Exception {
        String email = "chat-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD)))).andReturn();
        assertEquals(201, r.getResponse().getStatus());
        JsonNode body = read(r);
        return new Customer(body.get("user").get("accountId").asText(), body.get("token").asText());
    }

    private String adminToken() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", "admin-test@example.com", "password", "AdminPass123")))).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        return read(r).get("token").asText();
    }

    private JsonNode read(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    private JsonNode getJson(String url, String token) throws Exception {
        MvcResult r = mvc.perform(auth(get(url), token)).andReturn();
        assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return read(r);
    }

    private String newSession(Customer c) throws Exception {
        MvcResult r = mvc.perform(auth(post("/api/chat-sessions"), c.token())).andReturn();
        assertEquals(201, r.getResponse().getStatus());
        return read(r).get("sessionId").asText();
    }

    private MvcResult sendRaw(Customer c, String sessionId, String content) throws Exception {
        return mvc.perform(auth(post("/api/chat-sessions/" + sessionId + "/messages"), c.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("content", content)))).andReturn();
    }

    private JsonNode send(Customer c, String sessionId, String content) throws Exception {
        MvcResult r = sendRaw(c, sessionId, content);
        assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return read(r);
    }

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Motorbike bike(String brand, String name, long price, ProductStatus status) {
        Motorbike m = new Motorbike();
        m.setName(name);
        m.setPrice(BigDecimal.valueOf(price));
        m.setStatus(status);
        m.setBrand(brand);
        m.setModel("Model-" + brand);
        m.setRegistrationYear(2018);
        return productRepository.save(m);
    }

    private SparePart part(String name, long price, String partType, ProductStatus status) {
        SparePart p = new SparePart();
        p.setName(name);
        p.setPrice(BigDecimal.valueOf(price));
        p.setStatus(status);
        p.setPartType(partType);
        p.setPartOrigin(PartOriginType.GENUINE);
        return productRepository.save(p);
    }

    private VehicleModel vehicle(String brand, String model, int from, int to) {
        VehicleModel v = new VehicleModel();
        v.setBrand(brand);
        v.setModel(model);
        v.setYearFrom(from);
        v.setYearTo(to);
        return vehicleModelRepository.save(v);
    }

    private void compatible(VehicleModel v, SparePart p) {
        CompatibilityRule r = new CompatibilityRule();
        r.setVehicleModel(v);
        r.setSparePart(p);
        ruleRepository.save(r);
    }

    private static SearchCriteria motorbikes(String brand, Long maxPrice) {
        return new SearchCriteria(SearchCriteria.Intent.SEARCH, SearchCriteria.Kind.MOTORBIKE, null, brand, null, null,
                null, maxPrice, null, null, null, null);
    }

    private static SearchCriteria parts(String brand, String model, Integer year, String partType) {
        return new SearchCriteria(SearchCriteria.Intent.SEARCH, SearchCriteria.Kind.SPARE_PART, null, brand, model, year,
                null, null, partType, null, null, null);
    }

    private static SearchCriteria criteriaOf(SearchCriteria.Intent intent) {
        return new SearchCriteria(intent, SearchCriteria.Kind.ANY, null, null, null, null, null, null, null, null, null, null);
    }

    /** LLM giả: trả tiêu chí cho trước và viết lời đáp liệt kê đúng tên các sản phẩm được đưa vào. */
    private void llmReturns(SearchCriteria criteria) {
        when(llm.extractCriteria(anyString(), anyList())).thenReturn(criteria);
        when(llm.writeAnswer(anyString(), anyList())).thenAnswer(inv -> {
            List<ProductFact> facts = inv.getArgument(1);
            return "Gợi ý: " + facts.stream().map(ProductFact::name).collect(Collectors.joining(" | "));
        });
    }

    private static List<String> names(JsonNode products) {
        return StreamSupport.stream(products.spliterator(), false).map(p -> p.get("name").asText()).toList();
    }

    // ------------------------------------------------------------------ phiên và quyền

    @Test
    void requiresLogin() throws Exception {
        assertEquals(401, mvc.perform(post("/api/chat-sessions")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(get("/api/chat-sessions")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(post("/api/chat-sessions/x/messages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"a\"}")).andReturn().getResponse().getStatus());
    }

    @Test
    void createListAndReadOwnSession() throws Exception {
        Customer c = register();
        String id = newSession(c);

        JsonNode session = getJson("/api/chat-sessions/" + id, c.token());
        assertEquals(id, session.get("sessionId").asText());
        assertEquals(0, session.get("messages").size());

        JsonNode list = getJson("/api/chat-sessions", c.token());
        assertEquals(1, list.size());
        assertEquals(id, list.get(0).get("sessionId").asText());
        assertTrue(list.get(0).get("title").isNull(), "chưa hỏi gì thì chưa có tiêu đề");
    }

    @Test
    void otherCustomerCannotSeeOrUseSession() throws Exception {
        Customer a = register();
        Customer b = register();
        String id = newSession(a);

        assertEquals(404, mvc.perform(auth(get("/api/chat-sessions/" + id), b.token())).andReturn().getResponse().getStatus());
        assertEquals(404, sendRaw(b, id, "xin chào").getResponse().getStatus());
        assertEquals(0, getJson("/api/chat-sessions", b.token()).size());
        verify(llm, never()).extractCriteria(anyString(), anyList());
    }

    @Test
    void blankAndTooLongQuestionsAreRejectedWithoutCallingLlm() throws Exception {
        Customer c = register();
        String id = newSession(c);

        assertEquals(400, sendRaw(c, id, "   ").getResponse().getStatus());
        assertEquals(400, sendRaw(c, id, "a".repeat(501)).getResponse().getStatus());
        verify(llm, never()).extractCriteria(anyString(), anyList());
        assertEquals(0, getJson("/api/chat-sessions/" + id, c.token()).get("messages").size());
    }

    // ------------------------------------------------------------------ gợi ý xe máy

    @Test
    void recommendsOnlyAvailableMatchingBikesCheapestFirst() throws Exception {
        String brand = "Brand" + unique();
        bike(brand, "Xe giá 15tr", 15_000_000, ProductStatus.AVAILABLE);
        bike(brand, "Xe giá 10tr", 10_000_000, ProductStatus.AVAILABLE);
        bike(brand, "Xe giá 12tr", 12_000_000, ProductStatus.AVAILABLE);
        bike(brand, "Xe đã bán 5tr", 5_000_000, ProductStatus.SOLD);
        bike(brand, "Xe đắt 30tr", 30_000_000, ProductStatus.AVAILABLE);
        llmReturns(motorbikes(brand, 20_000_000L));

        Customer c = register();
        String sid = newSession(c);
        JsonNode result = send(c, sid, "tìm xe " + brand + " dưới 20 triệu");

        JsonNode reply = result.get("assistantMessage");
        assertEquals("ASSISTANT", reply.get("sender").asText());
        assertEquals(List.of("Xe giá 10tr", "Xe giá 12tr", "Xe giá 15tr"), names(reply.get("products")));
        assertEquals("Gợi ý: Xe giá 10tr | Xe giá 12tr | Xe giá 15tr", reply.get("content").asText());
        assertEquals("ANSWERED", result.get("customerMessage").get("status").asText());
        assertEquals("CUSTOMER", result.get("customerMessage").get("sender").asText());
        assertTrue(reply.get("status").isNull(), "trạng thái chỉ có ở câu hỏi của khách");

        // Hội thoại được lưu, tải lại vẫn thấy đủ và có sản phẩm gợi ý đúng thứ tự
        JsonNode saved = getJson("/api/chat-sessions/" + sid, c.token()).get("messages");
        assertEquals(2, saved.size());
        assertEquals("CUSTOMER", saved.get(0).get("sender").asText());
        assertEquals("ASSISTANT", saved.get(1).get("sender").asText());
        assertEquals(names(reply.get("products")), names(saved.get(1).get("products")));
        assertEquals("tìm xe " + brand + " dưới 20 triệu", getJson("/api/chat-sessions", c.token()).get(0).get("title").asText());
    }

    @Test
    void returnsAtMostFiveProducts() throws Exception {
        String brand = "Brand" + unique();
        for (int i = 1; i <= 7; i++) {
            bike(brand, "Xe số " + i, i * 1_000_000L, ProductStatus.AVAILABLE);
        }
        llmReturns(motorbikes(brand, null));

        Customer c = register();
        JsonNode products = send(c, newSession(c), "xe " + brand).get("assistantMessage").get("products");
        assertEquals(List.of("Xe số 1", "Xe số 2", "Xe số 3", "Xe số 4", "Xe số 5"), names(products));
    }

    @Test
    void likeWildcardsFromLlmAreNotTreatedAsPatterns() throws Exception {
        String brand = "Brand" + unique();
        bike(brand, "Xe thường", 9_000_000, ProductStatus.AVAILABLE);
        SearchCriteria percent = new SearchCriteria(SearchCriteria.Intent.SEARCH, SearchCriteria.Kind.MOTORBIKE, "%", brand,
                null, null, null, null, null, null, null, null);
        llmReturns(percent);

        Customer c = register();
        JsonNode result = send(c, newSession(c), "tìm xe");
        assertEquals(0, result.get("assistantMessage").get("products").size(), "% không được khớp mọi thứ");
        assertEquals("UNANSWERED", result.get("customerMessage").get("status").asText());
    }

    // ------------------------------------------------------------------ gợi ý phụ tùng theo luật tương thích

    @Test
    void recommendsOnlyPartsCompatibleWithTheCustomersVehicle() throws Exception {
        String u = unique();
        String brand = "Hang" + u;
        String model = "Dong" + u;
        VehicleModel vehicle = vehicle(brand, model, 2010, 2015);
        VehicleModel other = vehicle(brand, "Khac" + u, 2010, 2015);
        SparePart fit = part("Nhớt hợp xe " + u, 120_000, "nhot" + u, ProductStatus.AVAILABLE);
        SparePart fit2 = part("Lốp hợp xe " + u, 300_000, "lop" + u, ProductStatus.AVAILABLE);
        SparePart fitSold = part("Nhớt hết hàng " + u, 100_000, "nhot" + u, ProductStatus.SOLD);
        SparePart notFit = part("Nhớt dòng khác " + u, 90_000, "nhot" + u, ProductStatus.AVAILABLE);
        compatible(vehicle, fit);
        compatible(vehicle, fit2);
        compatible(vehicle, fitSold);
        compatible(other, notFit);

        Customer c = register();
        String sid = newSession(c);

        llmReturns(parts(brand, model, 2012, null));
        assertEquals(List.of("Nhớt hợp xe " + u, "Lốp hợp xe " + u),
                names(send(c, sid, "phụ tùng cho xe tôi").get("assistantMessage").get("products")),
                "chỉ phụ tùng có luật tương thích với đúng dòng xe, còn hàng, rẻ trước");

        llmReturns(parts(brand, model, 2012, "lop" + u));
        assertEquals(List.of("Lốp hợp xe " + u),
                names(send(c, sid, "lốp thôi").get("assistantMessage").get("products")), "lọc thêm theo loại phụ tùng");
    }

    @Test
    void vehicleYearOutsideProductionRangeOrUnknownVehicleIsUnanswered() throws Exception {
        String u = unique();
        VehicleModel vehicle = vehicle("Hang" + u, "Dong" + u, 2010, 2015);
        compatible(vehicle, part("Nhớt " + u, 100_000, "nhot", ProductStatus.AVAILABLE));
        Customer c = register();
        String sid = newSession(c);

        llmReturns(parts("Hang" + u, "Dong" + u, 2022, null));   // ngoài khoảng năm sản xuất
        JsonNode outOfRange = send(c, sid, "xe đời 2022");
        assertEquals("UNANSWERED", outOfRange.get("customerMessage").get("status").asText());
        assertEquals(0, outOfRange.get("assistantMessage").get("products").size());
        assertTrue(outOfRange.get("assistantMessage").get("content").asText().contains("chưa có dữ liệu về dòng xe"));

        llmReturns(parts("KhongCo" + u, "XeLa", null, null));    // dòng xe chưa có trong hệ thống
        assertEquals("UNANSWERED", send(c, sid, "xe lạ").get("customerMessage").get("status").asText());
    }

    @Test
    void vehicleWithoutAnyCompatibilityRuleGivesNoProducts() throws Exception {
        String u = unique();
        vehicle("Hang" + u, "Dong" + u, 2010, 2015);   // có dòng xe nhưng chưa có luật nào
        llmReturns(parts("Hang" + u, "Dong" + u, 2012, null));

        Customer c = register();
        JsonNode result = send(c, newSession(c), "phụ tùng cho xe tôi");
        assertEquals("UNANSWERED", result.get("customerMessage").get("status").asText());
        assertEquals(0, result.get("assistantMessage").get("products").size());
    }

    // ------------------------------------------------------------------ chào hỏi, ngoài phạm vi, LLM lỗi

    @Test
    void greetingIsAnsweredWithoutSearching() throws Exception {
        llmReturns(criteriaOf(SearchCriteria.Intent.GREETING));
        Customer c = register();
        JsonNode result = send(c, newSession(c), "xin chào");

        assertEquals("ANSWERED", result.get("customerMessage").get("status").asText());
        assertEquals(0, result.get("assistantMessage").get("products").size());
        verify(llm, never()).writeAnswer(anyString(), anyList());
    }

    @Test
    void outOfScopeQuestionIsUnanswered() throws Exception {
        llmReturns(criteriaOf(SearchCriteria.Intent.OTHER));
        Customer c = register();
        JsonNode result = send(c, newSession(c), "hôm nay thời tiết thế nào");

        assertEquals("UNANSWERED", result.get("customerMessage").get("status").asText());
        assertTrue(result.get("assistantMessage").get("content").asText().contains("chỉ tư vấn xe máy"));
    }

    @Test
    void noMatchingProductIsUnanswered() throws Exception {
        llmReturns(motorbikes("KhongCoHang" + unique(), null));
        Customer c = register();
        JsonNode result = send(c, newSession(c), "xe hãng không tồn tại");

        assertEquals("UNANSWERED", result.get("customerMessage").get("status").asText());
        assertEquals(0, result.get("assistantMessage").get("products").size());
        verify(llm, never()).writeAnswer(anyString(), anyList());
    }

    @Test
    void llmDownOnQuestionStillSavesConversationAsUnanswered() throws Exception {
        when(llm.extractCriteria(anyString(), anyList())).thenThrow(new LlmUnavailableException("hết hạn mức"));
        Customer c = register();
        String sid = newSession(c);
        JsonNode result = send(c, sid, "tìm xe số");

        assertEquals("UNANSWERED", result.get("customerMessage").get("status").asText());
        assertTrue(result.get("assistantMessage").get("content").asText().contains("đang bận"));
        assertEquals(2, getJson("/api/chat-sessions/" + sid, c.token()).get("messages").size());
    }

    @Test
    void unexpectedLlmCrashDoesNotLoseTheQuestion() throws Exception {
        when(llm.extractCriteria(anyString(), anyList())).thenThrow(new IllegalStateException("lỗi bất ngờ"));
        Customer c = register();
        String sid = newSession(c);
        JsonNode result = send(c, sid, "tìm xe số");

        assertEquals("UNANSWERED", result.get("customerMessage").get("status").asText());
        assertEquals(2, getJson("/api/chat-sessions/" + sid, c.token()).get("messages").size());
    }

    @Test
    void answerWritingFailureFallsBackToTemplateButKeepsRealProducts() throws Exception {
        String brand = "Brand" + unique();
        bike(brand, "Xe duy nhất", 11_000_000, ProductStatus.AVAILABLE);
        when(llm.extractCriteria(anyString(), anyList())).thenReturn(motorbikes(brand, null));
        when(llm.writeAnswer(anyString(), anyList())).thenThrow(new LlmUnavailableException("hết hạn mức"));

        Customer c = register();
        JsonNode result = send(c, newSession(c), "xe " + brand);

        assertEquals("ANSWERED", result.get("customerMessage").get("status").asText(), "vẫn có sản phẩm thật nên là đã trả lời");
        String text = result.get("assistantMessage").get("content").asText();
        assertTrue(text.contains("Xe duy nhất") && text.contains("11.000.000"), text);
        assertEquals(List.of("Xe duy nhất"), names(result.get("assistantMessage").get("products")));
    }

    // ------------------------------------------------------------------ ngữ cảnh, giới hạn, quản trị

    @Test
    void previousTurnsArePassedToLlmForFollowUpQuestions() throws Exception {
        llmReturns(criteriaOf(SearchCriteria.Intent.GREETING));
        Customer c = register();
        String sid = newSession(c);
        send(c, sid, "xin chào");
        send(c, sid, "còn gì rẻ hơn không");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LlmClient.ChatTurn>> history = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(llm, org.mockito.Mockito.times(2)).extractCriteria(anyString(), history.capture());
        assertEquals(0, history.getAllValues().get(0).size(), "câu đầu chưa có lịch sử");
        List<LlmClient.ChatTurn> second = history.getAllValues().get(1);
        assertEquals(2, second.size());
        assertTrue(second.get(0).fromCustomer());
        assertEquals("xin chào", second.get(0).text());
        assertFalse(second.get(1).fromCustomer());
    }

    @Test
    void emailAndAccountDetailsNeverReachTheLlm() throws Exception {
        String brand = "Brand" + unique();
        bike(brand, "Xe thử", 8_000_000, ProductStatus.AVAILABLE);
        llmReturns(motorbikes(brand, null));
        Customer c = register();
        send(c, newSession(c), "tìm xe " + brand);

        ArgumentCaptor<List<ProductFact>> facts = ArgumentCaptor.forClass(List.class);
        verify(llm).writeAnswer(anyString(), facts.capture());
        for (ProductFact f : facts.getValue()) {
            String all = f.name() + f.type() + f.description();
            assertFalse(all.contains("@"), "dữ kiện sản phẩm không chứa email: " + all);
        }
    }

    @Test
    void askingTooOftenIsLimitedPerAccount() throws Exception {
        llmReturns(criteriaOf(SearchCriteria.Intent.GREETING));
        Customer c = register();
        String sid = newSession(c);
        for (int i = 0; i < 6; i++) {   // giới hạn cấu hình trong test là 6 câu mỗi giờ
            assertEquals(200, sendRaw(c, sid, "chào " + i).getResponse().getStatus());
        }
        MvcResult blocked = sendRaw(c, sid, "chào nữa");
        assertEquals(429, blocked.getResponse().getStatus());
        assertEquals(12, getJson("/api/chat-sessions/" + sid, c.token()).get("messages").size(),
                "6 câu hỏi + 6 câu trả lời; câu bị chặn không được lưu");

        // tài khoản khác không bị ảnh hưởng
        Customer other = register();
        assertEquals(200, sendRaw(other, newSession(other), "chào").getResponse().getStatus());
    }

    @Test
    void adminSeesUnansweredQuestionsAndConversations() throws Exception {
        Customer c = register();
        String sid = newSession(c);
        String marker = "cau-hoi-" + unique();

        llmReturns(criteriaOf(SearchCriteria.Intent.OTHER));
        send(c, sid, "hỏi lạ " + marker);
        llmReturns(criteriaOf(SearchCriteria.Intent.GREETING));
        send(c, sid, "chào " + marker);

        String admin = adminToken();
        List<String> unanswered = new ArrayList<>();
        JsonNode page = getJson("/api/admin/chat/questions?status=UNANSWERED&size=50", admin);
        for (JsonNode row : page.get("items")) {
            assertEquals("UNANSWERED", row.get("status").asText());
            unanswered.add(row.get("content").asText());
        }
        assertTrue(unanswered.contains("hỏi lạ " + marker));
        assertFalse(unanswered.contains("chào " + marker), "câu đã trả lời không nằm trong danh sách chưa trả lời");

        JsonNode answered = getJson("/api/admin/chat/questions?status=ANSWERED&size=50", admin);
        assertTrue(StreamSupport.stream(answered.get("items").spliterator(), false)
                .anyMatch(r -> r.get("content").asText().equals("chào " + marker)));

        JsonNode all = getJson("/api/admin/chat/questions?size=50", admin);
        assertTrue(all.get("totalItems").asLong() >= 2);

        JsonNode detail = getJson("/api/admin/chat/sessions/" + sid, admin);
        assertTrue(detail.get("customerEmail").asText().startsWith("chat-"));
        assertEquals(4, detail.get("session").get("messages").size());

        assertEquals(400, mvc.perform(auth(get("/api/admin/chat/questions?status=LUNG_TUNG"), admin)).andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(auth(get("/api/admin/chat/sessions/khong-co"), admin)).andReturn().getResponse().getStatus());
    }

    @Test
    void customerCannotUseAdminChatEndpoints() throws Exception {
        Customer c = register();
        assertEquals(403, mvc.perform(auth(get("/api/admin/chat/questions"), c.token())).andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(auth(get("/api/admin/chat/sessions/x"), c.token())).andReturn().getResponse().getStatus());
    }
}
