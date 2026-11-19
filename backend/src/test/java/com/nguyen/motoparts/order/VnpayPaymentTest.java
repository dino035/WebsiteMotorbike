package com.nguyen.motoparts.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.aftersales.repository.NotificationRepository;
import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.order.repository.OnlinePaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Kiểm thử thanh toán VNPay trên toàn bộ ứng dụng + PostgreSQL thật. Phần "VNPay" trong test tự ký thông báo bằng
 * khóa thử (cài đặt HMAC riêng của test, không dùng code của dự án) giống cách cổng VNPay gọi về.
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
                "app.vnpay.return-url=http://localhost:3000/payment/vnpay-return"
        })
@AutoConfigureMockMvc
class VnpayPaymentTest {

    private static final String PASSWORD = "Passw0rd!x";
    private static final String TMN = "TESTTMN";
    private static final String SECRET = "test-vnpay-secret-0123456789";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProductRepository productRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired OnlinePaymentRepository onlinePaymentRepository;

    // ---------------------------------------------------------------- helpers

    private record Customer(String accountId, String token) {
    }

    private Customer register() throws Exception {
        String email = "vnp-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
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

    private Motorbike newBike(String price) {
        Motorbike m = new Motorbike();
        m.setName("Honda Vnp " + UUID.randomUUID().toString().substring(0, 6));
        m.setPrice(new BigDecimal(price));
        m.setStatus(ProductStatus.AVAILABLE);
        m.setBrand("Honda");
        m.setModel("Test");
        return productRepository.save(m);
    }

    private SparePart newPart(String price) {
        SparePart p = new SparePart();
        p.setName("Phu tung vnp " + UUID.randomUUID().toString().substring(0, 6));
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

    private void addToCart(Customer c, String productId, int quantity) throws Exception {
        MvcResult r = mvc.perform(auth(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("productId", productId, "quantity", quantity))), c.token())).andReturn();
        assertEquals(200, r.getResponse().getStatus());
    }

    private Map<String, Object> checkout(String paymentMethod) {
        Map<String, Object> body = new HashMap<>();
        body.put("deliveryMethod", "DELIVERY");
        body.put("receiverName", "Nguyen Van A");
        body.put("receiverPhone", "0912345678");
        body.put("shippingAddress", "12 Le Loi, Quan 1");
        body.put("paymentMethod", paymentMethod);
        body.put("returnPolicyAccepted", true);
        return body;
    }

