package com.nguyen.motoparts.advisory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.advisory.llm.LlmClient;
import com.nguyen.motoparts.advisory.llm.ProductFact;
import com.nguyen.motoparts.advisory.llm.SearchCriteria;
import com.nguyen.motoparts.advisory.repository.ChatSessionRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Chat box dùng hồ sơ xe của khách làm ngữ cảnh: hỏi phụ tùng mà không nói rõ xe thì gợi ý theo xe trong hồ sơ
 * (qua luật tương thích). LLM được thay bằng bản giả; sản phẩm và luật là dữ liệu thật trong PostgreSQL.
 * Chạy khi có TEST_DB_URL (xem AuthSecurityTest). Cấu hình giống ChatTest để dùng chung ngữ cảnh Spring.
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
class ChatVehicleProfileTest {

    private static final String PASSWORD = "Passw0rd!x";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProductRepository productRepository;
    @Autowired VehicleModelRepository vehicleModelRepository;
    @Autowired CompatibilityRuleRepository ruleRepository;
    @Autowired ChatSessionRepository sessionRepository;
    @MockBean LlmClient llm;

    // ---------------------------------------------------------------- helpers

    private record Customer(String accountId, String token) {
    }

    private Customer register() throws Exception {
        String email = "cvp-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
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

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private VehicleModel vehicle(String brand, String model, int from, int to) {
        VehicleModel v = new VehicleModel();
        v.setBrand(brand);
        v.setModel(model);
        v.setYearFrom(from);
        v.setYearTo(to);
        return vehicleModelRepository.save(v);
    }

    private SparePart part(String name, long price, String partType) {
        SparePart p = new SparePart();
        p.setName(name);
        p.setPrice(BigDecimal.valueOf(price));
        p.setStatus(ProductStatus.AVAILABLE);
        p.setPartType(partType);
        p.setPartOrigin(PartOriginType.GENUINE);
        return productRepository.save(p);
    }

    private Motorbike bike(String brand, String name, long price) {
        Motorbike m = new Motorbike();
        m.setName(name);
        m.setPrice(BigDecimal.valueOf(price));
        m.setStatus(ProductStatus.AVAILABLE);
        m.setBrand(brand);
        m.setModel("Model-" + brand);
        m.setRegistrationYear(2018);
        return productRepository.save(m);
    }

    private void compatible(VehicleModel v, SparePart p) {
        CompatibilityRule r = new CompatibilityRule();
        r.setVehicleModel(v);
        r.setSparePart(p);
        ruleRepository.save(r);
    }

    private String profile(Customer c, VehicleModel v, int year) throws Exception {
        MvcResult r = mvc.perform(auth(post("/api/vehicle-profiles"), c.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nickname", "Xe đi làm", "vehicleModelId", v.getModelId(), "registrationYear", year)))).andReturn();
        assertEquals(201, r.getResponse().getStatus(), r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return read(r).get("profileId").asText();
    }

    private MvcResult createSession(Customer c, String profileId) throws Exception {
        MockHttpServletRequestBuilder b = auth(post("/api/chat-sessions"), c.token());
        if (profileId != null) {
            b = b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("profileId", profileId)));
        }
        return mvc.perform(b).andReturn();
    }

    private String sessionWith(Customer c, String profileId) throws Exception {
        MvcResult r = createSession(c, profileId);
        assertEquals(201, r.getResponse().getStatus(), r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return read(r).get("sessionId").asText();
    }

    private JsonNode send(Customer c, String sessionId, String content) throws Exception {
        MvcResult r = mvc.perform(auth(post("/api/chat-sessions/" + sessionId + "/messages"), c.token())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("content", content)))).andReturn();
        assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return read(r);
    }

    private static List<String> names(JsonNode products) {
        return StreamSupport.stream(products.spliterator(), false).map(p -> p.get("name").asText()).toList();
    }

    private static SearchCriteria partsOfType(String partType) {
        return new SearchCriteria(SearchCriteria.Intent.SEARCH, SearchCriteria.Kind.SPARE_PART, null, null, null, null,
                null, null, partType, null, null, null);
    }

    private static SearchCriteria partsForVehicle(VehicleModel v, int year, String partType) {
        return new SearchCriteria(SearchCriteria.Intent.SEARCH, SearchCriteria.Kind.SPARE_PART, null, v.getBrand(), v.getModel(), year,
                null, null, partType, null, null, null);
    }

    private void llmReturns(SearchCriteria criteria) {
        when(llm.extractCriteria(anyString(), anyList())).thenReturn(criteria);
        when(llm.writeAnswer(anyString(), anyList())).thenAnswer(inv -> {
            List<ProductFact> facts = inv.getArgument(1);
            return "Gợi ý: " + facts.stream().map(ProductFact::name).collect(Collectors.joining(" | "));
        });
    }

    /** Hai dòng xe, ba phụ tùng cùng loại: P1 hợp xe A, P3 hợp xe B, P2 không hợp xe nào. */
    private record Fixture(VehicleModel a, VehicleModel b, String type) {
    }

    private Fixture fixture() {
        String type = "Loai" + unique();
        VehicleModel a = vehicle("HangA" + unique(), "DongA", 2010, 2020);
        VehicleModel b = vehicle("HangB" + unique(), "DongB", 2010, 2020);
        SparePart p1 = part("P1 hợp xe A", 100_000, type);
        part("P2 không hợp xe nào", 200_000, type);
        SparePart p3 = part("P3 hợp xe B", 300_000, type);
        compatible(a, p1);
        compatible(b, p3);
        return new Fixture(a, b, type);
    }

    // ---------------------------------------------------------------- tests

    @Test
    void partsQuestionWithoutVehicleUsesTheProfileVehicle() throws Exception {
        Fixture f = fixture();
        Customer c = register();
        String profileId = profile(c, f.a(), 2015);
        llmReturns(partsOfType(f.type()));

        JsonNode result = send(c, sessionWith(c, profileId), "phụ tùng cho xe của tôi");

        assertEquals(List.of("P1 hợp xe A"), names(result.get("assistantMessage").get("products")));
        assertEquals("ANSWERED", result.get("customerMessage").get("status").asText());
    }

    @Test
    void sameQuestionWithoutProfileIsNotNarrowedToAVehicle() throws Exception {
        Fixture f = fixture();
        Customer c = register();
        llmReturns(partsOfType(f.type()));

        JsonNode result = send(c, sessionWith(c, null), "phụ tùng loại này");

        assertEquals(List.of("P1 hợp xe A", "P2 không hợp xe nào", "P3 hợp xe B"), names(result.get("assistantMessage").get("products")));
    }

    @Test
    void vehicleNamedInTheQuestionWinsOverTheProfile() throws Exception {
        Fixture f = fixture();
        Customer c = register();
        String profileId = profile(c, f.a(), 2015);
        llmReturns(partsForVehicle(f.b(), 2015, f.type()));

        JsonNode result = send(c, sessionWith(c, profileId), "phụ tùng cho xe " + f.b().getBrand());

        assertEquals(List.of("P3 hợp xe B"), names(result.get("assistantMessage").get("products")));
    }

    @Test
    void motorbikeQuestionsAreNotNarrowedByTheProfile() throws Exception {
        Fixture f = fixture();
        Customer c = register();
        String profileId = profile(c, f.a(), 2015);
        // câu hỏi xe máy không nêu hãng, dòng hay năm (như "xe số dưới 20 triệu"): hồ sơ xe không được thu hẹp kết quả
        String marker = "XeThuDoi" + unique();
        bike("BikeBrand" + unique(), marker + " không liên quan hồ sơ", 9_000_000);
        llmReturns(new SearchCriteria(SearchCriteria.Intent.SEARCH, SearchCriteria.Kind.MOTORBIKE, marker, null, null, null,
                null, null, null, null, null, null));

        JsonNode result = send(c, sessionWith(c, profileId), "tìm xe " + marker);

        assertEquals(List.of(marker + " không liên quan hồ sơ"), names(result.get("assistantMessage").get("products")));
    }

    @Test
    void profileVehicleWithoutCompatibleStockIsUnanswered() throws Exception {
        String type = "Loai" + unique();
        VehicleModel lonely = vehicle("HangC" + unique(), "DongC", 2010, 2020);
        part("Phụ tùng không hợp xe nào", 100_000, type);
        Customer c = register();
        String profileId = profile(c, lonely, 2015);
        llmReturns(partsOfType(type));

        JsonNode result = send(c, sessionWith(c, profileId), "phụ tùng cho xe của tôi");

        assertEquals(0, result.get("assistantMessage").get("products").size());
        assertEquals("UNANSWERED", result.get("customerMessage").get("status").asText());
    }

    @Test
    void sessionShowsTheVehicleItIsAdvisingFor() throws Exception {
        Fixture f = fixture();
        Customer c = register();
        String profileId = profile(c, f.a(), 2015);

        MvcResult created = createSession(c, profileId);
        assertEquals(201, created.getResponse().getStatus());
        JsonNode vehicle = read(created).get("vehicle");
        assertEquals(profileId, vehicle.get("profileId").asText());
        assertEquals("Xe đi làm (" + f.a().getBrand() + " DongA 2015)", vehicle.get("label").asText());

        String sid = read(created).get("sessionId").asText();
        assertEquals(profileId, getJson("/api/chat-sessions/" + sid, c.token()).get("vehicle").get("profileId").asText());
        assertEquals(profileId, getJson("/api/admin/chat/sessions/" + sid, adminToken()).get("session").get("vehicle").get("profileId").asText());
    }

    @Test
    void sessionWithoutProfileHasNoVehicle() throws Exception {
        Customer c = register();
        assertTrue(read(createSession(c, null)).get("vehicle").isNull());
        assertTrue(read(createSession(c, "")).get("vehicle").isNull(), "profileId rỗng coi như hỏi chung");
    }

    @Test
    void cannotStartASessionWithSomeoneElsesOrMissingProfile() throws Exception {
        Fixture f = fixture();
        Customer owner = register();
        Customer other = register();
        String profileId = profile(owner, f.a(), 2015);

        assertEquals(404, createSession(other, profileId).getResponse().getStatus());
        assertEquals(404, createSession(other, "khong-co-ho-so-nay").getResponse().getStatus());
        assertEquals(0, getJson("/api/chat-sessions", other.token()).size(), "không tạo cuộc trò chuyện khi hồ sơ sai");
    }

    @Test
    void deletingTheProfileTurnsItsSessionsIntoGeneralChats() throws Exception {
        Fixture f = fixture();
        Customer c = register();
        String profileId = profile(c, f.a(), 2015);
        String sid = sessionWith(c, profileId);
        llmReturns(partsOfType(f.type()));

        assertEquals(204, mvc.perform(auth(delete("/api/vehicle-profiles/" + profileId), c.token())).andReturn().getResponse().getStatus());

        assertTrue(getJson("/api/chat-sessions/" + sid, c.token()).get("vehicle").isNull());
        assertTrue(sessionRepository.findById(sid).orElseThrow().getProfileId() == null,
                "cuộc trò chuyện không còn giữ mã hồ sơ đã xóa");
        JsonNode result = send(c, sid, "phụ tùng loại này");
        assertEquals(3, result.get("assistantMessage").get("products").size(), "hồ sơ đã xóa thì không còn thu hẹp theo xe");
    }
}
