package com.nguyen.motoparts.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.aftersales.repository.NotificationRepository;
import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.InstallationStatus;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.order.repository.InstallationRequestRepository;
import org.junit.jupiter.api.AfterEach;
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
import java.time.LocalDate;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Kiểm thử Admin quản lý đơn hàng và lịch lắp đặt trên toàn bộ ứng dụng + PostgreSQL thật.
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
class AdminOrderTest {

    private static final String PASSWORD = "Passw0rd!x";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProductRepository productRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired InstallationRequestRepository installationRepository;

    // ---------------------------------------------------------------- helpers

    private record Customer(String accountId, String email, String token) {
    }

    private Customer register() throws Exception {
        String email = "adm-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD)))).andReturn();
        assertEquals(201, r.getResponse().getStatus());
        JsonNode body = read(r);
        return new Customer(body.get("user").get("accountId").asText(), email, body.get("token").asText());
    }

    private String adminToken() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", "admin-test@example.com", "password", "AdminPass123")))).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        return read(r).get("token").asText();
    }

    private Motorbike newBike(String price) {
        Motorbike m = new Motorbike();
        m.setName("Honda Adm " + UUID.randomUUID().toString().substring(0, 6));
        m.setPrice(new BigDecimal(price));
        m.setStatus(ProductStatus.AVAILABLE);
        m.setBrand("Honda");
        m.setModel("Test");
        return productRepository.save(m);
    }

    private SparePart newPart(String price) {
        SparePart p = new SparePart();
        p.setName("Phu tung adm " + UUID.randomUUID().toString().substring(0, 6));
        p.setPrice(new BigDecimal(price));
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

    private void addToCart(Customer c, String productId, int quantity) throws Exception {
        MvcResult r = mvc.perform(auth(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("productId", productId, "quantity", quantity))), c.token())).andReturn();
        assertEquals(200, r.getResponse().getStatus());
    }

    private Map<String, Object> checkout() {
        Map<String, Object> body = new HashMap<>();
        body.put("deliveryMethod", "DELIVERY");
        body.put("receiverName", "Nguyen Van A");
        body.put("receiverPhone", "0912345678");
        body.put("shippingAddress", "12 Le Loi, Quan 1");
        body.put("paymentMethod", "COD");
        body.put("returnPolicyAccepted", true);
        return body;
    }

    private String place(Customer c, Map<String, Object> body) throws Exception {
        MvcResult r = mvc.perform(auth(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), c.token())).andReturn();
        assertEquals(201, r.getResponse().getStatus(), "đặt hàng chuẩn bị cho test phải thành công");
        return read(r).get("orderId").asText();
    }

    /** Đơn COD giao tận nơi có một phụ tùng. */
    private String partOrder(Customer c) throws Exception {
        addToCart(c, newPart("100000").getProductId(), 1);
        return place(c, checkout());
    }

    private MvcResult adminPost(String admin, String url, Object body) throws Exception {
        MockHttpServletRequestBuilder b = post(url).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            b.content(json.writeValueAsString(body));
        }
        return mvc.perform(auth(b, admin)).andReturn();
    }

    private MvcResult setStatus(String admin, String orderId, String status) throws Exception {
        return adminPost(admin, "/api/admin/orders/" + orderId + "/status", Map.of("status", status));
    }

    private JsonNode adminOrder(String admin, String orderId) throws Exception {
        MvcResult r = mvc.perform(auth(get("/api/admin/orders/" + orderId), admin)).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        return read(r);
    }

    /**
     * Một ngày hẹn còn hoàn toàn trống (trong khoảng 2 đến 56 ngày tới). DB test giữ dữ liệu giữa các lần chạy nên không
     * thể chọn ngày bừa: trúng ngày đã kín lịch thì đặt hàng bị 409 và test chập chờn.
     */
    private LocalDate randomFutureDate() {
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 300; i++) {
            LocalDate d = LocalDate.now().plusDays(2 + random.nextInt(55));
            if (installationRepository.countByPreferredDateAndStatusNot(d, InstallationStatus.CANCELLED) == 0) {
                return d;
            }
        }
        throw new AssertionError("không còn ngày trống để thử lịch lắp đặt");
    }

    /** Dọn sau mỗi test: hủy mọi lịch lắp đặt còn hiệu lực để lần chạy sau không bị ngày kín lịch của lần trước. */
    @AfterEach
    void releaseInstallationSlots() {
        installationRepository.findAll().stream()
                .filter(r -> r.getStatus() != InstallationStatus.CANCELLED)
                .forEach(r -> {
                    r.setStatus(InstallationStatus.CANCELLED);
                    installationRepository.save(r);
                });
    }

    /** Đơn lắp đặt (phụ tùng + phí công) vào ngày date; trả {orderId, requestId}. */
    private String[] installOrder(Customer c, String admin, LocalDate date) throws Exception {
        addToCart(c, newPart("80000").getProductId(), 1);
        Map<String, Object> body = checkout();
        body.put("deliveryMethod", "INSTALL");
        body.put("shippingAddress", null);
        body.put("installDate", date.toString());
        String orderId = place(c, body);
        for (JsonNode row : read(mvc.perform(auth(get("/api/admin/installations"), admin)).andReturn())) {
            if (orderId.equals(row.get("orderId").asText())) {
                return new String[]{orderId, row.get("requestId").asText()};
            }
        }
        throw new AssertionError("không thấy lịch lắp đặt của đơn vừa đặt");
    }

    // ------------------------------------------------------------------ tests

    @Test
    void adminEndpointsAreForAdminsOnly() throws Exception {
        Customer c = register();
        assertEquals(401, mvc.perform(get("/api/admin/orders")).andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(auth(get("/api/admin/orders"), c.token())).andReturn().getResponse().getStatus());
        assertEquals(403, mvc.perform(auth(get("/api/admin/installations"), c.token())).andReturn().getResponse().getStatus());
        assertEquals(403, adminPost(c.token(), "/api/admin/orders/x/status", Map.of("status", "CONFIRMED")).getResponse().getStatus());
        assertEquals(403, adminPost(c.token(), "/api/admin/orders/x/confirm-payment", null).getResponse().getStatus());
    }

    @Test
    void listFiltersByStatusAndShowsTheCustomer() throws Exception {
        String admin = adminToken();
        Customer c = register();
        String orderId = partOrder(c);

        MvcResult all = mvc.perform(auth(get("/api/admin/orders?size=50"), admin)).andReturn();
        assertEquals(200, all.getResponse().getStatus());
        JsonNode firstPage = read(all);
        assertTrue(firstPage.get("totalItems").asLong() >= 1);
        assertEquals(orderId, firstPage.get("items").get(0).get("orderId").asText(), "đơn mới nhất đứng đầu");
        assertEquals(c.email(), firstPage.get("items").get(0).get("customerEmail").asText());
        assertEquals("PENDING", firstPage.get("items").get(0).get("status").asText());
        assertEquals("COD", firstPage.get("items").get(0).get("paymentMethod").asText());

        JsonNode cancelledOnly = read(mvc.perform(auth(get("/api/admin/orders?status=CANCELLED&size=50"), admin)).andReturn());
        for (JsonNode row : cancelledOnly.get("items")) {
            assertEquals("CANCELLED", row.get("status").asText());
        }
        assertEquals(400, mvc.perform(auth(get("/api/admin/orders?status=XYZ"), admin)).andReturn().getResponse().getStatus());
        assertEquals(200, mvc.perform(auth(get("/api/admin/orders?status=ALL"), admin)).andReturn().getResponse().getStatus());
    }

    @Test
    void detailShowsCustomerNextStatusesAndPaymentAction() throws Exception {
        String admin = adminToken();
        Customer c = register();
        String orderId = partOrder(c);

        JsonNode d = adminOrder(admin, orderId);
        assertEquals(orderId, d.get("order").get("orderId").asText());
        assertEquals(c.email(), d.get("customer").get("email").asText());
        assertEquals(c.accountId(), d.get("customer").get("accountId").asText());
        assertEquals(2, d.get("nextStatuses").size());
        assertEquals("CONFIRMED", d.get("nextStatuses").get(0).get("status").asText());
        assertEquals("CANCELLED", d.get("nextStatuses").get(1).get("status").asText());
        assertTrue(d.get("canConfirmPayment").asBoolean());
        assertEquals(404, mvc.perform(auth(get("/api/admin/orders/khong-co"), admin)).andReturn().getResponse().getStatus());
    }

    @Test
    void statusMovesOnlyAlongValidStepsAndCompletingNeedsPayment() throws Exception {
        String admin = adminToken();
        Customer c = register();
        String orderId = partOrder(c);

        assertEquals(409, setStatus(admin, orderId, "COMPLETED").getResponse().getStatus(), "không nhảy cóc");
        assertEquals(409, setStatus(admin, orderId, "PENDING").getResponse().getStatus());
        assertEquals(400, setStatus(admin, orderId, "BAY").getResponse().getStatus());

        MvcResult r = setStatus(admin, orderId, "CONFIRMED");
        assertEquals(200, r.getResponse().getStatus());
        assertEquals("CONFIRMED", read(r).get("order").get("status").asText());
        assertEquals(200, setStatus(admin, orderId, "SHIPPING").getResponse().getStatus());

        r = setStatus(admin, orderId, "COMPLETED");
        assertEquals(409, r.getResponse().getStatus(), "chưa thu tiền thì chưa hoàn thành được");
        assertTrue(message(r).contains("thu tiền"));

        r = adminPost(admin, "/api/admin/orders/" + orderId + "/confirm-payment", null);
        assertEquals(200, r.getResponse().getStatus());
        assertEquals("PAID", read(r).get("order").get("payment").get("status").asText());
        assertEquals(false, read(r).get("canConfirmPayment").asBoolean());

        assertEquals(200, setStatus(admin, orderId, "COMPLETED").getResponse().getStatus());
        assertEquals(409, setStatus(admin, orderId, "CANCELLED").getResponse().getStatus(), "COMPLETED là trạng thái cuối");
        assertEquals(0, adminOrder(admin, orderId).get("nextStatuses").size());
    }

    @Test
    void confirmPaymentWorksOnceAndNotOnCancelledOrders() throws Exception {
        String admin = adminToken();
        Customer c = register();
        String orderId = partOrder(c);

        assertEquals(200, adminPost(admin, "/api/admin/orders/" + orderId + "/confirm-payment", null).getResponse().getStatus());
        MvcResult again = adminPost(admin, "/api/admin/orders/" + orderId + "/confirm-payment", null);
        assertEquals(409, again.getResponse().getStatus());
        assertTrue(message(again).contains("đã được xác nhận"));

        String cancelled = partOrder(register());
        assertEquals(200, setStatus(admin, cancelled, "CANCELLED").getResponse().getStatus());
        assertEquals(409, adminPost(admin, "/api/admin/orders/" + cancelled + "/confirm-payment", null).getResponse().getStatus());
        assertEquals(404, adminPost(admin, "/api/admin/orders/khong-co/confirm-payment", null).getResponse().getStatus());
    }

    @Test
    void cancellingReturnsTheMotorbikeAndFreesTheInstallationSlot() throws Exception {
        String admin = adminToken();
        Customer c = register();
        Motorbike bike = newBike("9000000");
        addToCart(c, bike.getProductId(), 1);
        addToCart(c, newPart("60000").getProductId(), 1);
        LocalDate date = randomFutureDate();
        Map<String, Object> body = checkout();
        body.put("deliveryMethod", "INSTALL");
        body.put("shippingAddress", null);
        body.put("installDate", date.toString());
        MvcResult placed = mvc.perform(auth(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), c.token())).andReturn();
        assertEquals(201, placed.getResponse().getStatus());
        String orderId = read(placed).get("orderId").asText();
        assertEquals(ProductStatus.SOLD, productRepository.findById(bike.getProductId()).orElseThrow().getStatus());
        long before = installationRepository.countByPreferredDateAndStatusNot(date, InstallationStatus.CANCELLED);

        MvcResult r = setStatus(admin, orderId, "CANCELLED");
        assertEquals(200, r.getResponse().getStatus());
        assertEquals("CANCELLED", read(r).get("order").get("status").asText());
        assertEquals("CANCELLED", read(r).get("order").get("installation").get("status").asText());

        assertEquals(ProductStatus.AVAILABLE, productRepository.findById(bike.getProductId()).orElseThrow().getStatus(),
                "xe được trả về bán tiếp");
        assertEquals(before - 1, installationRepository.countByPreferredDateAndStatusNot(date, InstallationStatus.CANCELLED),
                "chỗ trống của ngày hẹn được trả lại");
        assertEquals(409, setStatus(admin, orderId, "CONFIRMED").getResponse().getStatus());
    }

    @Test
    void paidOrderCannotBeCancelled() throws Exception {
        String admin = adminToken();
        String orderId = partOrder(register());
        assertEquals(200, adminPost(admin, "/api/admin/orders/" + orderId + "/confirm-payment", null).getResponse().getStatus());
        MvcResult r = setStatus(admin, orderId, "CANCELLED");
        assertEquals(409, r.getResponse().getStatus());
        assertTrue(message(r).contains("đã thu tiền"));
        assertEquals("PENDING", adminOrder(admin, orderId).get("order").get("status").asText());
    }

    @Test
    void customerIsNotifiedWhenTheStatusOrPaymentChanges() throws Exception {
        String admin = adminToken();
        Customer c = register();
        String orderId = partOrder(c);
        String code = adminOrder(admin, orderId).get("order").get("shortCode").asText();

        setStatus(admin, orderId, "CONFIRMED");
        adminPost(admin, "/api/admin/orders/" + orderId + "/confirm-payment", null);

        List<String> contents = notificationRepository.findAll().stream()
                .filter(n -> c.accountId().equals(n.getAccountId())).map(n -> n.getContent()).toList();
        assertTrue(contents.stream().anyMatch(t -> t.contains(code) && t.contains("Đã xác nhận")), contents.toString());
        assertTrue(contents.stream().anyMatch(t -> t.contains(code) && t.contains("thu tiền")), contents.toString());
    }

    @Test
    void installationListAndDoneNeedsAConfirmedOrder() throws Exception {
        String admin = adminToken();
        Customer c = register();
        String[] ids = installOrder(c, admin, randomFutureDate());

        JsonNode scheduled = read(mvc.perform(auth(get("/api/admin/installations?status=SCHEDULED"), admin)).andReturn());
        boolean found = false;
        for (JsonNode row : scheduled) {
            assertEquals("SCHEDULED", row.get("status").asText());
            if (ids[1].equals(row.get("requestId").asText())) {
                found = true;
                assertEquals(c.email(), row.get("customerEmail").asText());
            }
        }
        assertTrue(found);
        assertEquals(400, mvc.perform(auth(get("/api/admin/installations?status=XYZ"), admin)).andReturn().getResponse().getStatus());

        MvcResult r = adminPost(admin, "/api/admin/installations/" + ids[1] + "/action", Map.of("action", "DONE"));
        assertEquals(409, r.getResponse().getStatus(), "đơn chưa xác nhận thì chưa lắp xong được");

        setStatus(admin, ids[0], "CONFIRMED");
        r = adminPost(admin, "/api/admin/installations/" + ids[1] + "/action", Map.of("action", "DONE"));
        assertEquals(200, r.getResponse().getStatus());
        assertEquals("DONE", read(r).get("status").asText());

        assertEquals(409, adminPost(admin, "/api/admin/installations/" + ids[1] + "/action", Map.of("action", "CANCEL"))
                .getResponse().getStatus(), "lịch đã xử lý không đổi nữa");
        assertEquals(404, adminPost(admin, "/api/admin/installations/khong-co/action", Map.of("action", "DONE"))
                .getResponse().getStatus());
        assertEquals(400, adminPost(admin, "/api/admin/installations/" + ids[1] + "/action", Map.of()).getResponse().getStatus());
    }

    @Test
    void installationCanBeCancelledOrRescheduledWithinTheRules() throws Exception {
        String admin = adminToken();
        LocalDate date = randomFutureDate();
        String[] ids = installOrder(register(), admin, date);

        // đổi lịch: cùng ngày, ngày quá khứ, ngày quá xa đều bị từ chối
        assertEquals(400, adminPost(admin, "/api/admin/installations/" + ids[1] + "/action",
                Map.of("action", "RESCHEDULE", "newDate", date.toString())).getResponse().getStatus());
        assertEquals(400, adminPost(admin, "/api/admin/installations/" + ids[1] + "/action",
                Map.of("action", "RESCHEDULE", "newDate", LocalDate.now().toString())).getResponse().getStatus());
        assertEquals(400, adminPost(admin, "/api/admin/installations/" + ids[1] + "/action",
                Map.of("action", "RESCHEDULE", "newDate", LocalDate.now().plusDays(90).toString())).getResponse().getStatus());
        assertEquals(400, adminPost(admin, "/api/admin/installations/" + ids[1] + "/action",
                Map.of("action", "RESCHEDULE")).getResponse().getStatus());

        LocalDate newDate = randomFutureDate();   // ngày đang dùng đã có 1 lịch nên không bị chọn lại
        MvcResult r = adminPost(admin, "/api/admin/installations/" + ids[1] + "/action",
                Map.of("action", "RESCHEDULE", "newDate", newDate.toString()));
        assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals(newDate.toString(), read(r).get("preferredDate").asText());

        r = adminPost(admin, "/api/admin/installations/" + ids[1] + "/action", Map.of("action", "CANCEL"));
        assertEquals(200, r.getResponse().getStatus());
        assertEquals("CANCELLED", read(r).get("status").asText());
        assertEquals("CANCELLED", adminOrder(admin, ids[0]).get("order").get("installation").get("status").asText());
    }

    @Test
    void reschedulingToAFullDayIsRefused() throws Exception {
        String admin = adminToken();
        LocalDate full = randomFutureDate();
        // làm đầy ngày full (tối đa 5 lượt)
        for (int i = 0; i < 6; i++) {
            Customer c = register();
            addToCart(c, newPart("10000").getProductId(), 1);
            Map<String, Object> body = checkout();
            body.put("deliveryMethod", "INSTALL");
            body.put("shippingAddress", null);
            body.put("installDate", full.toString());
            int status = mvc.perform(auth(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(body)), c.token())).andReturn().getResponse().getStatus();
            if (status == 409) {
                break;
            }
        }
        assertEquals(5, installationRepository.countByPreferredDateAndStatusNot(full, InstallationStatus.CANCELLED));

        LocalDate other = randomFutureDate();   // ngày full đã kín nên không bị chọn lại
        String[] ids = installOrder(register(), admin, other);
        MvcResult r = adminPost(admin, "/api/admin/installations/" + ids[1] + "/action",
                Map.of("action", "RESCHEDULE", "newDate", full.toString()));
        assertEquals(409, r.getResponse().getStatus());
        assertTrue(message(r).contains("kín lịch"));
        assertEquals(other.toString(), read(mvc.perform(auth(get("/api/admin/installations"), admin)).andReturn()).findValues("preferredDate")
                .stream().filter(n -> n.asText().equals(other.toString())).findFirst().orElseThrow().asText(),
                "lịch cũ còn nguyên khi đổi bị từ chối");
    }

    @Test
    void twoAdminsChangingTheSameOrderAtOnceLeaveExactlyOneWinner() throws Exception {
        String admin = adminToken();
        String orderId = partOrder(register());

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return setStatus(admin, orderId, "CONFIRMED").getResponse().getStatus();
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
        assertEquals(1, conflict, "thao tác thứ hai thấy đơn đã CONFIRMED nên bị từ chối");
        assertEquals("CONFIRMED", adminOrder(admin, orderId).get("order").get("status").asText());
    }
}
