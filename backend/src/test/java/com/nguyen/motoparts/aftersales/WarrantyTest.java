package com.nguyen.motoparts.aftersales;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.aftersales.repository.NotificationRepository;
import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.common.enums.OrderStatus;
import com.nguyen.motoparts.order.entity.Order;
import com.nguyen.motoparts.order.repository.OrderItemRepository;
import com.nguyen.motoparts.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Kiểm thử bảo hành (khách gửi yêu cầu, Admin duyệt hoặc từ chối) trên toàn bộ ứng dụng + PostgreSQL thật.
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
                "app.cart.hold-minutes=30"
        })
@AutoConfigureMockMvc
class WarrantyTest {

    private static final String PASSWORD = "Passw0rd!x";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired NotificationRepository notificationRepository;

    // ---------------------------------------------------------------- helpers

    private record Customer(String accountId, String email, String token) {
    }

    private Customer register() throws Exception {
        String email = "war-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD)))).andReturn();
        assertEquals(201, r.getResponse().getStatus());
        JsonNode body = read(r);
        return new Customer(body.get("user").get("accountId").asText(), email, body.get("token").asText());
    }

    private String[] adminLogin() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", "admin-test@example.com", "password", "AdminPass123")))).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        JsonNode body = read(r);
        return new String[]{body.get("token").asText(), body.get("user").get("accountId").asText()};
    }

    private String adminToken() throws Exception {
        return adminLogin()[0];
    }

    private Motorbike newBike() {
        Motorbike m = new Motorbike();
        m.setName("Honda War " + UUID.randomUUID().toString().substring(0, 6));
        m.setPrice(new BigDecimal("7000000"));
        m.setStatus(ProductStatus.AVAILABLE);
        m.setBrand("Honda");
        m.setModel("Test");
        return productRepository.save(m);
    }

    private SparePart newPart() {
        SparePart p = new SparePart();
        p.setName("Phu tung war " + UUID.randomUUID().toString().substring(0, 6));
        p.setPrice(new BigDecimal("90000"));
        p.setStatus(ProductStatus.AVAILABLE);
        p.setPartType("Nhot");
        return productRepository.save(p);
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    private JsonNode read(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String message(MvcResult r) throws Exception {
        return read(r).get("message").asText();
    }

    private MvcResult postJson(String url, String token, Object body) throws Exception {
        MockHttpServletRequestBuilder b = post(url).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            b.content(json.writeValueAsString(body));
        }
        return mvc.perform(auth(b, token)).andReturn();
    }

    private JsonNode getJson(String url, String token) throws Exception {
        MvcResult r = mvc.perform(auth(get(url), token)).andReturn();
        assertEquals(200, r.getResponse().getStatus(), url);
        return read(r);
    }

    /** Đặt một đơn (COD, giao tận nơi) chỉ có một sản phẩm; trả mã đơn. */
    private String placeOrder(Customer c, String productId) throws Exception {
        assertEquals(200, postJson("/api/cart/items", c.token(), Map.of("productId", productId, "quantity", 1))
                .getResponse().getStatus());
        Map<String, Object> body = new HashMap<>();
        body.put("deliveryMethod", "DELIVERY");
        body.put("receiverName", "Nguyen Van A");
        body.put("receiverPhone", "0912345678");
        body.put("shippingAddress", "12 Le Loi");
        body.put("paymentMethod", "COD");
        body.put("returnPolicyAccepted", true);
        MvcResult r = postJson("/api/orders", c.token(), body);
        assertEquals(201, r.getResponse().getStatus());
        return read(r).get("orderId").asText();
    }

    /** Đưa đơn tới Hoàn thành qua API Admin: thu tiền, xác nhận, giao, hoàn thành. */
    private void complete(String orderId) throws Exception {
        String admin = adminToken();
        assertEquals(200, postJson("/api/admin/orders/" + orderId + "/confirm-payment", admin, null).getResponse().getStatus());
        for (String status : new String[]{"CONFIRMED", "SHIPPING", "COMPLETED"}) {
            assertEquals(200, postJson("/api/admin/orders/" + orderId + "/status", admin, Map.of("status", status))
                    .getResponse().getStatus());
        }
    }

    private void backdate(String orderId, int months) {
        Order o = orderRepository.findById(orderId).orElseThrow();
        o.setOrderDate(LocalDateTime.now().minusMonths(months));
        orderRepository.save(o);
    }

    /** Mã dòng đơn (orderItemId) của sản phẩm trong đơn Hoàn thành orderId, lấy từ danh sách bảo hành của khách. */
    private String firstOptionItem(Customer c, String orderId) throws Exception {
        String code = orderId.substring(0, 8).toUpperCase();
        for (JsonNode o : getJson("/api/warranty-requests/options", c.token())) {
            if (code.equals(o.get("orderCode").asText())) {
                return o.get("orderItemId").asText();
            }
        }
        throw new AssertionError("không thấy sản phẩm của đơn " + code + " trong danh sách bảo hành");
    }

    private Map<String, Object> request(String orderItemId, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderItemId", orderItemId);
        body.put("description", description);
        return body;
    }

    private MvcResult submit(Customer c, String orderItemId) throws Exception {
        return postJson("/api/warranty-requests", c.token(), request(orderItemId, "Dau nhot bi ro ri sau 2 tuan"));
    }

    // ------------------------------------------------------------------ tests

    @Test
    void warrantyNeedsLoginAndAdminEndpointsNeedAdmin() throws Exception {
        Customer c = register();
        assertEquals(401, mvc.perform(get("/api/warranty-requests")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(get("/api/warranty-requests/options")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(get("/api/admin/warranty-requests")).andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(auth(get("/api/admin/warranty-requests"), c.token())).andReturn().getResponse().getStatus());
        assertEquals(403, postJson("/api/admin/warranty-requests/x/decide", c.token(), Map.of("approve", true))
                .getResponse().getStatus());
    }

    @Test
    void optionsListOnlyItemsFromCompletedOrdersAndShowTheDeadline() throws Exception {
        Customer c = register();
        placeOrder(c, newPart().getProductId());               // đơn chưa hoàn thành: không có trong danh sách
        String done = placeOrder(c, newPart().getProductId());
        complete(done);

        JsonNode options = getJson("/api/warranty-requests/options", c.token());
        assertEquals(1, options.size());
        JsonNode o = options.get(0);
        assertEquals(done.substring(0, 8).toUpperCase(), o.get("orderCode").asText());
        assertTrue(o.get("canRequest").asBoolean());
        assertFalse(o.get("expired").asBoolean());
        assertFalse(o.get("pending").asBoolean());
        // phụ tùng: hạn 6 tháng kể từ ngày mua
        LocalDateTime purchased = LocalDateTime.parse(o.get("purchasedAt").asText());
        LocalDateTime deadline = LocalDateTime.parse(o.get("deadline").asText());
        assertEquals(purchased.plusMonths(6), deadline);
    }

    @Test
    void customerSubmitsARequestAndAdminsAreNotified() throws Exception {
        Customer c = register();
        String orderId = placeOrder(c, newPart().getProductId());
        complete(orderId);
        String itemId = firstOptionItem(c, orderId);
        String[] admin = adminLogin();
        long before = notificationRepository.findAll().stream().filter(n -> admin[1].equals(n.getAccountId())).count();

        MvcResult r = submit(c, itemId);
        assertEquals(201, r.getResponse().getStatus());
        JsonNode w = read(r);
        assertEquals("PENDING", w.get("status").asText());
        assertEquals("Chờ xử lý", w.get("statusLabel").asText());
        assertEquals(itemId, w.get("orderItemId").asText());
        assertEquals(orderId.substring(0, 8).toUpperCase(), w.get("orderCode").asText());
        assertTrue(w.get("attachmentUrl").isNull());

        assertEquals(before + 1, notificationRepository.findAll().stream().filter(n -> admin[1].equals(n.getAccountId())).count(),
                "Admin nhận một thông báo yêu cầu mới");
        // danh sách và chi tiết của chính khách
        JsonNode mine = getJson("/api/warranty-requests", c.token());
        assertEquals(1, mine.size());
        assertEquals(w.get("requestId").asText(), mine.get(0).get("requestId").asText());
        assertEquals("Dau nhot bi ro ri sau 2 tuan", getJson("/api/warranty-requests/" + w.get("requestId").asText(), c.token())
                .get("description").asText());
        // sản phẩm đó đang chờ xử lý nên không gửi thêm được
        JsonNode option = getJson("/api/warranty-requests/options", c.token()).get(0);
        assertTrue(option.get("pending").asBoolean());
        assertFalse(option.get("canRequest").asBoolean());
    }

    @Test
    void dataIsValidatedAndOnlyHttpLinksAreKept() throws Exception {
        Customer c = register();
        String orderId = placeOrder(c, newPart().getProductId());
        complete(orderId);
        String itemId = firstOptionItem(c, orderId);

        assertEquals(400, postJson("/api/warranty-requests", c.token(), request(itemId, "   ")).getResponse().getStatus());
        assertEquals(400, postJson("/api/warranty-requests", c.token(), request(itemId, "x".repeat(1001))).getResponse().getStatus());
        assertEquals(400, postJson("/api/warranty-requests", c.token(), request(null, "Loi")).getResponse().getStatus());

        Map<String, Object> evil = request(itemId, "Loi");
        evil.put("attachmentUrl", "javascript:alert(1)");
        MvcResult r = postJson("/api/warranty-requests", c.token(), evil);
        assertEquals(400, r.getResponse().getStatus());
        assertTrue(message(r).contains("http"));
        evil.put("attachmentUrl", "ftp://example.com/anh.jpg");
        assertEquals(400, postJson("/api/warranty-requests", c.token(), evil).getResponse().getStatus());
        evil.put("attachmentUrl", "https://example.com/" + "a".repeat(500));
        assertEquals(400, postJson("/api/warranty-requests", c.token(), evil).getResponse().getStatus());

        Map<String, Object> ok = request(itemId, "  Loi  ");
        ok.put("attachmentUrl", "  https://example.com/anh.jpg  ");
        r = postJson("/api/warranty-requests", c.token(), ok);
        assertEquals(201, r.getResponse().getStatus());
        assertEquals("https://example.com/anh.jpg", read(r).get("attachmentUrl").asText());
        assertEquals("Loi", read(r).get("description").asText(), "mô tả được cắt khoảng trắng");

        // đường dẫn trống thì coi như không có
        String orderId2 = placeOrder(c, newPart().getProductId());
        complete(orderId2);
        Map<String, Object> blank = request(firstOptionItem(c, orderId2), "Loi 2");
        blank.put("attachmentUrl", "   ");
        r = postJson("/api/warranty-requests", c.token(), blank);
        assertEquals(201, r.getResponse().getStatus());
        assertNull(read(r).get("attachmentUrl").textValue());
    }

    @Test
    void onlyEligibleItemsCanBeSubmitted() throws Exception {
        Customer owner = register();
        Customer other = register();
        String done = placeOrder(owner, newPart().getProductId());
        complete(done);
        String itemId = firstOptionItem(owner, done);

        // sản phẩm của người khác và mã không tồn tại: 404
        assertEquals(404, submit(other, itemId).getResponse().getStatus());
        assertEquals(404, submit(owner, "khong-co").getResponse().getStatus());

        // đơn chưa hoàn thành: 409
        String pendingOrder = placeOrder(owner, newPart().getProductId());
        String pendingItem = orderItemRepository.findByAccountAndOrderStatus(owner.accountId(), OrderStatus.PENDING)
                .stream().filter(i -> i.getOrder().getOrderId().equals(pendingOrder)).findFirst().orElseThrow().getOrderItemId();
        MvcResult r = submit(owner, pendingItem);
        assertEquals(409, r.getResponse().getStatus());
        assertTrue(message(r).contains("hoàn thành"));
    }

    @Test
    void anotherCustomerCannotSeeMyRequest() throws Exception {
        Customer a = register();
        Customer b = register();
        String orderId = placeOrder(a, newPart().getProductId());
        complete(orderId);
        String requestId = read(submit(a, firstOptionItem(a, orderId))).get("requestId").asText();

        assertEquals(404, mvc.perform(auth(get("/api/warranty-requests/" + requestId), b.token())).andReturn().getResponse().getStatus());
        assertEquals(0, getJson("/api/warranty-requests", b.token()).size());
        assertEquals(0, getJson("/api/warranty-requests/options", b.token()).size());
    }

    @Test
    void warrantyPeriodIsSixMonthsForPartsAndTwoYearsForMotorbikes() throws Exception {
        Customer c = register();
        // phụ tùng mua 7 tháng trước: hết hạn
        String partOrder = placeOrder(c, newPart().getProductId());
        complete(partOrder);
        String partItem = firstOptionItem(c, partOrder);
        backdate(partOrder, 7);
        // xe mua 18 tháng trước: vẫn còn hạn (24 tháng)
        String bikeOrder = placeOrder(c, newBike().getProductId());
        complete(bikeOrder);
        String bikeItem = firstOptionItem(c, bikeOrder);
        backdate(bikeOrder, 18);

        for (JsonNode o : getJson("/api/warranty-requests/options", c.token())) {
            if (partItem.equals(o.get("orderItemId").asText())) {
                assertTrue(o.get("expired").asBoolean());
                assertFalse(o.get("canRequest").asBoolean());
            }
            if (bikeItem.equals(o.get("orderItemId").asText())) {
                assertFalse(o.get("expired").asBoolean());
                assertTrue(o.get("canRequest").asBoolean());
            }
        }
        MvcResult r = submit(c, partItem);
        assertEquals(409, r.getResponse().getStatus());
        assertTrue(message(r).contains("hết thời hạn"));
        assertEquals(201, submit(c, bikeItem).getResponse().getStatus());

        // xe mua 25 tháng trước thì hết hạn
        String oldBike = placeOrder(c, newBike().getProductId());
        complete(oldBike);
        String oldItem = firstOptionItem(c, oldBike);
        backdate(oldBike, 25);
        assertEquals(409, submit(c, oldItem).getResponse().getStatus());
    }

    @Test
    void adminListsFiltersAndOpensRequests() throws Exception {
        Customer c = register();
        String orderId = placeOrder(c, newPart().getProductId());
        complete(orderId);
        String requestId = read(submit(c, firstOptionItem(c, orderId))).get("requestId").asText();
        String admin = adminToken();

        JsonNode page = getJson("/api/admin/warranty-requests?size=50", admin);
        assertTrue(page.get("totalItems").asLong() >= 1);
        assertEquals(requestId, page.get("items").get(0).get("requestId").asText(), "mới nhất đứng đầu");
        assertEquals(c.email(), page.get("items").get(0).get("customerEmail").asText());
        assertEquals("PENDING", page.get("items").get(0).get("status").asText());

        for (JsonNode row : getJson("/api/admin/warranty-requests?status=REJECTED&size=50", admin).get("items")) {
            assertEquals("REJECTED", row.get("status").asText());
        }
        assertEquals(400, mvc.perform(auth(get("/api/admin/warranty-requests?status=XYZ"), admin)).andReturn().getResponse().getStatus());
        assertEquals(200, mvc.perform(auth(get("/api/admin/warranty-requests?status=ALL"), admin)).andReturn().getResponse().getStatus());

        JsonNode d = getJson("/api/admin/warranty-requests/" + requestId, admin);
        assertEquals(c.email(), d.get("customer").get("email").asText());
        assertTrue(d.get("canDecide").asBoolean());
        assertEquals(LocalDateTime.parse(d.get("purchasedAt").asText()).plusMonths(6), LocalDateTime.parse(d.get("deadline").asText()));
        assertEquals(404, mvc.perform(auth(get("/api/admin/warranty-requests/khong-co"), admin)).andReturn().getResponse().getStatus());
    }

    @Test
    void adminApprovesOrRejectsAndTheCustomerIsNotified() throws Exception {
        Customer c = register();
        String admin = adminToken();

        // duyệt (ghi chú không bắt buộc)
        String o1 = placeOrder(c, newPart().getProductId());
        complete(o1);
        String r1 = read(submit(c, firstOptionItem(c, o1))).get("requestId").asText();
        MvcResult r = postJson("/api/admin/warranty-requests/" + r1 + "/decide", admin, Map.of("approve", true));
        assertEquals(200, r.getResponse().getStatus());
        assertEquals("APPROVED", read(r).get("request").get("status").asText());
        assertFalse(read(r).get("canDecide").asBoolean());
        assertTrue(read(r).get("request").get("resolvedAt").asText().length() > 0);

        // từ chối cần lý do
        String o2 = placeOrder(c, newPart().getProductId());
        complete(o2);
        String r2 = read(submit(c, firstOptionItem(c, o2))).get("requestId").asText();
        Map<String, Object> noReason = new HashMap<>();
        noReason.put("approve", false);
        noReason.put("note", "   ");
        r = postJson("/api/admin/warranty-requests/" + r2 + "/decide", admin, noReason);
        assertEquals(400, r.getResponse().getStatus());
        assertTrue(message(r).contains("lý do"));
        assertEquals(400, postJson("/api/admin/warranty-requests/" + r2 + "/decide", admin, Map.of()).getResponse().getStatus());
        assertEquals(400, postJson("/api/admin/warranty-requests/" + r2 + "/decide", admin,
                Map.of("approve", false, "note", "x".repeat(501))).getResponse().getStatus());
        r = postJson("/api/admin/warranty-requests/" + r2 + "/decide", admin, Map.of("approve", false, "note", "Hu hong do nguoi dung"));
        assertEquals(200, r.getResponse().getStatus());
        assertEquals("REJECTED", read(r).get("request").get("status").asText());
        assertEquals("Hu hong do nguoi dung", read(r).get("request").get("decisionReason").asText());

        // đã xử lý rồi thì không đổi nữa
        assertEquals(409, postJson("/api/admin/warranty-requests/" + r1 + "/decide", admin, Map.of("approve", false, "note", "Doi y"))
                .getResponse().getStatus());
        assertEquals(404, postJson("/api/admin/warranty-requests/khong-co/decide", admin, Map.of("approve", true)).getResponse().getStatus());

        // khách thấy kết quả và có thông báo
        JsonNode mine = getJson("/api/warranty-requests", c.token());
        assertEquals(2, mine.size());
        List<String> contents = notificationRepository.findAll().stream()
                .filter(n -> c.accountId().equals(n.getAccountId())).map(n -> n.getContent()).toList();
        assertTrue(contents.stream().anyMatch(t -> t.contains("đã được duyệt")), contents.toString());
        assertTrue(contents.stream().anyMatch(t -> t.contains("bị từ chối") && t.contains("Hu hong do nguoi dung")), contents.toString());
    }

    @Test
    void anItemCanBeSubmittedAgainOnceTheEarlierRequestIsDecided() throws Exception {
        Customer c = register();
        String orderId = placeOrder(c, newPart().getProductId());
        complete(orderId);
        String itemId = firstOptionItem(c, orderId);
        String requestId = read(submit(c, itemId)).get("requestId").asText();

        MvcResult dup = submit(c, itemId);
        assertEquals(409, dup.getResponse().getStatus());
        assertTrue(message(dup).contains("đang chờ xử lý"));

        assertEquals(200, postJson("/api/admin/warranty-requests/" + requestId + "/decide", adminToken(),
                Map.of("approve", false, "note", "Chua du bang chung")).getResponse().getStatus());
        assertEquals(201, submit(c, itemId).getResponse().getStatus(), "bị từ chối rồi thì gửi lại được");
    }

    @Test
    void doubleSubmitAtOnceCreatesExactlyOnePendingRequest() throws Exception {
        Customer c = register();
        String orderId = placeOrder(c, newPart().getProductId());
        complete(orderId);
        String itemId = firstOptionItem(c, orderId);

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return submit(c, itemId).getResponse().getStatus();
            }));
        }
        ready.await();
        go.countDown();
        int created = 0;
        int refused = 0;
        for (Future<Integer> f : results) {
            int s = f.get();
            if (s == 201) {
                created++;
            } else {
                assertEquals(409, s);
                refused++;
            }
        }
        pool.shutdown();
        assertEquals(1, created);
        assertEquals(1, refused);
        assertEquals(1, getJson("/api/warranty-requests", c.token()).size());
    }

    @Test
    void twoAdminsDecidingTheSameRequestAtOnceLeaveExactlyOneWinner() throws Exception {
        Customer c = register();
        String orderId = placeOrder(c, newPart().getProductId());
        complete(orderId);
        String requestId = read(submit(c, firstOptionItem(c, orderId))).get("requestId").asText();
        String admin = adminToken();

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return postJson("/api/admin/warranty-requests/" + requestId + "/decide", admin, Map.of("approve", true))
                        .getResponse().getStatus();
            }));
        }
        ready.await();
        go.countDown();
        int ok = 0;
        int conflict = 0;
        for (Future<Integer> f : results) {
            int s = f.get();
            if (s == 200) {
                ok++;
            } else {
                assertEquals(409, s);
                conflict++;
            }
        }
        pool.shutdown();
        assertEquals(1, ok);
        assertEquals(1, conflict);
        // chỉ một thông báo "đã duyệt" cho khách
        long approvedNotices = notificationRepository.findAll().stream()
                .filter(n -> c.accountId().equals(n.getAccountId()) && n.getContent().contains("đã được duyệt")).count();
        assertEquals(1, approvedNotices);
    }
}
