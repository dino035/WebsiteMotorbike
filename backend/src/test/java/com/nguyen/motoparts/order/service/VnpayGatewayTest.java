package com.nguyen.motoparts.order.service;

import com.nguyen.motoparts.order.entity.OnlinePayment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Kiểm thử riêng phần tạo liên kết và kiểm tra chữ ký VNPay (không cần CSDL). */
class VnpayGatewayTest {

    private static final String SECRET = "SECRETKEY123";
    private static final String RETURN_URL = "http://localhost:3000/payment/vnpay-return";

    private static VnpayGateway gateway() {
        return new VnpayGateway("TESTTMN", SECRET, "https://sandbox.example/pay", RETURN_URL, 15);
    }

    private static OnlinePayment attempt(String amount) {
        OnlinePayment a = new OnlinePayment();
        a.setTxnRef("abc123");
        a.setAmount(new BigDecimal(amount));
        return a;
    }

    /** Tách query string của liên kết thanh toán thành bản đồ tham số (đã giải mã). */
    private static Map<String, String> params(String url) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : url.substring(url.indexOf('?') + 1).split("&")) {
            int eq = pair.indexOf('=');
            map.put(pair.substring(0, eq), URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return map;
    }

    @Test
    void signatureMatchesAnIndependentHmacSha512Implementation() {
        // Giá trị tính bằng Python (hmac + hashlib) trên cùng khóa và chuỗi dữ liệu, không dùng code của dự án.
        String data = "vnp_Amount=1450000000&vnp_OrderInfo=Thanh+toan+don+hang+AB12CD34&vnp_TmnCode=TESTTMN&vnp_TxnRef=abc123";
        assertEquals("e21649ac0af7781b2fedaf3f65ab3764ec8b92502adfddd05c3cc83df9329b5b0755f784c2bbfca5c5997cb95d2caf2a5"
                + "28b9ba4962a46e6a2304d25e2811ab6", VnpayGateway.hmacSha512(SECRET, data));
    }

    @Test
    void dataToSignIsSortedByNameSkipsEmptyValuesAndEncodesValues() {
        Map<String, String> p = new HashMap<>();
        p.put("vnp_TxnRef", "abc123");
        p.put("vnp_Amount", "1450000000");
        p.put("vnp_OrderInfo", "Thanh toan don hang AB12CD34");
        p.put("vnp_TmnCode", "TESTTMN");
        p.put("vnp_BankCode", "");
        p.put("vnp_Note", null);
        assertEquals("vnp_Amount=1450000000&vnp_OrderInfo=Thanh+toan+don+hang+AB12CD34&vnp_TmnCode=TESTTMN&vnp_TxnRef=abc123",
                VnpayGateway.hashData(p));
    }

    @Test
    void paymentUrlCarriesTheRightParametersAndAValidSignature() {
        VnpayGateway g = gateway();
        String url = g.buildPaymentUrl(attempt("14500000"), "AB12CD34", "10.0.0.5",
                LocalDateTime.of(2026, 11, 20, 9, 30, 0));

        assertTrue(url.startsWith("https://sandbox.example/pay?"));
        Map<String, String> p = params(url);
        assertEquals("2.1.0", p.get("vnp_Version"));
        assertEquals("pay", p.get("vnp_Command"));
        assertEquals("TESTTMN", p.get("vnp_TmnCode"));
        assertEquals("1450000000", p.get("vnp_Amount"), "số tiền VND nhân 100");
        assertEquals("VND", p.get("vnp_CurrCode"));
        assertEquals("abc123", p.get("vnp_TxnRef"));
        assertEquals("Thanh toan don hang AB12CD34", p.get("vnp_OrderInfo"));
        assertEquals(RETURN_URL, p.get("vnp_ReturnUrl"));
        assertEquals("10.0.0.5", p.get("vnp_IpAddr"));
        assertEquals("20261120093000", p.get("vnp_CreateDate"));
        assertEquals("20261120094500", p.get("vnp_ExpireDate"), "hết hạn sau 15 phút");
        assertTrue(url.contains("&vnp_SecureHash="));
        assertTrue(g.verify(p), "chữ ký của chính liên kết phải kiểm tra được");
    }

    @Test
    void timestampsUseVietnamTimeWhateverTheServerTimeZoneIs() {
        // VNPay yêu cầu GMT+7; máy chủ đặt ở UTC (như trên Render) mà gửi giờ UTC thì giao dịch sẽ lệch 7 tiếng.
        String url = gateway().buildPaymentUrl(attempt("100000"), "AB12CD34", "10.0.0.5");
        LocalDateTime created = LocalDateTime.parse(params(url).get("vnp_CreateDate"),
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        LocalDateTime vietnamNow = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime();
        assertTrue(Math.abs(java.time.Duration.between(created, vietnamNow).toSeconds()) < 60,
                "vnp_CreateDate phải là giờ Việt Nam hiện tại, nhận được " + created + " so với " + vietnamNow);
    }

    @Test
    void verifyAcceptsAGenuineNotificationAndRejectsTamperedOnes() {
        VnpayGateway g = gateway();
        Map<String, String> signed = signedCallback(Map.of("vnp_TmnCode", "TESTTMN", "vnp_TxnRef", "abc123",
                "vnp_Amount", "1450000000", "vnp_ResponseCode", "00"));
        assertTrue(g.verify(signed));

        Map<String, String> upper = new HashMap<>(signed);
        upper.put("vnp_SecureHash", signed.get("vnp_SecureHash").toUpperCase());
        assertTrue(g.verify(upper), "so khớp chữ ký không phân biệt hoa thường");

        Map<String, String> otherAmount = new HashMap<>(signed);
        otherAmount.put("vnp_Amount", "100");
        assertFalse(g.verify(otherAmount), "đổi số tiền thì chữ ký sai");

        Map<String, String> otherResult = new HashMap<>(signed);
        otherResult.put("vnp_ResponseCode", "24");
        assertFalse(g.verify(otherResult));

        Map<String, String> extra = new HashMap<>(signed);
        extra.put("vnp_Extra", "x");
        assertFalse(g.verify(extra), "thêm tham số thì chữ ký sai");

        Map<String, String> noHash = new HashMap<>(signed);
        noHash.remove("vnp_SecureHash");
        assertFalse(g.verify(noHash));
        noHash.put("vnp_SecureHash", "");
        assertFalse(g.verify(noHash));
    }

    @Test
    void verifyRejectsAnotherMerchantEvenWithAValidSignature() {
        // Chữ ký đúng (ký bằng đúng khóa) nhưng mã website không phải của mình.
        Map<String, String> other = signedCallback(Map.of("vnp_TmnCode", "OTHERTMN", "vnp_TxnRef", "abc123",
                "vnp_Amount", "1450000000", "vnp_ResponseCode", "00"));
        assertFalse(gateway().verify(other));
    }

    @Test
    void verifyRejectsASignatureMadeWithAnotherSecret() {
        Map<String, String> p = new HashMap<>(Map.of("vnp_TmnCode", "TESTTMN", "vnp_TxnRef", "abc123"));
        p.put("vnp_SecureHash", VnpayGateway.hmacSha512("WRONG", VnpayGateway.hashData(p)));
        assertFalse(gateway().verify(p));
    }

    @Test
    void gatewayIsOnlyConfiguredWithTmnCodeSecretAndReturnUrl() {
        assertTrue(gateway().isConfigured());
        assertFalse(new VnpayGateway("", SECRET, "", RETURN_URL, 15).isConfigured());
        assertFalse(new VnpayGateway("TESTTMN", "", "", RETURN_URL, 15).isConfigured());
        assertFalse(new VnpayGateway("TESTTMN", SECRET, "", "", 15).isConfigured());
        assertFalse(new VnpayGateway("  ", SECRET, "", RETURN_URL, 15).isConfigured());

        // Chưa cấu hình thì không xác thực bất cứ thứ gì (không được coi chữ ký là hợp lệ chỉ vì thiếu khóa).
        VnpayGateway noSecret = new VnpayGateway("TESTTMN", "", "", RETURN_URL, 15);
        Map<String, String> p = new HashMap<>(Map.of("vnp_TmnCode", "TESTTMN", "vnp_TxnRef", "abc123"));
        p.put("vnp_SecureHash", VnpayGateway.hmacSha512(SECRET, VnpayGateway.hashData(p)));
        assertFalse(noSecret.verify(p));
        assertTrue(gateway().verify(p), "cùng thông báo đó hợp lệ khi đã cấu hình đúng khóa");
    }

    @Test
    void amountsAreRoundedToWholeDongAndMultipliedByOneHundred() {
        assertEquals(1450000000L, VnpayGateway.toVnpAmount(new BigDecimal("14500000")));
        assertEquals(1450000000L, VnpayGateway.toVnpAmount(new BigDecimal("14500000.00")));
        assertEquals(10100L, VnpayGateway.toVnpAmount(new BigDecimal("100.5")), "làm tròn lên từ .5");
        assertEquals(10000L, VnpayGateway.toVnpAmount(new BigDecimal("100.4")));
    }

    @Test
    void responseCodesAreExplainedInVietnamese() {
        assertEquals("Bạn đã hủy giao dịch", VnpayGateway.describeResponse("24"));
        assertEquals("Tài khoản không đủ số dư", VnpayGateway.describeResponse("51"));
        assertTrue(VnpayGateway.describeResponse("99").contains("99"));
        assertEquals("Không rõ lý do", VnpayGateway.describeResponse(null));
    }

    /** Dựng một thông báo giống VNPay gửi về: các tham số vnp_* và chữ ký ký bằng SECRET. */
    private static Map<String, String> signedCallback(Map<String, String> fields) {
        Map<String, String> p = new HashMap<>(fields);
        p.put("vnp_SecureHash", VnpayGateway.hmacSha512(SECRET, VnpayGateway.hashData(p)));
        return p;
    }
}
