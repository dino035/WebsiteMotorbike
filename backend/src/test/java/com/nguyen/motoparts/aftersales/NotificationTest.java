package com.nguyen.motoparts.aftersales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.aftersales.entity.Notification;
import com.nguyen.motoparts.aftersales.repository.NotificationRepository;
import com.nguyen.motoparts.aftersales.service.NotificationService;
import com.nguyen.motoparts.common.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Kiểm thử thông báo của khách: danh sách (mới nhất trước, phân trang), đếm chưa đọc, đánh dấu đã đọc,
 * không đụng được thông báo của người khác. Chạy khi có TEST_DB_URL (xem AuthSecurityTest).
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
                "app.cart.hold-minutes=30"
        })
@AutoConfigureMockMvc
class NotificationTest {

    private static final String PASSWORD = "Passw0rd!x";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;

    // ---------------------------------------------------------------- helpers

    private record Customer(String accountId, String token) {
    }

    private Customer register() throws Exception {
        String email = "noti-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD)))).andReturn();
        assertEquals(201, r.getResponse().getStatus());
        JsonNode body = read(r);
        return new Customer(body.get("user").get("accountId").asText(), body.get("token").asText());
    }

    private JsonNode read(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    private JsonNode getJson(String url, String token) throws Exception {
        MvcResult r = mvc.perform(auth(get(url), token)).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        return read(r);
    }

    private MvcResult postRaw(String url, String token) throws Exception {
        return mvc.perform(auth(post(url), token)).andReturn();
    }

    /** Tạo thông báo với thời điểm chỉ định để kiểm tra thứ tự không phụ thuộc đồng hồ. */
    private Notification create(Customer c, String content, LocalDateTime createdAt) {
        Notification n = notificationService.notify(c.accountId(), NotificationType.ORDER_STATUS_CHANGED, content);
        n.setCreatedAt(createdAt);
        return notificationRepository.save(n);
    }

    private long unread(Customer c) throws Exception {
        return getJson("/api/notifications/unread-count", c.token()).get("count").asLong();
    }

    // ------------------------------------------------------------------ tests

    @Test
    void requiresLogin() throws Exception {
        assertEquals(401, mvc.perform(get("/api/notifications")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(get("/api/notifications/unread-count")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(post("/api/notifications/read-all")).andReturn().getResponse().getStatus());
    }

    @Test
    void newCustomerHasNoNotifications() throws Exception {
        Customer c = register();
        JsonNode page = getJson("/api/notifications", c.token());
        assertEquals(0, page.get("items").size());
        assertEquals(0, page.get("totalItems").asInt());
        assertEquals(0, unread(c));
    }

    @Test
    void listsNewestFirstWithFields() throws Exception {
        Customer c = register();
        LocalDateTime now = LocalDateTime.now();
        create(c, "cu nhat", now.minusMinutes(30));
        create(c, "moi nhat", now);
        create(c, "o giua", now.minusMinutes(10));

        JsonNode items = getJson("/api/notifications", c.token()).get("items");
        assertEquals(3, items.size());
        assertEquals("moi nhat", items.get(0).get("content").asText());
        assertEquals("o giua", items.get(1).get("content").asText());
        assertEquals("cu nhat", items.get(2).get("content").asText());
        JsonNode first = items.get(0);
        assertEquals("ORDER_STATUS_CHANGED", first.get("type").asText());
        assertEquals("Cập nhật đơn hàng", first.get("typeLabel").asText());
        assertFalse(first.get("isRead").asBoolean());
        assertTrue(first.hasNonNull("createdAt"));
        assertTrue(first.hasNonNull("notificationId"));
    }

    @Test
    void onlySeesOwnNotificationsAndCount() throws Exception {
        Customer a = register();
        Customer b = register();
        create(a, "cua A 1", LocalDateTime.now());
        create(a, "cua A 2", LocalDateTime.now());
        create(b, "cua B", LocalDateTime.now());

        assertEquals(2, getJson("/api/notifications", a.token()).get("items").size());
        assertEquals(1, getJson("/api/notifications", b.token()).get("items").size());
        assertEquals(2, unread(a));
        assertEquals(1, unread(b));
    }

    @Test
    void paginatesWithStableOrder() throws Exception {
        Customer c = register();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 5; i++) {
            create(c, "tb " + i, now.minusMinutes(i));   // tb 0 mới nhất
        }
        JsonNode p0 = getJson("/api/notifications?page=0&size=2", c.token());
        JsonNode p1 = getJson("/api/notifications?page=1&size=2", c.token());
        JsonNode p2 = getJson("/api/notifications?page=2&size=2", c.token());
        assertEquals(5, p0.get("totalItems").asInt());
        assertEquals(3, p0.get("totalPages").asInt());
        assertEquals("tb 0", p0.get("items").get(0).get("content").asText());
        assertEquals("tb 1", p0.get("items").get(1).get("content").asText());
        assertEquals("tb 2", p1.get("items").get(0).get("content").asText());
        assertEquals("tb 3", p1.get("items").get(1).get("content").asText());
        assertEquals(1, p2.get("items").size());
        assertEquals("tb 4", p2.get("items").get(0).get("content").asText());
        // size quá lớn bị chặn ở 50, page âm coi như 0
        assertEquals(50, getJson("/api/notifications?size=999", c.token()).get("size").asInt());
        assertEquals(0, getJson("/api/notifications?page=-3", c.token()).get("page").asInt());
    }

    @Test
    void markReadLowersUnreadAndIsIdempotent() throws Exception {
        Customer c = register();
        Notification n1 = create(c, "mot", LocalDateTime.now().minusMinutes(1));
        create(c, "hai", LocalDateTime.now());
        assertEquals(2, unread(c));

        MvcResult r = postRaw("/api/notifications/" + n1.getNotificationId() + "/read", c.token());
        assertEquals(200, r.getResponse().getStatus());
        assertTrue(read(r).get("isRead").asBoolean());
        assertEquals(1, unread(c));

        // gọi lại không lỗi, số chưa đọc không đổi
        assertEquals(200, postRaw("/api/notifications/" + n1.getNotificationId() + "/read", c.token()).getResponse().getStatus());
        assertEquals(1, unread(c));
        // thông báo vẫn nằm trong danh sách (chỉ đổi cờ)
        assertEquals(2, getJson("/api/notifications", c.token()).get("items").size());
    }

    @Test
    void cannotMarkSomeoneElsesNotification() throws Exception {
        Customer a = register();
        Customer b = register();
        Notification na = create(a, "cua A", LocalDateTime.now());

        assertEquals(404, postRaw("/api/notifications/" + na.getNotificationId() + "/read", b.token()).getResponse().getStatus());
        assertEquals(1, unread(a), "thông báo của A phải còn chưa đọc");
        assertEquals(404, postRaw("/api/notifications/khong-co-id/read", a.token()).getResponse().getStatus());
    }

    @Test
    void markAllReadOnlyAffectsOwnAccount() throws Exception {
        Customer a = register();
        Customer b = register();
        create(a, "a1", LocalDateTime.now());
        create(a, "a2", LocalDateTime.now());
        create(a, "a3", LocalDateTime.now());
        create(b, "b1", LocalDateTime.now());

        MvcResult r = postRaw("/api/notifications/read-all", a.token());
        assertEquals(200, r.getResponse().getStatus());
        assertEquals(3, read(r).get("updated").asInt());
        assertEquals(0, unread(a));
        assertEquals(1, unread(b), "thông báo của B không bị đụng");

        // gọi lại: không còn gì để đánh dấu
        assertEquals(0, read(postRaw("/api/notifications/read-all", a.token())).get("updated").asInt());
    }
}
