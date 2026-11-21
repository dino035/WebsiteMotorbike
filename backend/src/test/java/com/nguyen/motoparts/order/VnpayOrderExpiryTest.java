package com.nguyen.motoparts.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.aftersales.repository.NotificationRepository;
import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.order.entity.Order;
import com.nguyen.motoparts.order.repository.OrderRepository;
import com.nguyen.motoparts.order.service.VnpayOrderExpiryJob;
import com.nguyen.motoparts.order.service.VnpayOrderExpiryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Kiểm thử tự hủy đơn VNPay chưa trả tiền (xe được mở bán lại). Thời gian được truyền vào tác vụ nên không phải chờ 15 phút.
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
                "app.cart.hold-minutes=30",
                "app.vnpay.tmn-code=TESTTMN",
                "app.vnpay.hash-secret=test-vnpay-secret-0123456789",
                "app.vnpay.pay-url=https://sandbox.example/pay",
                "app.vnpay.return-url=http://localhost:3000/payment/vnpay-return",
                "app.vnpay.expire-minutes=15",
                "app.vnpay.order-hold-minutes=15",
                // ca test kiểm tra tác vụ được đăng ký thật: bật lịch, nhưng để rất thưa để không tự chạy giữa chừng
                "app.scheduling.enabled=true",
                "app.vnpay.order-check-ms=3600000"
        })
@AutoConfigureMockMvc
class VnpayOrderExpiryTest {

    private static final String PASSWORD = "Passw0rd!x";
    private static final String TMN = "TESTTMN";
    private static final String SECRET = "test-vnpay-secret-0123456789";
    private static final int HOLD = 15;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired VnpayOrderExpiryJob job;
    @Autowired VnpayOrderExpiryService service;
    @Autowired ScheduledAnnotationBeanPostProcessor scheduler;

    // ---------------------------------------------------------------- helpers

    private record Customer(String accountId, String token) {
    }

    private Customer register() throws Exception {
        String email = "exp-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
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

    private Motorbike newBike() {
        Motorbike m = new Motorbike();
        m.setName("Honda Exp " + UUID.randomUUID().toString().substring(0, 6));
        m.setPrice(new BigDecimal("15000000"));
        m.setStatus(ProductStatus.AVAILABLE);
        m.setBrand("Honda");
        m.setModel("Test");
        return productRepository.save(m);
    }

    private SparePart newPart() {
        SparePart p = new SparePart();
        p.setName("Phu tung exp " + UUID.randomUUID().toString().substring(0, 6));
        p.setPrice(new BigDecimal("100000"));
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

    private void addToCart(Customer c, String productId) throws Exception {
        MvcResult r = mvc.perform(auth(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("productId", productId, "quantity", 1))), c.token())).andReturn();
        assertEquals(200, r.getResponse().getStatus());
    }

    private String place(Customer c, String productId, String method, boolean install) throws Exception {
        addToCart(c, productId);
        Map<String, Object> body = new HashMap<>();
        body.put("deliveryMethod", install ? "INSTALL" : "DELIVERY");
        body.put("receiverName", "Nguyen Van A");
        body.put("receiverPhone", "0912345678");
        if (install) {
            body.put("installDate", LocalDate.now().plusDays(3).toString());
        } else {
            body.put("shippingAddress", "12 Le Loi, Quan 1");
        }
        body.put("paymentMethod", method);
        body.put("returnPolicyAccepted", true);
        MvcResult r = mvc.perform(auth(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), c.token())).andReturn();
        assertEquals(201, r.getResponse().getStatus(), r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return read(r).get("orderId").asText();
    }

    private String vnpayPartOrder(Customer c) throws Exception {
        return place(c, newPart().getProductId(), "VNPAY", false);
    }

    private JsonNode order(Customer c, String orderId) throws Exception {
        return read(mvc.perform(auth(get("/api/orders/" + orderId), c.token())).andReturn());
    }

    private String txnRefOf(Customer c, String orderId) throws Exception {
        MvcResult r = mvc.perform(auth(post("/api/orders/" + orderId + "/vnpay"), c.token())).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        return linkParams(read(r).get("paymentUrl").asText()).get("vnp_TxnRef");
    }

    private LocalDateTime orderDate(String orderId) {
        Order o = orderRepository.findById(orderId).orElseThrow();
        return o.getOrderDate();
    }

