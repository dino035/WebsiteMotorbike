package com.nguyen.motoparts.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.account.entity.PasswordResetToken;
import com.nguyen.motoparts.account.repository.AccountLockRepository;
import com.nguyen.motoparts.account.repository.PasswordResetTokenRepository;
import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.common.exception.EmailSendException;
import com.nguyen.motoparts.mail.EmailService;
import com.nguyen.motoparts.order.entity.Order;
import com.nguyen.motoparts.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử quên mật khẩu, admin khóa/mở tài khoản và sổ địa chỉ trên toàn bộ ứng dụng + PostgreSQL thật.
 * Gửi email được mô phỏng (EmailService) để lấy mã đặt lại mật khẩu từ nội dung email.
 * Chạy khi có TEST_DB_URL (xem AuthSecurityTest); không có thì bỏ qua.
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
                "app.frontend-url=http://localhost:5173",
                "app.reset.max-per-hour=5"
        })
@AutoConfigureMockMvc
class AccountFeaturesTest {

    private static final String PASSWORD = "Passw0rd!x";
    private static final Pattern TOKEN_IN_MAIL = Pattern.compile("reset-password\\?token=([A-Za-z0-9_-]+)");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository userRepository;
    @Autowired AccountLockRepository lockRepository;
    @Autowired PasswordResetTokenRepository tokenRepository;
    @Autowired OrderRepository orderRepository;
    @MockBean EmailService emailService;

    // ---------------------------------------------------------------- helpers

    private static String email(String tag) {
        return tag + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
    }

    private String body(Map<String, ?> map) throws Exception {
        return json.writeValueAsString(map);
    }

    private MvcResult postJson(String url, Map<String, ?> map, String token) throws Exception {
        MockHttpServletRequestBuilder b = post(url).contentType(MediaType.APPLICATION_JSON).content(body(map));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(b).andReturn();
    }

