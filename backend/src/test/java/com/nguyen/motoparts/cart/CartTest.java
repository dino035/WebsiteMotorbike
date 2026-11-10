package com.nguyen.motoparts.cart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.cart.entity.CartItem;
import com.nguyen.motoparts.cart.repository.CartItemRepository;
import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.catalog.repository.ProductRepository;
import com.nguyen.motoparts.common.enums.ProductStatus;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Kiểm thử giỏ hàng và tạm giữ xe trên toàn bộ ứng dụng + PostgreSQL thật.
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
class CartTest {

    private static final String PASSWORD = "Passw0rd!x";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProductRepository productRepository;
    @Autowired CartItemRepository cartItemRepository;

    // ---------------------------------------------------------------- helpers

    private String register() throws Exception {
        String email = "cart-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD)))).andReturn();
        assertEquals(201, r.getResponse().getStatus());
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private Motorbike newBike(String price, ProductStatus status) {
        Motorbike m = new Motorbike();
        m.setName("Honda Test " + UUID.randomUUID().toString().substring(0, 6));
        m.setPrice(new BigDecimal(price));
        m.setStatus(status);
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

    private MvcResult add(String token, String productId, Integer quantity) throws Exception {
        Map<String, Object> body = quantity == null ? Map.of("productId", productId)
                : Map.of("productId", productId, "quantity", quantity);
        return mvc.perform(auth(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), token)).andReturn();
    }

    private JsonNode cart(String token) throws Exception {
        MvcResult r = mvc.perform(auth(get("/api/cart"), token)).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        return read(r);
    }

    private JsonNode read(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String message(MvcResult r) throws Exception {
        return read(r).get("message").asText();
    }

    private void backdateHold(String productId, int minutesAgo) {
        for (CartItem item : cartItemRepository.findAll()) {
            if (productId.equals(item.getProductId())) {
                item.setAddedAt(LocalDateTime.now().minusMinutes(minutesAgo));
                cartItemRepository.save(item);
            }
        }
    }

    // ------------------------------------------------------------------ tests

    @Test
    void cartNeedsLogin() throws Exception {
        assertEquals(401, mvc.perform(get("/api/cart")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"x\"}")).andReturn().getResponse().getStatus());
    }

    @Test
    void emptyCartIsReturnedForNewUser() throws Exception {
        JsonNode c = cart(register());
        assertEquals(0, c.get("lines").size());
        assertEquals(0, c.get("total").decimalValue().compareTo(BigDecimal.ZERO));
        assertEquals(30, c.get("holdMinutes").asInt());
    }

    @Test
    void sparePartsAccumulateQuantityAndTotalsAreComputed() throws Exception {
        String token = register();
        SparePart p1 = newPart("150000");
        SparePart p2 = newPart("200000");

        assertEquals(200, add(token, p1.getProductId(), 2).getResponse().getStatus());
        JsonNode c = read(add(token, p1.getProductId(), 3));
        assertEquals(1, c.get("lines").size());
        assertEquals(5, c.get("lines").get(0).get("quantity").asInt(), "thêm lại thì cộng dồn");

        c = read(add(token, p2.getProductId(), null));   // bỏ trống số lượng = 1
        assertEquals(2, c.get("lines").size());
        assertEquals(0, new BigDecimal("950000").compareTo(c.get("total").decimalValue()));   // 5*150000 + 200000
        assertEquals(6, c.get("itemCount").asInt());
        assertFalse(c.get("lines").get(0).get("motorbike").asBoolean());
        assertTrue(c.get("lines").get(0).get("holdExpiresAt").isNull(), "phụ tùng không có thời hạn giữ");
    }

    @Test
    void quantityIsValidated() throws Exception {
        String token = register();
        SparePart p = newPart("1000");
        assertEquals(400, add(token, p.getProductId(), 0).getResponse().getStatus());
        assertEquals(400, add(token, p.getProductId(), 100).getResponse().getStatus());
        assertEquals(400, mvc.perform(auth(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"), token)).andReturn().getResponse().getStatus(), "thiếu mã sản phẩm");

        assertEquals(200, add(token, p.getProductId(), 99).getResponse().getStatus());
        MvcResult over = add(token, p.getProductId(), 1);
        assertEquals(400, over.getResponse().getStatus(), "cộng dồn vượt 99");
        assertEquals(99, cart(token).get("lines").get(0).get("quantity").asInt());
    }

    @Test
    void unknownProductIsNotFound() throws Exception {
        assertEquals(404, add(register(), "khong-co-san-pham-nay", 1).getResponse().getStatus());
    }

    @Test
    void soldProductCannotBeAdded() throws Exception {
        String token = register();
        Motorbike sold = newBike("30000000", ProductStatus.SOLD);
        MvcResult r = add(token, sold.getProductId(), 1);
        assertEquals(409, r.getResponse().getStatus());
        assertTrue(message(r).contains("hết hàng"));
    }

    @Test
    void motorbikeIsAlwaysQuantityOneAndReAddingExtendsTheHold() throws Exception {
        String token = register();
        Motorbike bike = newBike("35000000", ProductStatus.AVAILABLE);

        JsonNode c = read(add(token, bike.getProductId(), 5));   // gửi 5 vẫn là 1
        assertEquals(1, c.get("lines").get(0).get("quantity").asInt());
        assertTrue(c.get("lines").get(0).get("motorbike").asBoolean());
        assertNotNull(c.get("lines").get(0).get("holdExpiresAt").asText());

        backdateHold(bike.getProductId(), 20);
        add(token, bike.getProductId(), 1);   // thêm lại: gia hạn, không thêm dòng mới
        c = cart(token);
        assertEquals(1, c.get("lines").size());
        LocalDateTime expires = LocalDateTime.parse(c.get("lines").get(0).get("holdExpiresAt").asText());
        assertTrue(expires.isAfter(LocalDateTime.now().plusMinutes(25)), "hạn giữ được tính lại từ bây giờ");
    }

    @Test
    void anotherCustomerCannotAddAHeldMotorbike() throws Exception {
        String a = register();
        String b = register();
        Motorbike bike = newBike("35000000", ProductStatus.AVAILABLE);

        assertEquals(200, add(a, bike.getProductId(), 1).getResponse().getStatus());
        MvcResult r = add(b, bike.getProductId(), 1);
        assertEquals(409, r.getResponse().getStatus());
        assertTrue(message(r).contains("khách khác giữ"));
        assertEquals(0, cart(b).get("lines").size());
    }

    @Test
    void expiredHoldCanBeTakenOverAndTheOldLineIsDropped() throws Exception {
        String a = register();
        String b = register();
        Motorbike bike = newBike("35000000", ProductStatus.AVAILABLE);

        add(a, bike.getProductId(), 1);
        backdateHold(bike.getProductId(), 40);   // hạn giữ 30 phút đã qua

        assertEquals(200, add(b, bike.getProductId(), 1).getResponse().getStatus(), "hết hạn thì người khác giữ được");

        JsonNode ca = cart(a);
        assertEquals(0, ca.get("lines").size(), "dòng của người hết hạn bị gỡ");
        assertEquals(1, ca.get("notices").size());
        assertTrue(ca.get("notices").get(0).asText().contains("không còn khả dụng"));
        assertEquals(1, cart(b).get("lines").size());
    }

    @Test
    void expiredHoldIsKeptWhileNobodyElseHoldsTheBike() throws Exception {
        String a = register();
        Motorbike bike = newBike("35000000", ProductStatus.AVAILABLE);
        add(a, bike.getProductId(), 1);
        backdateHold(bike.getProductId(), 40);

        JsonNode c = cart(a);
        assertEquals(1, c.get("lines").size(), "chưa ai giữ thì vẫn là của giỏ này");
        assertEquals(0, c.get("notices").size());
    }

    @Test
    void lineIsDroppedWithNoticeWhenProductBecomesSold() throws Exception {
        String token = register();
        SparePart part = newPart("50000");
        add(token, part.getProductId(), 1);

        Product p = productRepository.findById(part.getProductId()).orElseThrow();
        p.setStatus(ProductStatus.SOLD);
        productRepository.save(p);

        JsonNode c = cart(token);
        assertEquals(0, c.get("lines").size());
        assertEquals(1, c.get("notices").size());
        assertEquals(0, cart(token).get("notices").size(), "thông báo chỉ hiện một lần");
    }

    @Test
    void updateQuantityWorksForPartsAndIsRefusedForMotorbikes() throws Exception {
        String token = register();
        SparePart part = newPart("10000");
        Motorbike bike = newBike("20000000", ProductStatus.AVAILABLE);
        add(token, part.getProductId(), 1);
        add(token, bike.getProductId(), 1);
        JsonNode lines = cart(token).get("lines");
        String partLine = null, bikeLine = null;
        for (JsonNode l : lines) {
            if (l.get("motorbike").asBoolean()) {
                bikeLine = l.get("cartItemId").asText();
            } else {
                partLine = l.get("cartItemId").asText();
            }
        }

        MvcResult ok = mvc.perform(auth(put("/api/cart/items/" + partLine).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":7}"), token)).andReturn();
        assertEquals(200, ok.getResponse().getStatus());
        assertEquals(0, new BigDecimal("20070000").compareTo(read(ok).get("total").decimalValue()));

        MvcResult bikeRes = mvc.perform(auth(put("/api/cart/items/" + bikeLine).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":2}"), token)).andReturn();
        assertEquals(400, bikeRes.getResponse().getStatus());
        assertTrue(message(bikeRes).contains("chỉ mua được 1 chiếc"));

        for (String bad : List.of("0", "100", "null")) {
            assertEquals(400, mvc.perform(auth(put("/api/cart/items/" + partLine).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantity\":" + bad + "}"), token)).andReturn().getResponse().getStatus());
        }
    }

    @Test
    void removeDeletesTheLineAndUsersCannotTouchEachOthersLines() throws Exception {
        String owner = register();
        String other = register();
        SparePart part = newPart("10000");
        add(owner, part.getProductId(), 2);
        String lineId = cart(owner).get("lines").get(0).get("cartItemId").asText();

        assertEquals(404, mvc.perform(auth(put("/api/cart/items/" + lineId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":9}"), other)).andReturn().getResponse().getStatus());
        assertEquals(404, mvc.perform(auth(delete("/api/cart/items/" + lineId), other))
                .andReturn().getResponse().getStatus());
        assertEquals(2, cart(owner).get("lines").get(0).get("quantity").asInt(), "người khác không sửa được");

        MvcResult del = mvc.perform(auth(delete("/api/cart/items/" + lineId), owner)).andReturn();
        assertEquals(200, del.getResponse().getStatus());
        assertEquals(0, read(del).get("lines").size());
        assertEquals(404, mvc.perform(auth(delete("/api/cart/items/" + lineId), owner))
                .andReturn().getResponse().getStatus(), "xóa lần hai thì không còn");
    }

    @Test
    void whenTwoCustomersGrabTheSameMotorbikeAtOnceExactlyOneWins() throws Exception {
        String a = register();
        String b = register();
        Motorbike bike = newBike("35000000", ProductStatus.AVAILABLE);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> ta = () -> race(a, bike.getProductId(), ready, go);
            Callable<Integer> tb = () -> race(b, bike.getProductId(), ready, go);
            Future<Integer> fa = pool.submit(ta);
            Future<Integer> fb = pool.submit(tb);
            ready.await();
            go.countDown();
            int sa = fa.get();
            int sb = fb.get();
            assertEquals(1, (sa == 200 ? 1 : 0) + (sb == 200 ? 1 : 0), "chỉ một người giữ được xe: " + sa + " và " + sb);
            assertEquals(409, sa == 200 ? sb : sa);
        } finally {
            pool.shutdownNow();
        }
        long holders = cartItemRepository.findAll().stream().filter(i -> bike.getProductId().equals(i.getProductId())).count();
        assertEquals(1, holders);
    }

    private int race(String token, String productId, CountDownLatch ready, CountDownLatch go) throws Exception {
        ready.countDown();
        go.await();
        return add(token, productId, 1).getResponse().getStatus();
    }
}