    private String productStatus(String productId) {
        return productRepository.findById(productId).orElseThrow().getStatus().name();
    }

    private long notificationsContaining(Customer c, String text) {
        return notificationRepository.findAll().stream()
                .filter(n -> c.accountId().equals(n.getAccountId()) && n.getContent().contains(text)).count();
    }

    // ---- phần "phía VNPay": ký thông báo kết quả, độc lập với code của dự án ----

    private static String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            StringBuilder hex = new StringBuilder();
            for (byte b : mac.doFinal(data.getBytes(StandardCharsets.UTF_8))) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> linkParams(String url) {
        Map<String, String> map = new HashMap<>();
        for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
            int eq = pair.indexOf('=');
            map.put(pair.substring(0, eq), URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return map;
    }

    private static Map<String, String> callback(String txnRef, String amountVnd, String responseCode) {
        Map<String, String> p = new HashMap<>();
        p.put("vnp_TmnCode", TMN);
        p.put("vnp_TxnRef", txnRef);
        p.put("vnp_Amount", String.valueOf(Long.parseLong(amountVnd) * 100));
        p.put("vnp_ResponseCode", responseCode);
        p.put("vnp_TransactionStatus", responseCode);
        p.put("vnp_TransactionNo", "1" + Math.abs(txnRef.hashCode() % 1_000_000));
        p.put("vnp_BankCode", "NCB");
        p.put("vnp_OrderInfo", "Thanh toan don hang");
        p.put("vnp_PayDate", "20261120101010");
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, String> e : new TreeMap<>(p).entrySet()) {
            if (data.length() > 0) {
                data.append('&');
            }
            data.append(e.getKey()).append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        p.put("vnp_SecureHash", hmac(data.toString()));
        return p;
    }

    private MvcResult paymentReturn(Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder b = get("/api/payments/vnpay/return");
        params.forEach(b::param);
        return mvc.perform(b).andReturn();
    }

    // ------------------------------------------------------------------ tests

    @Test
    void unpaidVnpayOrderIsCancelledAndTheMotorbikeGoesBackOnSale() throws Exception {
        Customer c = register();
        Motorbike bike = newBike();
        String orderId = place(c, bike.getProductId(), "VNPAY", false);
        assertEquals("SOLD", productStatus(bike.getProductId()), "đặt xe xong xe bị giữ");

        LocalDateTime due = orderDate(orderId).plusMinutes(HOLD);
        assertFalse(service.expireIfStillUnpaid(orderId, due.minusSeconds(1)), "chưa tới hạn thì chưa hủy");
        assertEquals("SOLD", productStatus(bike.getProductId()));
        assertEquals("PENDING", order(c, orderId).get("status").asText());

        assertTrue(job.runOnce(due.plusSeconds(1)) >= 1, "quá hạn thì tác vụ hủy đơn");
        JsonNode o = order(c, orderId);
        assertEquals("CANCELLED", o.get("status").asText());
        assertEquals("FAILED", o.get("payment").get("status").asText(), "đơn đã hủy không còn ở trạng thái chờ thanh toán");
        assertFalse(o.get("payment").get("canPayOnline").asBoolean(), "đơn đã hủy không còn nút trả tiền");
        assertEquals("AVAILABLE", productStatus(bike.getProductId()), "xe được mở bán lại");
        assertEquals(1, notificationsContaining(c, "tự động hủy"), "khách được báo vì sao đơn bị hủy");

        // khách khác mua được chiếc xe vừa được nhả
        Customer other = register();
        place(other, bike.getProductId(), "COD", false);
        assertEquals("SOLD", productStatus(bike.getProductId()));
    }

    @Test
    void exactlyAtTheDeadlineTheOrderIsCancelled() throws Exception {
        Customer c = register();
        String orderId = vnpayPartOrder(c);
        LocalDateTime due = orderDate(orderId).plusMinutes(HOLD);
        assertFalse(service.expireIfStillUnpaid(orderId, due.minusNanos(1_000)));
        assertTrue(service.expireIfStillUnpaid(orderId, due), "đúng hạn là hủy");
        assertFalse(service.expireIfStillUnpaid(orderId, due.plusMinutes(5)), "đơn đã hủy thì lần sau không làm gì nữa");
        assertEquals(1, notificationsContaining(c, "tự động hủy"), "chạy lặp lại không báo trùng");
    }

    @Test
    void aCancelledOrderCanNoLongerBePaid() throws Exception {
        Customer c = register();
        String orderId = vnpayPartOrder(c);
        assertTrue(service.expireIfStillUnpaid(orderId, orderDate(orderId).plusMinutes(HOLD + 1)));

        MvcResult r = mvc.perform(auth(post("/api/orders/" + orderId + "/vnpay"), c.token())).andReturn();
        assertEquals(409, r.getResponse().getStatus());
    }

    @Test
    void codOrdersAreNeverExpired() throws Exception {
        Customer c = register();
        String orderId = place(c, newPart().getProductId(), "COD", false);
        assertFalse(service.expireIfStillUnpaid(orderId, orderDate(orderId).plusDays(3)));
        assertEquals("PENDING", order(c, orderId).get("status").asText());
    }

    @Test
    void anOrderSwitchedToCodIsKept() throws Exception {
        Customer c = register();
        String orderId = vnpayPartOrder(c);
        assertEquals(200, mvc.perform(auth(post("/api/orders/" + orderId + "/pay-cod"), c.token()))
                .andReturn().getResponse().getStatus());
        assertFalse(service.expireIfStillUnpaid(orderId, orderDate(orderId).plusDays(3)), "đã chọn COD thì không tự hủy");
        assertEquals("PENDING", order(c, orderId).get("status").asText());
    }

    @Test
    void aPaidOrderIsNeverExpired() throws Exception {
        Customer c = register();
        String orderId = vnpayPartOrder(c);
        String txnRef = txnRefOf(c, orderId);
        assertEquals(200, paymentReturn(callback(txnRef, "100000", "00")).getResponse().getStatus());
        assertEquals("PAID", order(c, orderId).get("payment").get("status").asText());

        assertFalse(service.expireIfStillUnpaid(orderId, orderDate(orderId).plusDays(3)));
        assertEquals("PENDING", order(c, orderId).get("status").asText(), "đơn đã trả tiền giữ nguyên");
    }

    @Test
    void anOrderThatTheShopAlreadyConfirmedIsLeftAlone() throws Exception {
        Customer c = register();
        String orderId = vnpayPartOrder(c);
        MvcResult r = mvc.perform(auth(post("/api/admin/orders/" + orderId + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"), adminToken())).andReturn();
        assertEquals(200, r.getResponse().getStatus());

        assertFalse(service.expireIfStillUnpaid(orderId, orderDate(orderId).plusDays(3)), "Admin đã xử lý thì không tự hủy");
        assertEquals("CONFIRMED", order(c, orderId).get("status").asText());
    }

    @Test
    void aFailedPaymentAttemptStillExpiresOnceTheLinkIsDead() throws Exception {
        Customer c = register();
        String orderId = vnpayPartOrder(c);
        String txnRef = txnRefOf(c, orderId);
        assertEquals(200, paymentReturn(callback(txnRef, "100000", "24")).getResponse().getStatus());   // khách hủy trên cổng
        assertEquals("FAILED", order(c, orderId).get("payment").get("status").asText());

        LocalDateTime now = LocalDateTime.now();
        assertFalse(service.expireIfStillUnpaid(orderId, now.plusMinutes(HOLD + 1)),
                "vừa thử xong, liên kết (15 phút + 2 phút chờ) coi như còn hiệu lực nên chưa hủy");
        assertTrue(service.expireIfStillUnpaid(orderId, now.plusMinutes(HOLD + 20)), "bỏ dở lâu thì hủy");
        assertEquals("CANCELLED", order(c, orderId).get("status").asText());
    }

    @Test
    void aRecentRetryKeepsTheOrderUntilItsLinkExpires() throws Exception {
        Customer c = register();
        String orderId = vnpayPartOrder(c);
        LocalDateTime base = LocalDateTime.now();   // lần thử lại diễn ra "bây giờ", đơn đặt trước đó
        txnRefOf(c, orderId);

        // quá hạn giữ đơn nhưng liên kết vừa tạo còn sống: khách có thể đang nhập OTP, không được hủy
        assertFalse(service.expireIfStillUnpaid(orderId, base.plusMinutes(HOLD + 1)));
        assertEquals("PENDING", order(c, orderId).get("status").asText());
        // liên kết hết hạn (15 phút) cộng thời gian chờ IPN (2 phút) rồi mới hủy
        assertFalse(service.expireIfStillUnpaid(orderId, base.plusMinutes(15 + 1)));
        assertTrue(service.expireIfStillUnpaid(orderId, base.plusMinutes(15 + 3)));
        assertEquals("CANCELLED", order(c, orderId).get("status").asText());
    }

    @Test
    void cancellingAnInstallationOrderFreesTheAppointment() throws Exception {
        Customer c = register();
        String orderId = place(c, newPart().getProductId(), "VNPAY", true);
        assertEquals("SCHEDULED", order(c, orderId).get("installation").get("status").asText());

        assertTrue(service.expireIfStillUnpaid(orderId, orderDate(orderId).plusMinutes(HOLD + 1)));
        assertEquals("CANCELLED", order(c, orderId).get("installation").get("status").asText(),
                "hủy đơn thì trả luôn chỗ trống của ngày hẹn");
    }

    @Test
    void oneSweepCancelsEveryOverdueOrder() throws Exception {
        Customer a = register();
        Customer b = register();
        String first = vnpayPartOrder(a);
        String second = vnpayPartOrder(b);
        LocalDateTime now = orderDate(second).plusMinutes(HOLD + 1);

        assertTrue(job.runOnce(now) >= 2, "một lượt quét hủy hết các đơn quá hạn");
        assertEquals("CANCELLED", order(a, first).get("status").asText());
        assertEquals("CANCELLED", order(b, second).get("status").asText());
    }

    @Test
    void paymentArrivingWhileTheOrderExpiresLeavesAConsistentState() throws Exception {
        for (int round = 0; round < 8; round++) {
            Customer c = register();
            Motorbike bike = newBike();
            String orderId = place(c, bike.getProductId(), "VNPAY", false);
            String txnRef = txnRefOf(c, orderId);
            Map<String, String> success = callback(txnRef, "15000000", "00");
            LocalDateTime late = LocalDateTime.now().plusMinutes(HOLD + 30);   // đơn và liên kết đều đã quá hạn

            ExecutorService pool = Executors.newFixedThreadPool(2);
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            Future<Boolean> expiry = pool.submit(() -> {
                ready.countDown();
                go.await();
                return service.expireIfStillUnpaid(orderId, late);
            });
            Future<Integer> payment = pool.submit(() -> {
                ready.countDown();
                go.await();
                return paymentReturn(success).getResponse().getStatus();
            });
            ready.await();
            go.countDown();
            boolean cancelled = expiry.get();
            assertEquals(200, payment.get());
            pool.shutdown();

            JsonNode o = order(c, orderId);
            assertEquals("PAID", o.get("payment").get("status").asText(), "tiền VNPay đã nhận thì phải được ghi nhận");
            if (cancelled) {
                // hủy trước, tiền về sau: cửa hàng phải thấy đơn hủy đã nhận tiền để hoàn
                assertEquals("CANCELLED", o.get("status").asText());
                assertEquals("AVAILABLE", productStatus(bike.getProductId()));
                assertEquals(1, notificationsContaining(c, "hoàn tiền"), "báo khách sẽ được hoàn tiền");
            } else {
                // trả tiền trước: đơn không bị hủy, xe vẫn được giữ cho khách
                assertEquals("PENDING", o.get("status").asText());
                assertEquals("SOLD", productStatus(bike.getProductId()));
            }
        }
    }

    @Test
    void policyTellsReactHowLongTheCustomerHasToPay() throws Exception {
        Customer c = register();
        JsonNode p = read(mvc.perform(auth(get("/api/orders/policy"), c.token())).andReturn());
        assertEquals(HOLD, p.get("vnpayHoldMinutes").asInt());
    }

    @Test
    void theSweepIsReallyRegisteredWithTheScheduler() {
        boolean registered = scheduler.getScheduledTasks().stream()
                .anyMatch(t -> t.toString().contains(VnpayOrderExpiryJob.class.getSimpleName()));
        assertTrue(registered, "tác vụ chạy nền phải được đăng ký: thiếu @EnableScheduling thì đơn không bao giờ tự hủy");
    }
}