    private String register(String email) throws Exception {
        MvcResult r = postJson("/api/auth/register", Map.of("email", email, "password", PASSWORD), null);
        assertEquals(201, r.getResponse().getStatus());
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private int loginStatus(String email, String password) throws Exception {
        return postJson("/api/auth/login", Map.of("email", email, "password", password), null)
                .getResponse().getStatus();
    }

    private String adminToken() throws Exception {
        MvcResult r = postJson("/api/auth/login", Map.of("email", "admin-test@example.com", "password", "AdminPass123"), null);
        assertEquals(200, r.getResponse().getStatus(), "admin mẫu phải được tạo từ ADMIN_EMAIL/ADMIN_PASSWORD");
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private String accountId(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getAccountId();
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    /** Gọi quên mật khẩu rồi lấy mã trong nội dung email vừa "gửi". */
    private String requestResetAndGetToken(String email) throws Exception {
        reset(emailService);
        mvcForgot(email);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(eq(email), anyString(), bodyCaptor.capture());
        Matcher m = TOKEN_IN_MAIL.matcher(bodyCaptor.getValue());
        assertTrue(m.find(), "email phải chứa liên kết đặt lại mật khẩu");
        return m.group(1);
    }

    private void mvcForgot(String email) throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("email", email)))).andExpect(status().isOk());
    }

    private boolean tokenValid(String token) throws Exception {
        MvcResult r = postJson("/api/auth/reset-password/validate", Map.of("token", token), null);
        assertEquals(200, r.getResponse().getStatus());
        return json.readTree(r.getResponse().getContentAsString()).get("valid").asBoolean();
    }

    private MvcResult doReset(String token, String password, String confirm) throws Exception {
        return postJson("/api/auth/reset-password",
                Map.of("token", token, "password", password, "confirmPassword", confirm), null);
    }

    // ------------------------------------------------------- quên mật khẩu

    @Test
    void forgotPasswordGivesSameAnswerForUnknownEmailAndSendsMailOnlyToKnownOne() throws Exception {
        String known = email("known");
        register(known);
        reset(emailService);

        String a = mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", known)))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String b = mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email("nobody"))))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(a, b, "không để lộ email nào đã đăng ký");
        verify(emailService, times(1)).send(eq(known), anyString(), anyString());
        verify(emailService, times(1)).send(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPasswordValidatesEmailFormat() throws Exception {
        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "khong-phai-email"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fullResetFlowChangesPasswordAndLinkWorksOnlyOnce() throws Exception {
        String email = email("reset");
        register(email);
        String token = requestResetAndGetToken(email);

        assertTrue(tokenValid(token));
        assertFalse(tokenValid("ma-khong-ton-tai"));

        MvcResult ok = doReset(token, "NewPassw0rd!", "NewPassw0rd!");
        assertEquals(200, ok.getResponse().getStatus());

        assertEquals(401, loginStatus(email, PASSWORD), "mật khẩu cũ phải hết hiệu lực");
        assertEquals(200, loginStatus(email, "NewPassw0rd!"));

        assertFalse(tokenValid(token), "mã đã dùng không còn hợp lệ");
        assertEquals(400, doReset(token, "Another1234", "Another1234").getResponse().getStatus());
    }

    @Test
    void resetRejectsMismatchShortPasswordAndBadToken() throws Exception {
        String email = email("rules");
        register(email);
        String token = requestResetAndGetToken(email);

        assertEquals(400, doReset(token, "NewPassw0rd!", "Khac-nhau-123").getResponse().getStatus());
        assertEquals(400, doReset(token, "short", "short").getResponse().getStatus());
        assertEquals(400, doReset("ma-sai", "NewPassw0rd!", "NewPassw0rd!").getResponse().getStatus());
        assertTrue(tokenValid(token), "nhập sai biểu mẫu không làm mất mã");
        assertEquals(200, loginStatus(email, PASSWORD), "mật khẩu chưa bị đổi");
    }

    @Test
    void newRequestInvalidatesOlderLink() throws Exception {
        String email = email("twice");
        register(email);
        String first = requestResetAndGetToken(email);
        String second = requestResetAndGetToken(email);

        assertFalse(tokenValid(first), "mã cũ bị thay bởi mã mới");
        assertTrue(tokenValid(second));
    }

    @Test
    void expiredLinkIsRejected() throws Exception {
        String email = email("expired");
        register(email);
        String token = requestResetAndGetToken(email);
        String accountId = accountId(email);

        for (PasswordResetToken t : tokenRepository.findAll()) {
            if (accountId.equals(t.getAccountId())) {
                t.setExpiresAt(LocalDateTime.now().minusMinutes(1));
                tokenRepository.save(t);
            }
        }
        assertFalse(tokenValid(token));
        assertEquals(400, doReset(token, "NewPassw0rd!", "NewPassw0rd!").getResponse().getStatus());
    }

    @Test
    void tooManyRequestsPerHourAreSilentlyIgnored() throws Exception {
        String email = email("limit");
        register(email);
        reset(emailService);
        for (int i = 0; i < 7; i++) {
            mvcForgot(email);
        }
        verify(emailService, times(5)).send(eq(email), anyString(), anyString());
    }

    @Test
    void mailFailureReturns503AndLeavesNoToken() throws Exception {
        String email = email("mailfail");
        register(email);
        reset(emailService);
        doThrow(new EmailSendException("loi gia lap", null)).when(emailService).send(eq(email), anyString(), anyString());

        mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email))))
                .andExpect(status().isServiceUnavailable());

        String accountId = accountId(email);
        assertEquals(0, tokenRepository.findAll().stream().filter(t -> accountId.equals(t.getAccountId())).count(),
                "gửi email lỗi thì mã vừa tạo không được lưu");
    }

    // ----------------------------------------------------- admin khóa tài khoản

    @Test
    void adminEndpointsAreForAdminsOnly() throws Exception {
        String customer = register(email("cust"));
        mvc.perform(get("/api/admin/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(auth(get("/api/admin/accounts"), customer)).andExpect(status().isForbidden());
        mvc.perform(auth(post("/api/admin/accounts/x/lock"), customer)).andExpect(status().isForbidden());
    }

    @Test
    void adminLocksAndUnlocksAndLockedUserIsBlockedImmediately() throws Exception {
        String email = email("lockme");
        String userToken = register(email);
        String id = accountId(email);
        String admin = adminToken();

        mvc.perform(auth(get("/api/auth/me"), userToken)).andExpect(status().isOk());

        mvc.perform(auth(post("/api/admin/accounts/" + id + "/lock").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("reason", "Vi pham"))), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.lockReason").value("Vi pham"));

        mvc.perform(auth(get("/api/auth/me"), userToken)).andExpect(status().isForbidden());
        assertEquals(403, loginStatus(email, PASSWORD));

        mvc.perform(auth(post("/api/admin/accounts/" + id + "/lock"), admin)).andExpect(status().isConflict());

        mvc.perform(auth(post("/api/admin/accounts/" + id + "/unlock"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.locked").value(false));
        mvc.perform(auth(get("/api/auth/me"), userToken)).andExpect(status().isOk());
        assertEquals(200, loginStatus(email, PASSWORD));

        mvc.perform(auth(post("/api/admin/accounts/" + id + "/unlock"), admin)).andExpect(status().isConflict());
    }

    @Test
    void adminCannotLockAdminOrUnknownAccountAndReasonIsLimited() throws Exception {
        String admin = adminToken();
        String adminId = accountId("admin-test@example.com");
        mvc.perform(auth(post("/api/admin/accounts/" + adminId + "/lock"), admin)).andExpect(status().isBadRequest());
        mvc.perform(auth(post("/api/admin/accounts/khong-co/lock"), admin)).andExpect(status().isNotFound());

        String email = email("longreason");
        register(email);
        mvc.perform(auth(post("/api/admin/accounts/" + accountId(email) + "/lock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("reason", "x".repeat(201)))), admin))
                .andExpect(status().isBadRequest());
        assertFalse(lockRepository.existsById(accountId(email)));
    }

    @Test
    void lockingAccountWithUnfinishedOrdersNeedsConfirmation() throws Exception {
        String email = email("orders");
        register(email);
        String id = accountId(email);
        String admin = adminToken();

        Order pending = new Order();
        pending.setAccountId(id);
        pending.setStatus(OrderStatus.PENDING);
        orderRepository.save(pending);
        Order done = new Order();
        done.setAccountId(id);
        done.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(done);

        mvc.perform(auth(get("/api/admin/accounts").param("q", email), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unfinishedOrders").value(1));

        mvc.perform(auth(post("/api/admin/accounts/" + id + "/lock"), admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("1 đơn hàng chưa hoàn tất")));
        assertFalse(lockRepository.existsById(id));

        mvc.perform(auth(post("/api/admin/accounts/" + id + "/lock").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("force", true))), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.locked").value(true));
    }

    @Test
    void adminListSearchesFiltersAndPaginatesCustomersOnly() throws Exception {
        String tag = "lst" + UUID.randomUUID().toString().substring(0, 6);
        String e1 = tag + "-a@test.local", e2 = tag + "-b@test.local", e3 = tag + "-c@test.local";
        register(e1);
        register(e2);
        register(e3);
        String admin = adminToken();
        mvc.perform(auth(post("/api/admin/accounts/" + accountId(e2) + "/lock"), admin)).andExpect(status().isOk());

        mvc.perform(auth(get("/api/admin/accounts").param("q", tag), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.items[0].email").value(e1));
        mvc.perform(auth(get("/api/admin/accounts").param("q", tag).param("status", "LOCKED"), admin))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].email").value(e2));
        mvc.perform(auth(get("/api/admin/accounts").param("q", tag).param("status", "ACTIVE"), admin))
                .andExpect(jsonPath("$.totalItems").value(2));
        mvc.perform(auth(get("/api/admin/accounts").param("q", tag).param("size", "2").param("page", "1"), admin))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
        // ký tự đặc biệt của LIKE không được coi là ký tự thay thế
        mvc.perform(auth(get("/api/admin/accounts").param("q", "%"), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));
        // admin không nằm trong danh sách khách hàng
        mvc.perform(auth(get("/api/admin/accounts").param("q", "admin-test"), admin))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    // ----------------------------------------------------------- sổ địa chỉ

    private Map<String, Object> addr(String name, boolean makeDefault) {
        return Map.of("receiverName", name, "receiverPhone", "090 123.4567", "houseNumber", "12",
                "street", "Le Loi", "district", "Quan 1", "city", "TP HCM", "makeDefault", makeDefault);
    }

    private JsonNode createAddress(String token, Map<String, Object> req) throws Exception {
        MvcResult r = postJson("/api/addresses", req, token);
        assertEquals(201, r.getResponse().getStatus(), r.getResponse().getContentAsString());
        return json.readTree(r.getResponse().getContentAsString());
    }

    private JsonNode listAddresses(String token) throws Exception {
        return json.readTree(mvc.perform(auth(get("/api/addresses"), token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    void addressApiNeedsLogin() throws Exception {
        mvc.perform(get("/api/addresses")).andExpect(status().isUnauthorized());
    }

    @Test
    void firstAddressIsDefaultAndPhoneIsNormalized() throws Exception {
        String token = register(email("addr1"));
        JsonNode first = createAddress(token, addr("An", false));
        assertTrue(first.get("defaultAddress").asBoolean(), "địa chỉ đầu tiên tự là mặc định");
        assertEquals("0901234567", first.get("receiverPhone").asText());
        assertEquals("12, Le Loi, Quan 1, TP HCM", first.get("fullAddress").asText());

        JsonNode second = createAddress(token, addr("Binh", false));
        assertFalse(second.get("defaultAddress").asBoolean());
        JsonNode list = listAddresses(token);
        assertEquals(2, list.size());
        assertEquals("An", list.get(0).get("receiverName").asText(), "mặc định lên đầu");
    }

    @Test
    void makeDefaultMovesTheDefaultAndKeepsOnlyOne() throws Exception {
        String token = register(email("addr2"));
        createAddress(token, addr("An", false));
        JsonNode b = createAddress(token, addr("Binh", true));
        assertTrue(b.get("defaultAddress").asBoolean());

        JsonNode list = listAddresses(token);
        long defaults = 0;
        for (JsonNode a : list) {
            if (a.get("defaultAddress").asBoolean()) {
                defaults++;
            }
        }
        assertEquals(1, defaults);
        assertEquals("Binh", list.get(0).get("receiverName").asText());

        String firstId = list.get(1).get("addressId").asText();
        mvc.perform(auth(post("/api/addresses/" + firstId + "/default"), token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.defaultAddress").value(true));
        assertEquals("An", listAddresses(token).get(0).get("receiverName").asText());
    }

    @Test
    void updateChangesFieldsAndCanMakeDefault() throws Exception {
        String token = register(email("addr3"));
        createAddress(token, addr("An", false));
        String second = createAddress(token, addr("Binh", false)).get("addressId").asText();

        MvcResult r = mvc.perform(auth(put("/api/addresses/" + second).contentType(MediaType.APPLICATION_JSON)
                        .content(body(addr("Binh Moi", true))), token))
                .andExpect(status().isOk()).andReturn();
        JsonNode updated = json.readTree(r.getResponse().getContentAsString());
        assertEquals("Binh Moi", updated.get("receiverName").asText());
        assertTrue(updated.get("defaultAddress").asBoolean());
        assertEquals("Binh Moi", listAddresses(token).get(0).get("receiverName").asText());
    }

    @Test
    void deletingTheDefaultPromotesTheOldestRemaining() throws Exception {
        String token = register(email("addr4"));
        String a = createAddress(token, addr("An", false)).get("addressId").asText();
        createAddress(token, addr("Binh", false));
        createAddress(token, addr("Chi", false));

        mvc.perform(auth(delete("/api/addresses/" + a), token)).andExpect(status().isNoContent());
        JsonNode list = listAddresses(token);
        assertEquals(2, list.size());
        assertEquals("Binh", list.get(0).get("receiverName").asText(), "địa chỉ cũ nhất còn lại thành mặc định");
        assertTrue(list.get(0).get("defaultAddress").asBoolean());
    }

    @Test
    void addressRulesAreEnforced() throws Exception {
        String token = register(email("addr5"));
        assertEquals(400, postJson("/api/addresses", Map.of("receiverName", "", "receiverPhone", "0901234567",
                "houseNumber", "1", "street", "s", "district", "d", "city", "c"), token).getResponse().getStatus());
        Map<String, Object> badPhone = new java.util.HashMap<>(addr("An", false));
        badPhone.put("receiverPhone", "12345");
        assertEquals(400, postJson("/api/addresses", badPhone, token).getResponse().getStatus());

        for (int i = 0; i < 10; i++) {
            createAddress(token, addr("Nguoi " + i, false));
        }
        MvcResult over = postJson("/api/addresses", addr("Thu 11", false), token);
        assertEquals(400, over.getResponse().getStatus());
        assertTrue(over.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("tối đa 10 địa chỉ"));
    }

    @Test
    void usersCannotSeeOrChangeEachOthersAddresses() throws Exception {
        String owner = register(email("owner"));
        String other = register(email("other"));
        String id = createAddress(owner, addr("Chu", false)).get("addressId").asText();

        assertEquals(0, listAddresses(other).size());
        mvc.perform(auth(put("/api/addresses/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(body(addr("Hack", false))), other)).andExpect(status().isNotFound());
        mvc.perform(auth(post("/api/addresses/" + id + "/default"), other)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/addresses/" + id), other)).andExpect(status().isNotFound());
        assertEquals("Chu", listAddresses(owner).get(0).get("receiverName").asText());
    }
}
