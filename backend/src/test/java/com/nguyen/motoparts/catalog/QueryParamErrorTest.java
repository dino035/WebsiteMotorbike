package com.nguyen.motoparts.catalog;

import com.nguyen.motoparts.MotoPartsApplication;
import com.nguyen.motoparts.common.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Query string sai kiểu (?status=ABC, ?page=abc) là lỗi của người gọi: phải trả 400, không phải 500.
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
                "app.jwt.secret=test-secret-test-secret-test-secret-0123456789"
        })
@AutoConfigureMockMvc
class QueryParamErrorTest {

    @Autowired MockMvc mvc;

    private String messageOf(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void invalidEnumStatusIsBadRequestAndListsValidValues() throws Exception {
        MvcResult r = mvc.perform(get("/api/products").param("status", "ABC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("'status'")))
                .andReturn();
        String text = messageOf(r);
        for (ProductStatus s : ProductStatus.values()) {
            assertTrue(text.contains(s.name()), "thông báo phải liệt kê giá trị hợp lệ " + s);
        }
    }

    @Test
    void invalidNumberParametersAreBadRequestToo() throws Exception {
        mvc.perform(get("/api/products").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("'page'")));
        mvc.perform(get("/api/products").param("size", "1.5"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("minPrice", "rẻ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("'minPrice'")));
    }

    @Test
    void messageNeverEchoesTheValueTheCallerSent() throws Exception {
        // React đổ message thẳng vào DOM: không được dội lại dữ liệu từ bên ngoài
        MvcResult r = mvc.perform(get("/api/products").param("status", "<script>alert(1)</script>"))
                .andExpect(status().isBadRequest()).andReturn();
        String text = messageOf(r);
        assertFalse(text.contains("<script>") || text.contains("alert(1)"), "không trả lại nguyên văn giá trị gửi lên");
    }

    @Test
    void validParametersAndOtherSpringErrorsKeepTheirStatus() throws Exception {
        mvc.perform(get("/api/products").param("status", "AVAILABLE").param("page", "0"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/products/compare"))   // thiếu tham số bắt buộc ids: Spring tự phân loại 400
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/khong-co-duong-dan-nay"))
                .andExpect(status().is4xxClientError());
    }
}