    private MvcResult placeOrder(Customer c, Map<String, Object> body) throws Exception {
        return mvc.perform(auth(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), c.token())).andReturn();
    }

    /** Đơn VNPay có một phụ tùng 100.000 ×2 (tổng 200.000); trả orderId. */
    private String vnpayOrder(Customer c) throws Exception {
        addToCart(c, newPart("100000").getProductId(), 2);
        MvcResult r = placeOrder(c, checkout("VNPAY"));
        assertEquals(201, r.getResponse().getStatus(), "đặt hàng VNPay phải thành công");
        return read(r).get("orderId").asText();
    }

    private String codOrder(Customer c) throws Exception {
        addToCart(c, newPart("100000").getProductId(), 1);
        MvcResult r = placeOrder(c, checkout("COD"));
        assertEquals(201, r.getResponse().getStatus());
        return read(r).get("orderId").asText();
    }

    private MvcResult startPayment(Customer c, String orderId) throws Exception {
        return mvc.perform(auth(post("/api/orders/" + orderId + "/vnpay"), c.token())).andReturn();
    }

    /** Bắt đầu một lần trả tiền và trả mã giao dịch (vnp_TxnRef) lấy từ liên kết. */
    private String txnRefOf(Customer c, String orderId) throws Exception {
        MvcResult r = startPayment(c, orderId);
        assertEquals(200, r.getResponse().getStatus(), "tạo liên kết thanh toán phải thành công");
        return linkParams(read(r).get("paymentUrl").asText()).get("vnp_TxnRef");
    }

    private JsonNode order(Customer c, String orderId) throws Exception {
        return read(mvc.perform(auth(get("/api/orders/" + orderId), c.token())).andReturn());
    }

    private JsonNode adminOrder(String admin, String orderId) throws Exception {
        return read(mvc.perform(auth(get("/api/admin/orders/" + orderId), admin)).andReturn());
    }

    private long notificationsContaining(Customer c, String text) {
        return notificationRepository.findAll().stream()
                .filter(n -> c.accountId().equals(n.getAccountId()) && n.getContent().contains(text)).count();
    }

    // ---- phần "phía VNPay" của test: ký và gửi thông báo, độc lập với code của dự án ----

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

    private static String signedData(Map<String, String> params) {
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, String> e : new TreeMap<>(params).entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) {
                continue;
            }
            if (data.length() > 0) {
                data.append('&');
            }
            data.append(e.getKey()).append('=').append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return data.toString();
    }

    private static Map<String, String> linkParams(String url) {
        Map<String, String> map = new HashMap<>();
        for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
            int eq = pair.indexOf('=');
            map.put(pair.substring(0, eq), URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return map;
    }

    /** Thông báo kết quả đã ký, giống cái VNPay gửi về (return và IPN dùng chung dạng tham số). */
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
        return sign(p);
    }

    private static Map<String, String> sign(Map<String, String> p) {
        Map<String, String> signed = new HashMap<>(p);
        signed.remove("vnp_SecureHash");   // ký trên các tham số còn lại, không kèm chữ ký cũ
        signed.put("vnp_SecureHash", hmac(signedData(signed)));
        return signed;
    }

    private MvcResult send(String path, Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder b = get(path);
        params.forEach(b::param);
        return mvc.perform(b).andReturn();   // không kèm token: VNPay và trình duyệt vừa quay về không đăng nhập được
    }

    private JsonNode paymentReturn(Map<String, String> params) throws Exception {
        MvcResult r = send("/api/payments/vnpay/return", params);
        assertEquals(200, r.getResponse().getStatus(), r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return read(r);
    }

    private String ipn(Map<String, String> params) throws Exception {
        MvcResult r = send("/api/payments/vnpay/ipn", params);
        assertEquals(200, r.getResponse().getStatus(), "IPN luôn trả HTTP 200 kèm RspCode");
        return read(r).get("RspCode").asText();
    }

    // ------------------------------------------------------------------ tests

    @Test
    void policyTellsReactThatVnpayIsAvailable() throws Exception {
        Customer c = register();
        JsonNode p = read(mvc.perform(auth(get("/api/orders/policy"), c.token())).andReturn());
        assertTrue(p.get("vnpayEnabled").asBoolean());
    }

    @Test
    void vnpayOrderStartsUnpaidAndCanBePaidOnline() throws Exception {
        Customer c = register();
        String orderId = vnpayOrder(c);
        JsonNode o = order(c, orderId);
        assertEquals("VNPAY", o.get("payment").get("method").asText());
        assertEquals("PENDING", o.get("payment").get("status").asText());
        assertEquals(0, new BigDecimal("200000").compareTo(o.get("payment").get("amount").decimalValue()));
        assertTrue(o.get("payment").get("canPayOnline").asBoolean());

        JsonNode codOrder = order(c, codOrder(c));
        assertFalse(codOrder.get("payment").get("canPayOnline").asBoolean(), "đơn COD không có nút trả tiền online");
    }

    @Test
    void paymentLinkIsSignedAndCarriesTheOrderAmount() throws Exception {
        Customer c = register();
        String orderId = vnpayOrder(c);
        MvcResult r = startPayment(c, orderId);
        assertEquals(200, r.getResponse().getStatus());
        String url = read(r).get("paymentUrl").asText();

        assertTrue(url.startsWith("https://sandbox.example/pay?"));
        Map<String, String> p = linkParams(url);
        assertEquals(TMN, p.get("vnp_TmnCode"));
        assertEquals("20000000", p.get("vnp_Amount"), "200.000đ nhân 100");
        assertEquals(read(r).get("txnRef").asText(), p.get("vnp_TxnRef"));
        assertNotNull(p.get("vnp_IpAddr"));
        Map<String, String> unsigned = new HashMap<>(p);
        String hash = unsigned.remove("vnp_SecureHash");
        assertEquals(hmac(signedData(unsigned)), hash, "liên kết phải ký đúng khóa");

        String second = txnRefOf(c, orderId);
        assertNotEquals(p.get("vnp_TxnRef"), second, "mỗi lần bấm là một mã giao dịch mới");
    }

    @Test
    void onlyTheOwnerOfAVnpayOrderCanStartPayment() throws Exception {
        Customer owner = register();
        Customer other = register();
        String orderId = vnpayOrder(owner);

        assertEquals(401, mvc.perform(post("/api/orders/" + orderId + "/vnpay")).andReturn().getResponse().getStatus());
        assertEquals(404, startPayment(other, orderId).getResponse().getStatus());
        assertEquals(404, startPayment(owner, UUID.randomUUID().toString()).getResponse().getStatus());
        assertEquals(404, mvc.perform(auth(post("/api/orders/" + orderId + "/pay-cod"), other.token()))
                .andReturn().getResponse().getStatus());
        assertEquals(0, onlinePaymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).size(),
                "người khác thử mà không có giao dịch nào được tạo");
    }

    @Test
    void successfulReturnMarksTheOrderPaidAndIsRecordedOnce() throws Exception {
        Customer c = register();
        String admin = adminToken();
        String orderId = vnpayOrder(c);
        String txnRef = txnRefOf(c, orderId);
        String code = order(c, orderId).get("shortCode").asText();

        Map<String, String> cb = callback(txnRef, "200000", "00");
        JsonNode res = paymentReturn(cb);
        assertEquals("PAID", res.get("outcome").asText());
        assertTrue(res.get("paid").asBoolean());
        assertEquals(orderId, res.get("orderId").asText());
        assertEquals(code, res.get("orderCode").asText());
        assertTrue(res.get("message").asText().contains("thành công"));

        JsonNode o = order(c, orderId);
        assertEquals("PAID", o.get("payment").get("status").asText());
        assertEquals("VNPAY", o.get("payment").get("method").asText());
        assertFalse(o.get("payment").get("paidAt").isNull());
        assertFalse(o.get("payment").get("canPayOnline").asBoolean());
        assertEquals(1, notificationsContaining(c, "Thanh toán VNPay thành công cho đơn hàng #" + code));

        // Admin thấy lần trả tiền để đối soát, và không có nút "thu tiền COD" cho đơn VNPay
        JsonNode adminView = adminOrder(admin, orderId);
        assertFalse(adminView.get("canConfirmPayment").asBoolean());
        JsonNode attempt = adminView.get("onlinePayments").get(0);
        assertEquals(txnRef, attempt.get("txnRef").asText());
        assertEquals("PAID", attempt.get("status").asText());
        assertEquals("00", attempt.get("responseCode").asText());
        assertEquals("NCB", attempt.get("bankCode").asText());
        assertFalse(attempt.get("transactionNo").isNull());

        // Trình duyệt tải lại trang kết quả, rồi IPN đến sau: không ghi trùng, vẫn báo đúng là đã thanh toán thành công
        JsonNode again = paymentReturn(cb);
        assertEquals("ALREADY_PROCESSED", again.get("outcome").asText());
        assertTrue(again.get("paid").asBoolean());
        assertTrue(again.get("message").asText().contains("thành công"));
        assertEquals("02", ipn(cb));
        assertEquals(1, notificationsContaining(c, "Thanh toán VNPay thành công cho đơn hàng #" + code));
    }

    @Test
    void ipnRecordsThePaymentEvenIfTheBrowserNeverReturns() throws Exception {
        Customer c = register();
        String orderId = vnpayOrder(c);
        Map<String, String> cb = callback(txnRefOf(c, orderId), "200000", "00");

        assertEquals("00", ipn(cb));
        assertEquals("PAID", order(c, orderId).get("payment").get("status").asText());
        // trình duyệt quay về sau IPN: vẫn thấy kết quả thành công
        JsonNode res = paymentReturn(cb);
        assertEquals("ALREADY_PROCESSED", res.get("outcome").asText());
        assertTrue(res.get("paid").asBoolean());
    }

    @Test
    void tamperedUnknownOrWrongMerchantNotificationsChangeNothing() throws Exception {
        Customer c = register();
        String orderId = vnpayOrder(c);
        String txnRef = txnRefOf(c, orderId);

        // đổi số tiền sau khi ký
        Map<String, String> tampered = callback(txnRef, "200000", "00");
        tampered.put("vnp_Amount", "100");
        assertEquals(400, send("/api/payments/vnpay/return", tampered).getResponse().getStatus());
        assertEquals("97", ipn(tampered));

        // thiếu chữ ký
        Map<String, String> unsigned = callback(txnRef, "200000", "00");
        unsigned.remove("vnp_SecureHash");
        assertEquals(400, send("/api/payments/vnpay/return", unsigned).getResponse().getStatus());
        assertEquals("97", ipn(unsigned));

        // chữ ký đúng khóa nhưng mã website khác
        Map<String, String> other = new HashMap<>(callback(txnRef, "200000", "00"));
        other.put("vnp_TmnCode", "OTHERTMN");
        Map<String, String> otherSigned = sign(other);
        assertEquals(400, send("/api/payments/vnpay/return", otherSigned).getResponse().getStatus());
        assertEquals("97", ipn(otherSigned));

        // mã giao dịch lạ (chữ ký hợp lệ)
        Map<String, String> unknown = callback("khongco" + UUID.randomUUID().toString().replace("-", ""), "200000", "00");
        assertEquals("NOT_FOUND", paymentReturn(unknown).get("outcome").asText());
        assertEquals("01", ipn(unknown));

        assertEquals("PENDING", order(c, orderId).get("payment").get("status").asText(), "đơn vẫn chưa thanh toán");
        assertEquals("PENDING", onlinePaymentRepository.findByTxnRef(txnRef).orElseThrow().getStatus().name());
    }

    @Test
    void amountThatDoesNotMatchIsRefusedEvenWithAValidSignature() throws Exception {
        Customer c = register();
        String orderId = vnpayOrder(c);
        String txnRef = txnRefOf(c, orderId);

        Map<String, String> cheap = callback(txnRef, "1000", "00");   // trả 1.000đ cho đơn 200.000đ
        JsonNode res = paymentReturn(cheap);
        assertEquals("AMOUNT_MISMATCH", res.get("outcome").asText());
        assertFalse(res.get("paid").asBoolean());
        assertEquals("04", ipn(cheap));

        assertEquals("PENDING", order(c, orderId).get("payment").get("status").asText());
        assertEquals("PENDING", onlinePaymentRepository.findByTxnRef(txnRef).orElseThrow().getStatus().name());

        // sau đó thông báo đúng số tiền vẫn ghi nhận bình thường
        assertEquals("PAID", paymentReturn(callback(txnRef, "200000", "00")).get("outcome").asText());
    }

    @Test
    void failedPaymentCanBeRetriedUntilItSucceeds() throws Exception {
        Customer c = register();
        String orderId = vnpayOrder(c);
        String first = txnRefOf(c, orderId);

        JsonNode failed = paymentReturn(callback(first, "200000", "24"));   // khách hủy trên cổng VNPay
        assertEquals("FAILED", failed.get("outcome").asText());
        assertFalse(failed.get("paid").asBoolean());
        assertTrue(failed.get("reason").asText().contains("hủy"));

        JsonNode afterFail = order(c, orderId);
        assertEquals("FAILED", afterFail.get("payment").get("status").asText());
        assertTrue(afterFail.get("payment").get("canPayOnline").asBoolean(), "vẫn được thử lại");

        String second = txnRefOf(c, orderId);
        assertNotEquals(first, second);
        assertEquals("PENDING", order(c, orderId).get("payment").get("status").asText(), "đang thử lại");

        assertEquals("PAID", paymentReturn(callback(second, "200000", "00")).get("outcome").asText());
        assertEquals("PAID", order(c, orderId).get("payment").get("status").asText());
        assertEquals("FAILED", onlinePaymentRepository.findByTxnRef(first).orElseThrow().getStatus().name(),
                "lần thất bại cũ vẫn được giữ lại");
        assertEquals(2, onlinePaymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).size());
    }

    @Test
    void customerCanSwitchAFailedVnpayOrderToCodAndAdminCollectsIt() throws Exception {
        Customer c = register();
        String admin = adminToken();
        String orderId = vnpayOrder(c);
        paymentReturn(callback(txnRefOf(c, orderId), "200000", "24"));

        MvcResult r = mvc.perform(auth(post("/api/orders/" + orderId + "/pay-cod"), c.token())).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        JsonNode payment = read(r).get("payment");
        assertEquals("COD", payment.get("method").asText());
        assertEquals("PENDING", payment.get("status").asText());
        assertFalse(payment.get("canPayOnline").asBoolean());

        assertEquals(409, startPayment(c, orderId).getResponse().getStatus(), "đã là COD thì không trả VNPay nữa");
        assertEquals(409, mvc.perform(auth(post("/api/orders/" + orderId + "/pay-cod"), c.token()))
                .andReturn().getResponse().getStatus(), "đổi sang COD hai lần");

        assertTrue(adminOrder(admin, orderId).get("canConfirmPayment").asBoolean());
        assertEquals(200, mvc.perform(auth(post("/api/admin/orders/" + orderId + "/confirm-payment"), admin))
                .andReturn().getResponse().getStatus());
        assertEquals("PAID", order(c, orderId).get("payment").get("status").asText());
    }

    @Test
    void moneyReceivedAfterSwitchingToCodIsStillRecordedAsVnpay() throws Exception {
        Customer c = register();
        String orderId = vnpayOrder(c);
        String txnRef = txnRefOf(c, orderId);   // khách mở cổng VNPay nhưng chưa trả
        assertEquals(200, mvc.perform(auth(post("/api/orders/" + orderId + "/pay-cod"), c.token()))
                .andReturn().getResponse().getStatus());

        assertEquals("PAID", paymentReturn(callback(txnRef, "200000", "00")).get("outcome").asText());
        JsonNode payment = order(c, orderId).get("payment");
        assertEquals("PAID", payment.get("status").asText());
        assertEquals("VNPAY", payment.get("method").asText(), "khách đã thật sự trả bằng VNPay");
    }

    @Test
    void paidOrCancelledOrNonVnpayOrdersCannotStartPayment() throws Exception {
        Customer c = register();
        String admin = adminToken();

        assertEquals(409, startPayment(c, codOrder(c)).getResponse().getStatus(), "đơn COD");

        String paid = vnpayOrder(c);
        paymentReturn(callback(txnRefOf(c, paid), "200000", "00"));
        assertEquals(409, startPayment(c, paid).getResponse().getStatus(), "đã thanh toán");
        assertEquals(409, mvc.perform(auth(post("/api/orders/" + paid + "/pay-cod"), c.token()))
                .andReturn().getResponse().getStatus(), "đã thanh toán thì không đổi sang COD");

        String cancelled = vnpayOrder(c);
        assertEquals(200, mvc.perform(auth(post("/api/admin/orders/" + cancelled + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"), admin))
                .andReturn().getResponse().getStatus());
        assertEquals(409, startPayment(c, cancelled).getResponse().getStatus(), "đơn đã hủy");
        assertEquals(409, mvc.perform(auth(post("/api/orders/" + cancelled + "/pay-cod"), c.token()))
                .andReturn().getResponse().getStatus());
        assertFalse(order(c, cancelled).get("payment").get("canPayOnline").asBoolean());
    }

    @Test
    void moneyForAnAlreadyCancelledOrderIsRecordedAndFlaggedForRefund() throws Exception {
        Customer c = register();
        String admin = adminToken();
        String orderId = vnpayOrder(c);
        String txnRef = txnRefOf(c, orderId);
        String code = order(c, orderId).get("shortCode").asText();
        assertEquals(200, mvc.perform(auth(post("/api/admin/orders/" + orderId + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"), admin))
                .andReturn().getResponse().getStatus());

        JsonNode res = paymentReturn(callback(txnRef, "200000", "00"));
        assertEquals("PAID_ORDER_CANCELLED", res.get("outcome").asText());
        assertTrue(res.get("paid").asBoolean(), "tiền đã thu thật");
        assertTrue(res.get("message").asText().contains("hoàn tiền"));
        assertEquals("PAID", order(c, orderId).get("payment").get("status").asText());
        assertEquals("CANCELLED", order(c, orderId).get("status").asText(), "đơn vẫn hủy");
        assertEquals(1, notificationsContaining(c, "Cửa hàng đã nhận tiền VNPay cho đơn hàng #" + code));
        assertEquals("PAID", adminOrder(admin, orderId).get("onlinePayments").get(0).get("status").asText(),
                "Admin thấy giao dịch để hoàn tiền");
    }

    @Test
    void aSecondSuccessfulPaymentForThePaidOrderIsFlaggedAsDuplicate() throws Exception {
        Customer c = register();
        String orderId = vnpayOrder(c);
        String first = txnRefOf(c, orderId);
        String second = txnRefOf(c, orderId);   // khách mở hai cổng thanh toán

        assertEquals("PAID", paymentReturn(callback(first, "200000", "00")).get("outcome").asText());
        String paidAt = order(c, orderId).get("payment").get("paidAt").asText();

        JsonNode dup = paymentReturn(callback(second, "200000", "00"));
        assertEquals("PAID_DUPLICATE", dup.get("outcome").asText());
        assertTrue(dup.get("message").asText().contains("hoàn tiền"));
        assertEquals(paidAt, order(c, orderId).get("payment").get("paidAt").asText(), "thời điểm thanh toán không đổi");
    }

    @Test
    void failureOfAnOlderAttemptDoesNotUndoAPaidOrder() throws Exception {
        Customer c = register();
        String orderId = vnpayOrder(c);
        String first = txnRefOf(c, orderId);
        String second = txnRefOf(c, orderId);

        assertEquals("PAID", paymentReturn(callback(second, "200000", "00")).get("outcome").asText());
        assertEquals("FAILED", paymentReturn(callback(first, "200000", "11")).get("outcome").asText());
        assertEquals("PAID", order(c, orderId).get("payment").get("status").asText(),
                "thông báo thất bại muộn của lần cũ không được hạ đơn đã thanh toán");
    }

    @Test
    void returnAndIpnArrivingTogetherRecordThePaymentExactlyOnce() throws Exception {
        Customer c = register();
        String orderId = vnpayOrder(c);
        String code = order(c, orderId).get("shortCode").asText();
        Map<String, String> cb = callback(txnRefOf(c, orderId), "200000", "00");

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            boolean viaIpn = i % 2 == 0;
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                if (viaIpn) {
                    return "ipn:" + ipn(cb);
                }
                return "return:" + paymentReturn(cb).get("outcome").asText();
            }));
        }
        ready.await();
        go.countDown();
        List<String> all = new ArrayList<>();
        for (Future<String> f : results) {
            all.add(f.get());
        }
        pool.shutdown();

        assertEquals(1, all.stream().filter(s -> s.equals("ipn:00") || s.equals("return:PAID")).count(),
                "đúng một lần ghi nhận thành công, các lần còn lại là đã xử lý: " + all);
        assertEquals(threads - 1, all.stream().filter(s -> s.equals("ipn:02") || s.equals("return:ALREADY_PROCESSED")).count(),
                all.toString());
        assertEquals(1, notificationsContaining(c, "Thanh toán VNPay thành công cho đơn hàng #" + code));
        assertEquals("PAID", order(c, orderId).get("payment").get("status").asText());
    }

    @Test
    void vnpayCallbacksAreReachableWithoutLoginButOtherPaymentRoutesAreNot() throws Exception {
        assertEquals(400, mvc.perform(get("/api/payments/vnpay/return")).andReturn().getResponse().getStatus(),
                "không có chữ ký: bị từ chối chứ không phải 401");
        assertEquals(200, mvc.perform(get("/api/payments/vnpay/ipn")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(post("/api/orders/x/vnpay")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(post("/api/orders/x/pay-cod")).andReturn().getResponse().getStatus());
    }
}
