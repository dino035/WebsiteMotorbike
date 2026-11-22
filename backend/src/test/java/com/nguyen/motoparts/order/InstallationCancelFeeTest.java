package com.nguyen.motoparts.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.InstallationStatus;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.order.entity.OnlinePayment;
import com.nguyen.motoparts.order.repository.InstallationRequestRepository;
import com.nguyen.motoparts.order.repository.OnlinePaymentRepository;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Hủy riêng lịch lắp đặt thì phí công phải được trừ khỏi đơn (Order.recalculateTotal) và khỏi số tiền cần thu, trừ khi
 * đơn đã thu tiền hoặc khách đang có liên kết VNPay với số tiền cũ. Chạy khi có TEST_DB_URL (xem AuthSecurityTest).
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
                "app.vnpay.tmn-code=TESTTMN",
                "app.vnpay.hash-secret=test-vnpay-secret-0123456789",
                "app.vnpay.pay-url=https://sandbox.example/pay",
                "app.vnpay.return-url=http://localhost:3000/payment/vnpay-return"
        })
@AutoConfigureMockMvc
class InstallationCancelFeeTest {

    private static final String PASSWORD = "Passw0rd!x";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProductRepository productRepository;
    @Autowired InstallationRequestRepository installationRepository;
    @Autowired OnlinePaymentRepository onlinePaymentRepository;

    // ---------------------------------------------------------------- helpers

    private record Customer(String email, String token) {
    }

