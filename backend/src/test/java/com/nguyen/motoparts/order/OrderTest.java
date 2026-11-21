package com.nguyen.motoparts.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.aftersales.repository.NotificationRepository;
import com.nguyen.motoparts.cart.entity.CartItem;
import com.nguyen.motoparts.cart.repository.CartItemRepository;
import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.entity.Product;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Kiểm thử đặt hàng (COD, giao tận nơi, nhận tại cửa hàng, lắp đặt) trên toàn bộ ứng dụng + PostgreSQL thật.
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
                // test này cần VNPay CHƯA cấu hình, kể cả khi máy đã đặt biến môi trường VNPAY_* thật
                "app.vnpay.tmn-code=",
                "app.vnpay.hash-secret="
        })
@AutoConfigureMockMvc
class OrderTest {

    private static final String PASSWORD = "Passw0rd!x";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProductRepository productRepository;
    @Autowired CartItemRepository cartItemRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired InstallationRequestRepository installationRepository;

    // ---------------------------------------------------------------- helpers

    private record Customer(String accountId, String token) {
    }

    private Customer register() throws Exception {
        String email = "order-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD)))).andReturn();
        assertEquals(201, r.getResponse().getStatus());
        JsonNode body = read(r);
        return new Customer(body.get("user").get("accountId").asText(), body.get("token").asText());
    }

    private Motorbike newBike(String price) {
        Motorbike m = new Motorbike();
        m.setName("Honda Test " + UUID.randomUUID().toString().substring(0, 6));
        m.setPrice(new BigDecimal(price));
        m.setStatus(ProductStatus.AVAILABLE);
        m.setBrand("Honda");
        m.setModel("Test");
        return productRepository.save(m);
    }

    private SparePart newPart(String price) {
        SparePart p = new SparePart();
        p.setName("Phu tung " + UUID.randomUUID().toString().substring(0, 6));
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

    private MvcResult addToCart(Customer c, String productId, Integer quantity) throws Exception {
        Map<String, Object> body = quantity == null ? Map.of("productId", productId)
                : Map.of("productId", productId, "quantity", quantity);
        MvcResult r = mvc.perform(auth(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), c.token())).andReturn();
        assertEquals(200, r.getResponse().getStatus(), "thêm vào giỏ phải thành công");
        return r;
    }

    private JsonNode cart(Customer c) throws Exception {
        return read(mvc.perform(auth(get("/api/cart"), c.token())).andReturn());
    }

    /** Thân yêu cầu đặt hàng hợp lệ (COD, giao tận nơi); từng test sửa bớt trường để thử lỗi. */
    private Map<String, Object> checkout() {
        Map<String, Object> body = new HashMap<>();
        body.put("deliveryMethod", "DELIVERY");
        body.put("receiverName", "Nguyen Van A");
        body.put("receiverPhone", "0912 345 678");
        body.put("shippingAddress", "12 Le Loi, Quan 1, TP HCM");
        body.put("paymentMethod", "COD");
        body.put("returnPolicyAccepted", true);
        return body;
    }

    private MvcResult placeOrder(Customer c, Map<String, Object> body) throws Exception {
        return mvc.perform(auth(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), c.token())).andReturn();
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

    // ------------------------------------------------------------------ tests

