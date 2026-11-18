package com.nguyen.motoparts.advisory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.advisory.llm.LlmClient;
import com.nguyen.motoparts.vehicle.entity.VehicleModel;
import com.nguyen.motoparts.vehicle.repository.VehicleModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Kiểm thử hồ sơ xe của khách (SRS 3.15) trên toàn bộ ứng dụng + PostgreSQL thật: thêm, xem, sửa, xóa,
 * kiểm tra dữ liệu, quyền sở hữu và giới hạn số xe. Chạy khi có TEST_DB_URL (xem AuthSecurityTest).
 * Cấu hình giống ChatTest để dùng chung một ngữ cảnh Spring (chạy nhanh hơn).
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
                "app.chat.max-messages-per-hour=6"
        })
@AutoConfigureMockMvc
class VehicleProfileTest {

    private static final String PASSWORD = "Passw0rd!x";
    private static final String URL = "/api/vehicle-profiles";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired VehicleModelRepository vehicleModelRepository;
    @MockBean LlmClient llm;

    // ---------------------------------------------------------------- helpers

    private String register() throws Exception {
        String email = "xe-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD)))).andReturn();
        assertEquals(201, r.getResponse().getStatus());
        return read(r).get("token").asText();
    }

    private JsonNode read(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    private VehicleModel vehicle(int from, int to) {
        VehicleModel v = new VehicleModel();
        v.setBrand("Hang" + UUID.randomUUID().toString().substring(0, 8));
        v.setModel("Dong " + UUID.randomUUID().toString().substring(0, 4));
        v.setYearFrom(from);
        v.setYearTo(to);
        return vehicleModelRepository.save(v);
    }

    private static Map<String, Object> body(String nickname, String vehicleModelId, Integer year) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (nickname != null) {
            m.put("nickname", nickname);
        }
        if (vehicleModelId != null) {
            m.put("vehicleModelId", vehicleModelId);
        }
        if (year != null) {
            m.put("registrationYear", year);
        }
        return m;
    }

    private MvcResult send(MockHttpServletRequestBuilder b, String token, Object content) throws Exception {
        return mvc.perform(auth(b, token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(content))).andReturn();
    }

    private JsonNode create(String token, String nickname, VehicleModel v, int year) throws Exception {
        MvcResult r = send(post(URL), token, body(nickname, v.getModelId(), year));
        assertEquals(201, r.getResponse().getStatus(), r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return read(r);
    }

    private JsonNode list(String token) throws Exception {
        MvcResult r = mvc.perform(auth(get(URL), token)).andReturn();
        assertEquals(200, r.getResponse().getStatus());
        return read(r);
    }

    private static List<String> ids(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).map(n -> n.get("profileId").asText()).toList();
    }

    // ---------------------------------------------------------------- tests

    @Test
    void requiresLogin() throws Exception {
        assertEquals(401, mvc.perform(get(URL)).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(delete(URL + "/x")).andReturn().getResponse().getStatus());
    }

