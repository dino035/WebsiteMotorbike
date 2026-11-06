package com.nguyen.motoparts.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.account.entity.AccountLock;
import com.nguyen.motoparts.account.entity.User;
import com.nguyen.motoparts.account.repository.AccountLockRepository;
import com.nguyen.motoparts.account.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử đăng ký / đăng nhập / JWT / phân quyền / khóa tài khoản / CORS qua HTTP giả (MockMvc)
 * trên PostgreSQL thật. Chỉ nạp phần tài khoản + bảo mật, không phụ thuộc entity của phần khác.
 *
 * Chạy: đặt TEST_DB_URL (vd jdbc:postgresql://localhost:5432/motoparts_test), TEST_DB_USERNAME, TEST_DB_PASSWORD.
 * Không đặt TEST_DB_URL thì test bị bỏ qua (để mvn package không cần CSDL).
 */
@EnabledIfEnvironmentVariable(named = "TEST_DB_URL", matches = ".+")
@SpringBootTest(
        classes = AuthSecurityTest.TestApp.class,
        properties = {
                "spring.datasource.url=${TEST_DB_URL}",
                "spring.datasource.username=${TEST_DB_USERNAME:postgres}",
                "spring.datasource.password=${TEST_DB_PASSWORD:}",
                "spring.jpa.hibernate.ddl-auto=update",
                "app.jwt.secret=test-secret-test-secret-test-secret-0123456789",
                "app.cors.allowed-origins=http://localhost:5173",
                "app.admin.email=Admin-Test@Example.com",
                "app.admin.password=AdminPass123"
        })
@AutoConfigureMockMvc
class AuthSecurityTest {

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan({"com.nguyen.motoparts.security", "com.nguyen.motoparts.auth",
            "com.nguyen.motoparts.common.exception"})
    @EntityScan("com.nguyen.motoparts.account.entity")
    @EnableJpaRepositories("com.nguyen.motoparts.account.repository")
    static class TestApp {
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository userRepository;
    @Autowired AccountLockRepository accountLockRepository;

    private static String email() {
        return "u" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
    }

    private static String body(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private MvcResult postJson(String url, String content) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(content)).andReturn();
    }

    private String registerAndToken(String email) throws Exception {
        MvcResult r = postJson("/api/auth/register", body(email, "Passw0rd!x"));
        assertEquals(201, r.getResponse().getStatus());
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    @Test
    void registerCreatesCustomerWithHashedPasswordAndNeverReturnsIt() throws Exception {
        String email = email();
        MvcResult r = postJson("/api/auth/register", body(email.toUpperCase(), "Passw0rd!x"));
        assertEquals(201, r.getResponse().getStatus());
        String text = r.getResponse().getContentAsString();
        JsonNode node = json.readTree(text);
        assertEquals("CUSTOMER", node.at("/user/role").asText());
        assertEquals(email, node.at("/user/email").asText(), "email được chuẩn hóa về chữ thường");
        assertFalse(text.contains("passwordHash") || text.contains("Passw0rd!x"));

        User saved = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertTrue(saved.getPasswordHash().startsWith("$2"), "mật khẩu phải được băm BCrypt");
    }

    @Test
    void registerRejectsDuplicateEmailIgnoringCase() throws Exception {
        String email = email();
        registerAndToken(email);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(email.toUpperCase(), "Another1234")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email này đã được đăng ký."));
    }

    @Test
    void registerValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-an-email", "Passw0rd!x")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body(email(), "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email() + "\",\"password\":\"Passw0rd!x\",\"phone\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").exists());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginSucceedsAndFailuresShareOneMessage() throws Exception {
        String email = email();
        registerAndToken(email);

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(email, "Passw0rd!x")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        String wrongPassword = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, "WrongPass123")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownEmail = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(email(), "WrongPass123")))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertEquals(wrongPassword, unknownEmail, "không để lộ email nào đã đăng ký");
    }

    @Test
    void protectedRouteNeedsValidToken() throws Exception {
        String token = registerAndToken(email());

        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Cần đăng nhập."));
        mvc.perform(bearer(get("/api/auth/me"), "abc.def.ghi")).andExpect(status().isUnauthorized());
        mvc.perform(bearer(get("/api/auth/me"), token.substring(0, token.length() - 2) + "xx"))
                .andExpect(status().isUnauthorized());
        mvc.perform(bearer(get("/api/auth/me"), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void adminRoutesAreForAdminOnly() throws Exception {
        String customer = registerAndToken(email());
        // /api/admin/** chưa có controller: đã qua phân quyền thì sẽ ra 404, bị chặn thì ra 401/403.
        mvc.perform(get("/api/admin/anything")).andExpect(status().isUnauthorized());
        mvc.perform(bearer(get("/api/admin/anything"), customer)).andExpect(status().isForbidden());

        MvcResult login = postJson("/api/auth/login", body("admin-test@example.com", "AdminPass123"));
        assertEquals(200, login.getResponse().getStatus(), "admin mẫu được tạo từ ADMIN_EMAIL/ADMIN_PASSWORD");
        String adminToken = json.readTree(login.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(bearer(get("/api/admin/anything"), adminToken)).andExpect(status().isNotFound());
    }

    @Test
    void publicCatalogReadsAreOpenButOthersAreNot() throws Exception {
        // Chưa có endpoint → 404 nghĩa là đã qua bảo mật; 401 nghĩa là bị chặn.
        mvc.perform(get("/api/products")).andExpect(status().isNotFound());
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void lockedAccountIsBlockedEvenWithValidToken() throws Exception {
        String email = email();
        String token = registerAndToken(email);
        String accountId = userRepository.findByEmailIgnoreCase(email).orElseThrow().getAccountId();

        AccountLock lock = new AccountLock();
        lock.setAccountId(accountId);
        lock.setReason("test");
        lock.setLockedAt(LocalDateTime.now());
        accountLockRepository.save(lock);
        try {
            mvc.perform(bearer(get("/api/auth/me"), token)).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Tài khoản đã bị khóa."));
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(body(email, "Passw0rd!x")))
                    .andExpect(status().isForbidden());
        } finally {
            accountLockRepository.deleteById(accountId);
        }
        mvc.perform(bearer(get("/api/auth/me"), token)).andExpect(status().isOk());
    }

    @Test
    void corsAllowsOnlyConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(options("/api/auth/login")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