    @Test
    void ordersNeedLogin() throws Exception {
        assertEquals(401, mvc.perform(get("/api/orders")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus());
    }

    @Test
    void policyIsReturned() throws Exception {
        Customer c = register();
        JsonNode p = read(mvc.perform(auth(get("/api/orders/policy"), c.token())).andReturn());
        assertEquals(0, new BigDecimal("50000").compareTo(p.get("laborFee").decimalValue()));
        assertEquals(5, p.get("installMaxPerDay").asInt());
        assertEquals(60, p.get("installMaxDaysAhead").asInt());
    }

    @Test
    void vnpayIsLockedWhenTheMerchantKeysAreNotConfigured() throws Exception {
        // Context này không đặt app.vnpay.tmn-code / hash-secret: VNPay phải bị khóa ở mọi cửa, chỉ còn COD.
        Customer c = register();
        JsonNode p = read(mvc.perform(auth(get("/api/orders/policy"), c.token())).andReturn());
        assertFalse(p.get("vnpayEnabled").asBoolean());

        addToCart(c, newPart("100000").getProductId(), 1);
        String orderId = read(placeOrder(c, checkout())).get("orderId").asText();
        MvcResult r = mvc.perform(auth(post("/api/orders/" + orderId + "/vnpay"), c.token())).andReturn();
        assertEquals(400, r.getResponse().getStatus());
        assertTrue(message(r).contains("VNPay"));

        // Dù ai đó tự ký bằng khóa rỗng, thông báo VNPay cũng không được chấp nhận.
        assertEquals(400, mvc.perform(get("/api/payments/vnpay/return").param("vnp_TxnRef", "x")
                .param("vnp_TmnCode", "").param("vnp_SecureHash", "abc")).andReturn().getResponse().getStatus());
        MvcResult ipn = mvc.perform(get("/api/payments/vnpay/ipn").param("vnp_TxnRef", "x")).andReturn();
        assertEquals(200, ipn.getResponse().getStatus());
        assertEquals("99", read(ipn).get("RspCode").asText());
    }

    @Test
    void emptyCartCannotBeOrdered() throws Exception {
        Customer c = register();
        MvcResult r = placeOrder(c, checkout());
        assertEquals(400, r.getResponse().getStatus());
        assertTrue(message(r).contains("trống"));
    }

    @Test
    void codDeliveryOrderIsCreatedFromTheCart() throws Exception {
        Customer c = register();
        Motorbike bike = newBike("14500000");
        SparePart part = newPart("120000");
        addToCart(c, bike.getProductId(), null);
        addToCart(c, part.getProductId(), 2);

        MvcResult r = placeOrder(c, checkout());
        assertEquals(201, r.getResponse().getStatus());
        JsonNode order = read(r);

        assertEquals("PENDING", order.get("status").asText());
        assertEquals("DELIVERY", order.get("deliveryMethod").asText());
        assertEquals("0912345678", order.get("receiverPhone").asText(), "số điện thoại được chuẩn hóa");
        assertEquals("12 Le Loi, Quan 1, TP HCM", order.get("shippingAddress").asText());
        assertEquals(0, new BigDecimal("14740000").compareTo(order.get("totalAmount").decimalValue()));
        assertEquals(2, order.get("items").size());
        assertEquals("COD", order.get("payment").get("method").asText());
        assertEquals("PENDING", order.get("payment").get("status").asText());
        assertTrue(order.get("installation").isNull());
        assertEquals(8, order.get("shortCode").asText().length());

        // xe chuyển sang SOLD, phụ tùng giữ nguyên; giỏ đã trống; có thông báo
        assertEquals(ProductStatus.SOLD, productRepository.findById(bike.getProductId()).orElseThrow().getStatus());
        assertEquals(ProductStatus.AVAILABLE, productRepository.findById(part.getProductId()).orElseThrow().getStatus());
        assertEquals(0, cart(c).get("lines").size());
        assertTrue(notificationRepository.findAll().stream()
                .anyMatch(n -> c.accountId().equals(n.getAccountId()) && n.getContent().contains(order.get("shortCode").asText())));
    }

    @Test
    void invalidCheckoutDataIsRefusedAndNothingIsConsumed() throws Exception {
        Customer c = register();
        Motorbike bike = newBike("9000000");
        addToCart(c, bike.getProductId(), null);

        Map<String, Object> noPolicy = checkout();
        noPolicy.put("returnPolicyAccepted", false);
        assertEquals(400, placeOrder(c, noPolicy).getResponse().getStatus());

        Map<String, Object> noAddress = checkout();
        noAddress.put("shippingAddress", "   ");
        MvcResult r = placeOrder(c, noAddress);
        assertEquals(400, r.getResponse().getStatus());
        assertTrue(message(r).contains("địa chỉ"));

        Map<String, Object> badPhone = checkout();
        badPhone.put("receiverPhone", "123");
        assertEquals(400, placeOrder(c, badPhone).getResponse().getStatus());

        Map<String, Object> noDelivery = checkout();
        noDelivery.remove("deliveryMethod");
        assertEquals(400, placeOrder(c, noDelivery).getResponse().getStatus());

        Map<String, Object> badEnum = checkout();
        badEnum.put("deliveryMethod", "DRONE");
        assertEquals(400, placeOrder(c, badEnum).getResponse().getStatus());

        Map<String, Object> vnpay = checkout();
        vnpay.put("paymentMethod", "VNPAY");
        r = placeOrder(c, vnpay);
        assertEquals(400, r.getResponse().getStatus());
        assertTrue(message(r).contains("VNPay"));

        // chưa có đơn nào, xe vẫn bán được, giỏ vẫn còn
        assertEquals(ProductStatus.AVAILABLE, productRepository.findById(bike.getProductId()).orElseThrow().getStatus());
        assertEquals(1, cart(c).get("lines").size());
        assertEquals(0, read(mvc.perform(auth(get("/api/orders"), c.token())).andReturn()).size());
    }

    @Test
    void pickupOrderHasNoShippingAddress() throws Exception {
        Customer c = register();
        addToCart(c, newPart("50000").getProductId(), 1);
        Map<String, Object> body = checkout();
        body.put("deliveryMethod", "PICKUP");
        body.put("shippingAddress", "dia chi nay phai bi bo qua");
        MvcResult r = placeOrder(c, body);
        assertEquals(201, r.getResponse().getStatus());
        assertNull(read(r).get("shippingAddress").textValue());
    }

    @Test
    void orderKeepsASnapshotOfPriceAndNameWhenTheProductChangesLater() throws Exception {
        Customer c = register();
        SparePart part = newPart("100000");
        String originalName = part.getName();
        addToCart(c, part.getProductId(), 3);
        String orderId = read(placeOrder(c, checkout())).get("orderId").asText();

        part.setPrice(new BigDecimal("999999"));
        part.setName("Ten moi");
        productRepository.save(part);

        JsonNode detail = read(mvc.perform(auth(get("/api/orders/" + orderId), c.token())).andReturn());
        JsonNode line = detail.get("items").get(0);
        assertEquals(originalName, line.get("productName").asText());
        assertEquals(0, new BigDecimal("100000").compareTo(line.get("unitPrice").decimalValue()));
        assertEquals(0, new BigDecimal("300000").compareTo(detail.get("totalAmount").decimalValue()));
    }

    @Test
    void installationAddsLaborFeeAndNeedsASparePartAndAValidDate() throws Exception {
        Customer c = register();
        addToCart(c, newBike("8000000").getProductId(), null);

        Map<String, Object> body = checkout();
        body.put("deliveryMethod", "INSTALL");
        body.put("shippingAddress", null);
        body.put("installDate", randomFutureDate().toString());
        MvcResult r = placeOrder(c, body);
        assertEquals(400, r.getResponse().getStatus(), "đơn chỉ có xe không lắp đặt được");
        assertTrue(message(r).contains("phụ tùng"));

        addToCart(c, newPart("200000").getProductId(), 1);
        body.put("installDate", LocalDate.now().toString());
        assertEquals(400, placeOrder(c, body).getResponse().getStatus(), "hôm nay không được");
        body.put("installDate", LocalDate.now().plusDays(61).toString());
        assertEquals(400, placeOrder(c, body).getResponse().getStatus(), "quá 60 ngày");
        body.remove("installDate");
        assertEquals(400, placeOrder(c, body).getResponse().getStatus(), "thiếu ngày");

        LocalDate date = randomFutureDate();
        body.put("installDate", date.toString());
        r = placeOrder(c, body);
        if (r.getResponse().getStatus() == 409) {   // ngày ngẫu nhiên trùng kín lịch từ lần chạy trước: thử ngày khác
            date = randomFutureDate().plusDays(0);
            body.put("installDate", date.toString());
            r = placeOrder(c, body);
        }
        assertEquals(201, r.getResponse().getStatus());
        JsonNode order = read(r);
        // xe 8.000.000 + phụ tùng 200.000 + phí công 50.000
        assertEquals(0, new BigDecimal("8250000").compareTo(order.get("totalAmount").decimalValue()));
        assertEquals(date.toString(), order.get("installation").get("preferredDate").asText());
        assertEquals("SCHEDULED", order.get("installation").get("status").asText());
        assertEquals(0, new BigDecimal("50000").compareTo(order.get("installation").get("laborFee").decimalValue()));
        assertNull(order.get("shippingAddress").textValue());
        assertEquals(0, new BigDecimal("8250000").compareTo(order.get("payment").get("amount").decimalValue()));
    }

    @Test
    void aFullyBookedDayRefusesFurtherInstallations() throws Exception {
        LocalDate date = randomFutureDate();
        int accepted = 0;
        boolean refused = false;
        for (int i = 0; i < 6 && !refused; i++) {
            Customer c = register();
            addToCart(c, newPart("10000").getProductId(), 1);
            Map<String, Object> body = checkout();
            body.put("deliveryMethod", "INSTALL");
            body.put("shippingAddress", null);
            body.put("installDate", date.toString());
            MvcResult r = placeOrder(c, body);
            if (r.getResponse().getStatus() == 201) {
                accepted++;
            } else {
                assertEquals(409, r.getResponse().getStatus());
                assertTrue(message(r).contains("kín lịch"));
                refused = true;
                assertEquals(1, cart(c).get("lines").size(), "đơn bị từ chối thì giỏ còn nguyên");
            }
        }
        assertTrue(refused, "tối đa 5 lượt mỗi ngày nên lượt thứ 6 phải bị từ chối");
        assertTrue(accepted <= 5);
        assertEquals(5, installationRepository.countByPreferredDateAndStatusNot(date, InstallationStatus.CANCELLED));
    }

    @Test
    void productThatIsNoLongerAvailableBlocksTheWholeOrder() throws Exception {
        Customer c = register();
        SparePart part = newPart("30000");
        Motorbike bike = newBike("7000000");
        addToCart(c, part.getProductId(), 1);
        addToCart(c, bike.getProductId(), null);

        // xe vừa bị bán cho người khác
        bike.setStatus(ProductStatus.SOLD);
        productRepository.save(bike);

        MvcResult r = placeOrder(c, checkout());
        assertEquals(409, r.getResponse().getStatus());
        assertTrue(message(r).contains("không còn khả dụng"));
        // không có gì bị tiêu thụ: giỏ còn đủ 2 dòng, không có đơn
        assertEquals(2, cartItemRepository.findByCart_AccountIdOrderByAddedAtAsc(c.accountId()).size());
        assertEquals(0, read(mvc.perform(auth(get("/api/orders"), c.token())).andReturn()).size());
    }

    @Test
    void expiredHoldLosesToAnotherCustomerWhoNowHoldsTheBike() throws Exception {
        Customer a = register();
        Customer b = register();
        Motorbike bike = newBike("6000000");
        addToCart(a, bike.getProductId(), null);
        for (CartItem item : cartItemRepository.findByCart_AccountIdOrderByAddedAtAsc(a.accountId())) {
            item.setAddedAt(LocalDateTime.now().minusMinutes(45));   // hết hạn tạm giữ
            cartItemRepository.save(item);
        }
        addToCart(b, bike.getProductId(), null);   // b giữ xe sau khi hạn của a đã hết

        MvcResult r = placeOrder(a, checkout());
        assertEquals(409, r.getResponse().getStatus());

        assertEquals(201, placeOrder(b, checkout()).getResponse().getStatus(), "người đang giữ xe đặt được");
        assertEquals(ProductStatus.SOLD, productRepository.findById(bike.getProductId()).orElseThrow().getStatus());
    }

    @Test
    void expiredHoldIsStillValidWhileNobodyElseHoldsTheBike() throws Exception {
        Customer a = register();
        Motorbike bike = newBike("6500000");
        addToCart(a, bike.getProductId(), null);
        for (CartItem item : cartItemRepository.findByCart_AccountIdOrderByAddedAtAsc(a.accountId())) {
            item.setAddedAt(LocalDateTime.now().minusMinutes(45));
            cartItemRepository.save(item);
        }
        assertEquals(201, placeOrder(a, checkout()).getResponse().getStatus());
    }

    @Test
    void orderedMotorbikeCannotBeAddedByAnotherCustomer() throws Exception {
        Customer a = register();
        Customer b = register();
        Motorbike bike = newBike("5000000");
        addToCart(a, bike.getProductId(), null);
        assertEquals(201, placeOrder(a, checkout()).getResponse().getStatus());

        MvcResult r = mvc.perform(auth(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("productId", bike.getProductId()))), b.token())).andReturn();
        assertEquals(409, r.getResponse().getStatus());
    }

    @Test
    void ordersAreListedNewestFirstAndOnlyOwnersCanOpenThem() throws Exception {
        Customer a = register();
        Customer b = register();
        addToCart(a, newPart("11000").getProductId(), 1);
        String first = read(placeOrder(a, checkout())).get("orderId").asText();
        addToCart(a, newPart("22000").getProductId(), 2);
        String second = read(placeOrder(a, checkout())).get("orderId").asText();

        JsonNode list = read(mvc.perform(auth(get("/api/orders"), a.token())).andReturn());
        assertEquals(2, list.size());
        assertEquals(second, list.get(0).get("orderId").asText(), "mới nhất đứng đầu");
        assertEquals(first, list.get(1).get("orderId").asText());
        assertEquals(2, list.get(0).get("itemCount").asInt());
        assertEquals("Chờ xác nhận", list.get(0).get("statusLabel").asText());
        assertEquals("COD", list.get(0).get("paymentMethod").asText());
        assertEquals("PENDING", list.get(0).get("paymentStatus").asText());

        assertEquals(0, read(mvc.perform(auth(get("/api/orders"), b.token())).andReturn()).size());
        assertEquals(404, mvc.perform(auth(get("/api/orders/" + first), b.token())).andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(auth(get("/api/orders/khong-co"), a.token())).andReturn().getResponse().getStatus());
        assertEquals(200, mvc.perform(auth(get("/api/orders/" + first), a.token())).andReturn().getResponse().getStatus());
    }

    @Test
    void twoOrdersFromTheSameCartAtOnceCreateExactlyOneOrder() throws Exception {
        Customer c = register();
        Motorbike bike = newBike("12000000");
        addToCart(c, bike.getProductId(), null);
        addToCart(c, newPart("45000").getProductId(), 1);

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return placeOrder(c, checkout()).getResponse().getStatus();
            }));
        }
        ready.await();
        go.countDown();
        int created = 0;
        int refused = 0;
        for (Future<Integer> f : results) {
            int status = f.get();
            if (status == 201) {
                created++;
            } else {
                assertEquals(400, status, "lượt thứ hai thấy giỏ đã trống");
                refused++;
            }
        }
        pool.shutdown();
        assertEquals(1, created);
        assertEquals(1, refused);
        assertEquals(1, read(mvc.perform(auth(get("/api/orders"), c.token())).andReturn()).size());
    }

    @Test
    void productStatusIsNotChangedForSparePartsAndOrdersDoNotLeakAcrossCustomers() throws Exception {
        Customer a = register();
        Customer b = register();
        SparePart shared = newPart("70000");
        addToCart(a, shared.getProductId(), 2);
        addToCart(b, shared.getProductId(), 3);

        assertEquals(201, placeOrder(a, checkout()).getResponse().getStatus());
        assertEquals(1, cart(b).get("lines").size(), "giỏ của người khác không bị đụng tới");
        assertEquals(201, placeOrder(b, checkout()).getResponse().getStatus());
        assertEquals(ProductStatus.AVAILABLE, productRepository.findById(shared.getProductId()).orElseThrow().getStatus());
        assertNotNull(read(mvc.perform(auth(get("/api/orders"), b.token())).andReturn()).get(0));
        Product p = productRepository.findById(shared.getProductId()).orElseThrow();
        assertEquals(ProductStatus.AVAILABLE, p.getStatus());
    }
}