    @Test
    void createReadUpdateAndDelete() throws Exception {
        String token = register();
        VehicleModel wave = vehicle(2010, 2020);
        VehicleModel exciter = vehicle(2012, 2018);

        JsonNode created = create(token, "  Xe đi làm  ", wave, 2015);
        assertEquals("Xe đi làm", created.get("nickname").asText(), "biệt danh được cắt khoảng trắng hai đầu");
        assertEquals(wave.getModelId(), created.get("vehicleModelId").asText());
        assertEquals(wave.getBrand(), created.get("brand").asText());
        assertEquals(2015, created.get("registrationYear").asInt());
        assertEquals("Xe đi làm (" + wave.getBrand() + " " + wave.getModel() + " 2015)", created.get("label").asText());
        String id = created.get("profileId").asText();

        JsonNode mine = list(token);
        assertEquals(1, mine.size());
        assertEquals(id, mine.get(0).get("profileId").asText());

        MvcResult updated = send(put(URL + "/" + id), token, body("Xe cuối tuần", exciter.getModelId(), 2013));
        assertEquals(200, updated.getResponse().getStatus(), updated.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode u = read(updated);
        assertEquals(id, u.get("profileId").asText());
        assertEquals("Xe cuối tuần", u.get("nickname").asText());
        assertEquals(exciter.getModelId(), u.get("vehicleModelId").asText());
        assertEquals(2013, u.get("registrationYear").asInt());
        assertEquals(exciter.getModel(), list(token).get(0).get("model").asText());

        assertEquals(204, mvc.perform(auth(delete(URL + "/" + id), token)).andReturn().getResponse().getStatus());
        assertEquals(0, list(token).size());
    }

    @Test
    void nicknameIsOptional() throws Exception {
        String token = register();
        VehicleModel v = vehicle(2010, 2020);

        JsonNode noNickname = create(token, null, v, 2016);
        assertTrue(noNickname.get("nickname").isNull());
        assertEquals(v.getBrand() + " " + v.getModel() + " 2016", noNickname.get("label").asText());

        JsonNode blank = create(token, "   ", v, 2016);
        assertTrue(blank.get("nickname").isNull(), "biệt danh toàn khoảng trắng coi như không có");
    }

    @Test
    void rejectsInvalidInput() throws Exception {
        String token = register();
        VehicleModel v = vehicle(2010, 2020);

        assertEquals(400, send(post(URL), token, body("a", null, 2015)).getResponse().getStatus(), "thiếu dòng xe");
        assertEquals(400, send(post(URL), token, body("a", v.getModelId(), null)).getResponse().getStatus(), "thiếu năm");
        assertEquals(400, send(post(URL), token, body("x".repeat(51), v.getModelId(), 2015)).getResponse().getStatus(), "biệt danh quá dài");
        assertEquals(400, send(post(URL), token, body("a", "khong-co-dong-xe-nay", 2015)).getResponse().getStatus(), "dòng xe không tồn tại");

        MvcResult tooOld = send(post(URL), token, body("a", v.getModelId(), 2009));
        assertEquals(400, tooOld.getResponse().getStatus());
        assertTrue(read(tooOld).get("message").asText().contains("2010 - 2020"), "báo rõ khoảng năm sản xuất");
        assertEquals(400, send(post(URL), token, body("a", v.getModelId(), 2021)).getResponse().getStatus());

        assertEquals(0, list(token).size(), "dữ liệu sai không được lưu");
        // hai đầu của khoảng năm hợp lệ
        create(token, null, v, 2010);
        create(token, null, v, 2020);
    }

    @Test
    void updateIsValidatedToo() throws Exception {
        String token = register();
        VehicleModel v = vehicle(2010, 2020);
        String id = create(token, "Xe", v, 2015).get("profileId").asText();

        assertEquals(400, send(put(URL + "/" + id), token, body("Xe", v.getModelId(), 1999)).getResponse().getStatus());
        assertEquals(400, send(put(URL + "/" + id), token, body("Xe", "khong-co", 2015)).getResponse().getStatus());
        assertEquals(2015, list(token).get(0).get("registrationYear").asInt(), "sửa sai không làm đổi hồ sơ");
    }

    @Test
    void otherCustomerCannotSeeChangeOrDelete() throws Exception {
        String owner = register();
        String other = register();
        VehicleModel v = vehicle(2010, 2020);
        String id = create(owner, "Xe của tôi", v, 2015).get("profileId").asText();

        assertEquals(0, list(other).size());
        assertEquals(404, send(put(URL + "/" + id), other, body("Bị đổi", v.getModelId(), 2016)).getResponse().getStatus());
        assertEquals(404, mvc.perform(auth(delete(URL + "/" + id), other)).andReturn().getResponse().getStatus());

        JsonNode still = list(owner).get(0);
        assertEquals("Xe của tôi", still.get("nickname").asText());
        assertEquals(2015, still.get("registrationYear").asInt());
    }

    @Test
    void updateOrDeleteOfMissingProfileIsNotFound() throws Exception {
        String token = register();
        VehicleModel v = vehicle(2010, 2020);
        assertEquals(404, send(put(URL + "/khong-co"), token, body("a", v.getModelId(), 2015)).getResponse().getStatus());
        assertEquals(404, mvc.perform(auth(delete(URL + "/khong-co"), token)).andReturn().getResponse().getStatus());
    }

    @Test
    void listsNewestFirst() throws Exception {
        String token = register();
        VehicleModel v = vehicle(2010, 2020);
        String first = create(token, "Một", v, 2011).get("profileId").asText();
        String second = create(token, "Hai", v, 2012).get("profileId").asText();
        String third = create(token, "Ba", v, 2013).get("profileId").asText();

        assertEquals(List.of(third, second, first), ids(list(token)));
    }

    @Test
    void limitsEachCustomerToTenVehicles() throws Exception {
        String token = register();
        VehicleModel v = vehicle(2010, 2020);
        String firstId = null;
        for (int i = 1; i <= 10; i++) {
            String id = create(token, "Xe " + i, v, 2015).get("profileId").asText();
            if (firstId == null) {
                firstId = id;
            }
        }
        MvcResult eleventh = send(post(URL), token, body("Xe 11", v.getModelId(), 2015));
        assertEquals(409, eleventh.getResponse().getStatus());
        assertEquals(10, list(token).size());

        // khách khác không bị ảnh hưởng, và xóa bớt thì thêm lại được
        create(register(), "Xe", v, 2015);
        assertEquals(204, mvc.perform(auth(delete(URL + "/" + firstId), token)).andReturn().getResponse().getStatus());
        create(token, "Xe 11", v, 2015);
        assertEquals(10, list(token).size());
    }

    @Test
    void addingManyVehiclesAtOnceNeverExceedsTheLimit() throws Exception {
        String token = register();
        VehicleModel v = vehicle(2010, 2020);
        for (int i = 1; i <= 9; i++) {
            create(token, "Xe " + i, v, 2015);
        }

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return send(post(URL), token, body("Cùng lúc", v.getModelId(), 2015)).getResponse().getStatus();
            }));
        }
        ready.await();
        go.countDown();
        int created = 0;
        for (Future<Integer> f : results) {
            int status = f.get();
            if (status == 201) {
                created++;
            } else {
                assertEquals(409, status);
            }
        }
        pool.shutdown();

        assertEquals(1, created, "chỉ còn đúng một chỗ trống nên chỉ một lần thêm thành công");
        assertEquals(10, list(token).size());
    }

    @Test
    void updatingAtTheLimitIsStillAllowed() throws Exception {
        String token = register();
        VehicleModel v = vehicle(2010, 2020);
        String id = null;
        for (int i = 1; i <= 10; i++) {
            id = create(token, "Xe " + i, v, 2015).get("profileId").asText();
        }
        Map<String, Object> change = new HashMap<>(body("Đổi tên", v.getModelId(), 2016));
        assertEquals(200, send(put(URL + "/" + id), token, change).getResponse().getStatus());
    }
}