    private JsonNode read(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String message(MvcResult r) throws Exception {
        return read(r).get("message").asText();
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    private Customer register() throws Exception {
        String email = "fee-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD)))).andReturn();
        assertEquals(201, r.getResponse().getStatus());
        return new Customer(email, read(r).get("token").asText());
    }

    private String adminToken() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", "admin-test@example.com", "password", "AdminPass123")))).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        return read(r).get("token").asText();
    }

    private SparePart newPart(String price) {
        SparePart p = new SparePart();
        p.setName("Phu tung phi " + UUID.randomUUID().toString().substring(0, 6));
        p.setPrice(new BigDecimal(price));
        p.setStatus(ProductStatus.AVAILABLE);
        p.setPartType("Nhot");
        return productRepository.save(p);
    }

    private void addToCart(Customer c, String productId, int quantity) throws Exception {
        MvcResult r = mvc.perform(auth(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("productId", productId, "quantity", quantity))), c.token())).andReturn();
        assertEquals(200, r.getResponse().getStatus());
    }

    private MvcResult adminPost(String admin, String url, Object body) throws Exception {
        MockHttpServletRequestBuilder b = post(url).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            b.content(json.writeValueAsString(body));
        }
        return mvc.perform(auth(b, admin)).andReturn();
    }

    private MvcResult cancelInstallation(String admin, String requestId) throws Exception {
        return adminPost(admin, "/api/admin/installations/" + requestId + "/action", Map.of("action", "CANCEL"));
    }

    private JsonNode adminOrder(String admin, String orderId) throws Exception {
        MvcResult r = mvc.perform(auth(get("/api/admin/orders/" + orderId), admin)).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        return read(r).get("order");
    }

    /** Một ngày hẹn còn hoàn toàn trống (DB test giữ dữ liệu giữa các lần chạy nên không chọn ngày bừa). */
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

    /** Trả chỗ trống của mọi lịch còn hiệu lực sau mỗi test, để các lần chạy sau không gặp ngày kín lịch. */
    @AfterEach
    void releaseInstallationSlots() {
        installationRepository.findAll().stream()
                .filter(r -> r.getStatus() != InstallationStatus.CANCELLED)
                .forEach(r -> {
                    r.setStatus(InstallationStatus.CANCELLED);
                    installationRepository.save(r);
                });
    }

    /** Đơn lắp đặt (một phụ tùng 80.000 + phí công 50.000 = 130.000) trả bằng paymentMethod; trả {orderId, requestId}. */
    private String[] installOrder(Customer c, String admin, String paymentMethod) throws Exception {
        addToCart(c, newPart("80000").getProductId(), 1);
        Map<String, Object> body = new HashMap<>();
        body.put("deliveryMethod", "INSTALL");
        body.put("receiverName", "Nguyen Van A");
        body.put("receiverPhone", "0912345678");
        body.put("shippingAddress", null);
        body.put("installDate", randomFutureDate().toString());
        body.put("paymentMethod", paymentMethod);
        body.put("returnPolicyAccepted", true);
        MvcResult r = mvc.perform(auth(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), c.token())).andReturn();
        assertEquals(201, r.getResponse().getStatus(), "đặt hàng chuẩn bị cho test phải thành công");
        String orderId = read(r).get("orderId").asText();
        for (JsonNode row : read(mvc.perform(auth(get("/api/admin/installations"), admin)).andReturn())) {
            if (orderId.equals(row.get("orderId").asText())) {
                return new String[]{orderId, row.get("requestId").asText()};
            }
        }
        throw new AssertionError("không thấy lịch lắp đặt của đơn vừa đặt");
    }

    private static void assertMoney(String expected, JsonNode actual, String why) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.decimalValue()), why + " (thực tế " + actual + ")");
    }

    /** Công thức mà mọi đơn phải thỏa: tổng các dòng hàng, cộng phí công nếu lịch chưa hủy. */
    private static void assertTotalMatchesItsParts(JsonNode order) {
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode item : order.get("items")) {
            sum = sum.add(item.get("lineTotal").decimalValue());
        }
        JsonNode inst = order.get("installation");
        if (inst != null && !inst.isNull() && !"CANCELLED".equals(inst.get("status").asText())) {
            sum = sum.add(inst.get("laborFee").decimalValue());
        }
        assertEquals(0, sum.compareTo(order.get("totalAmount").decimalValue()),
                "tổng đơn phải bằng tổng các thành phần của nó: " + order);
    }

    // ------------------------------------------------------------------ tests

    @Test
    void cancellingTheInstallationTakesTheLaborFeeOffTheOrderAndThePayment() throws Exception {
        String admin = adminToken();
        Customer c = register();
        String[] ids = installOrder(c, admin, "COD");

        JsonNode before = adminOrder(admin, ids[0]);
        assertMoney("130000", before.get("totalAmount"), "đặt hàng: tổng gồm cả phí công");
        assertMoney("130000", before.get("payment").get("amount"), "số tiền cần thu khớp tổng đơn");
        assertTotalMatchesItsParts(before);

        MvcResult r = cancelInstallation(admin, ids[1]);
        assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString(StandardCharsets.UTF_8));

        JsonNode after = adminOrder(admin, ids[0]);
        assertMoney("80000", after.get("totalAmount"), "hủy lắp đặt: tổng đơn bớt đúng phí công");
        assertMoney("80000", after.get("payment").get("amount"), "số tiền cần thu cũng giảm theo");
        assertMoney("50000", after.get("installation").get("laborFee"), "phí công vẫn ghi trong lịch để đối chiếu");
        assertEquals("CANCELLED", after.get("installation").get("status").asText());
        assertTotalMatchesItsParts(after);

        // khách thấy số mới trên đơn của mình và được báo
        JsonNode mine = read(mvc.perform(auth(get("/api/orders/" + ids[0]), c.token())).andReturn());
        assertMoney("80000", mine.get("totalAmount"), "khách thấy tổng mới");
        String notes = read(mvc.perform(auth(get("/api/notifications?size=50"), c.token())).andReturn()).toString();
        assertTrue(notes.contains("Tổng tiền mới: 80.000đ"), notes);

        // Admin thu tiền COD theo số mới
        r = adminPost(admin, "/api/admin/orders/" + ids[0] + "/confirm-payment", null);
        assertEquals(200, r.getResponse().getStatus());
        assertMoney("80000", read(r).get("order").get("payment").get("amount"), "thu đúng số đã trừ");
    }

    @Test
    void cancellingTwiceNeverTakesTheFeeOffTwice() throws Exception {
        String admin = adminToken();
        String[] ids = installOrder(register(), admin, "COD");

        assertEquals(200, cancelInstallation(admin, ids[1]).getResponse().getStatus());
        assertEquals(409, cancelInstallation(admin, ids[1]).getResponse().getStatus(), "lịch đã xử lý rồi");
        JsonNode order = adminOrder(admin, ids[0]);
        assertMoney("80000", order.get("totalAmount"), "không bị trừ lần hai thành 30.000");
        assertMoney("80000", order.get("payment").get("amount"), "số tiền cần thu cũng không đổi");
    }

    @Test
    void anAlreadyPaidOrderKeepsItsInstallationAndTellsTheAdminToRefundByHand() throws Exception {
        String admin = adminToken();
        String[] ids = installOrder(register(), admin, "COD");
        assertEquals(200, adminPost(admin, "/api/admin/orders/" + ids[0] + "/confirm-payment", null).getResponse().getStatus());

        MvcResult r = cancelInstallation(admin, ids[1]);
        assertEquals(409, r.getResponse().getStatus());
        assertTrue(message(r).contains("hoàn lại phí công 50.000đ"), message(r));

        JsonNode order = adminOrder(admin, ids[0]);
        assertMoney("130000", order.get("totalAmount"), "đã thu 130.000 thì tổng đơn không đổi");
        assertMoney("130000", order.get("payment").get("amount"), "số đã thu và số trên đơn không lệch nhau");
        assertEquals("SCHEDULED", order.get("installation").get("status").asText(), "lịch vẫn còn nguyên");
        assertTotalMatchesItsParts(order);
    }

    @Test
    void aLiveVnpayLinkWithTheOldAmountBlocksTheCancelUntilItExpires() throws Exception {
        String admin = adminToken();
        Customer c = register();
        String[] ids = installOrder(c, admin, "VNPAY");
        MvcResult link = mvc.perform(auth(post("/api/orders/" + ids[0] + "/vnpay"), c.token())).andReturn();
        assertEquals(200, link.getResponse().getStatus(), "khách bấm trả tiền: có một liên kết 130.000 còn hiệu lực");

        MvcResult r = cancelInstallation(admin, ids[1]);
        assertEquals(409, r.getResponse().getStatus());
        assertTrue(message(r).contains("VNPay"), message(r));
        JsonNode order = adminOrder(admin, ids[0]);
        assertMoney("130000", order.get("totalAmount"), "bị từ chối thì đơn không đổi");
        assertEquals("SCHEDULED", order.get("installation").get("status").asText());

        // liên kết đã hết hạn từ lâu (không còn trả được số cũ): được hủy, số cần thu theo tổng mới
        for (OnlinePayment attempt : onlinePaymentRepository.findByOrderIdOrderByCreatedAtDesc(ids[0])) {
            attempt.setCreatedAt(LocalDateTime.now().minusHours(3));
            onlinePaymentRepository.save(attempt);
        }
        r = cancelInstallation(admin, ids[1]);
        assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        order = adminOrder(admin, ids[0]);
        assertMoney("80000", order.get("totalAmount"), "hết liên kết cũ thì trừ được");
        assertMoney("80000", order.get("payment").get("amount"), "lần trả tiền sau dùng số mới");
        assertTotalMatchesItsParts(order);
    }

    @Test
    void everyKindOfOrderTotalEqualsItsParts() throws Exception {
        String admin = adminToken();
        Customer c = register();
        // đơn giao hàng nhiều dòng và đơn lắp đặt
        addToCart(c, newPart("100000").getProductId(), 2);
        addToCart(c, newPart("30000").getProductId(), 3);
        Map<String, Object> body = new HashMap<>();
        body.put("deliveryMethod", "DELIVERY");
        body.put("receiverName", "Nguyen Van A");
        body.put("receiverPhone", "0912345678");
        body.put("shippingAddress", "12 Le Loi, Quan 1");
        body.put("paymentMethod", "COD");
        body.put("returnPolicyAccepted", true);
        MvcResult r = mvc.perform(auth(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), c.token())).andReturn();
        assertEquals(201, r.getResponse().getStatus());
        String deliveryId = read(r).get("orderId").asText();
        String[] install = installOrder(register(), admin, "COD");

        JsonNode d = adminOrder(admin, deliveryId);
        assertMoney("290000", d.get("totalAmount"), "2 x 100.000 + 3 x 30.000");
        assertTotalMatchesItsParts(d);
        assertTotalMatchesItsParts(adminOrder(admin, install[0]));
    }
}
