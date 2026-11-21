package com.nguyen.motoparts.order.service;

import com.nguyen.motoparts.order.entity.OnlinePayment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Nói chuyện với cổng VNPay Sandbox theo tài liệu tích hợp phiên bản 2.1.0: các tham số vnp_* được sắp theo tên,
 * bỏ tham số rỗng, mã hóa URL giá trị, nối bằng "&" rồi ký HMAC-SHA512 bằng HashSecret. Chữ ký gửi kèm trong vnp_SecureHash.
 *
 * Mã website (TmnCode) và HashSecret chỉ đọc từ biến môi trường VNPAY_TMN_CODE, VNPAY_HASH_SECRET (không ghi vào code
 * hay Git). Thiếu một trong hai thì VNPay được xem là CHƯA CẤU HÌNH: đặt hàng chỉ cho COD.
 */
@Component
public class VnpayGateway {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /** VNPay bắt buộc giờ Việt Nam (GMT+7) cho vnp_CreateDate và vnp_ExpireDate, bất kể máy chủ đặt ở múi giờ nào. */
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String returnUrl;
    private final int expireMinutes;

    public VnpayGateway(@Value("${app.vnpay.tmn-code:}") String tmnCode,
                        @Value("${app.vnpay.hash-secret:}") String hashSecret,
                        @Value("${app.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String payUrl,
                        @Value("${app.vnpay.return-url:}") String returnUrl,
                        @Value("${app.vnpay.expire-minutes:15}") int expireMinutes) {
        this.tmnCode = tmnCode == null ? "" : tmnCode.trim();
        this.hashSecret = hashSecret == null ? "" : hashSecret.trim();
        this.payUrl = payUrl == null || payUrl.isBlank() ? "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html" : payUrl.trim();
        this.returnUrl = returnUrl == null ? "" : returnUrl.trim();
        this.expireMinutes = expireMinutes >= 5 && expireMinutes <= 60 ? expireMinutes : 15;
    }

    public boolean isConfigured() {
        return !tmnCode.isEmpty() && !hashSecret.isEmpty() && !returnUrl.isEmpty();
    }

    public String tmnCode() {
        return tmnCode;
    }

    /** Số phút một liên kết thanh toán còn hiệu lực trên cổng VNPay. */
    public int expireMinutes() {
        return expireMinutes;
    }

    // ---------------------------------------------------------------- tạo liên kết thanh toán

    /** Liên kết chuyển khách sang cổng VNPay cho một lần thanh toán. */
    public String buildPaymentUrl(OnlinePayment attempt, String orderCode, String ipAddress) {
        return buildPaymentUrl(attempt, orderCode, ipAddress, ZonedDateTime.now(VN_ZONE).toLocalDateTime());
    }

    String buildPaymentUrl(OnlinePayment attempt, String orderCode, String ipAddress, LocalDateTime created) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(toVnpAmount(attempt.getAmount())));   // số tiền VND nhân 100
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", attempt.getTxnRef());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + orderCode);              // không dấu theo khuyến nghị của VNPay
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", created.format(DATE_FORMAT));
        params.put("vnp_ExpireDate", created.plusMinutes(expireMinutes).format(DATE_FORMAT));

        String data = hashData(params);
        return payUrl + "?" + data + "&vnp_SecureHash=" + hmacSha512(hashSecret, data);
    }

    // ---------------------------------------------------------------- kiểm tra thông báo từ VNPay

    /** Chữ ký vnp_SecureHash có khớp các tham số còn lại không, và đúng mã website của mình (so sánh hằng thời gian). */
    public boolean verify(Map<String, String> params) {
        if (!isConfigured()) {
            return false;
        }
        String received = params.get("vnp_SecureHash");
        if (received == null || received.isEmpty() || !tmnCode.equals(params.get("vnp_TmnCode"))) {
            return false;
        }
        Map<String, String> signed = new HashMap<>(params);
        signed.remove("vnp_SecureHash");
        signed.remove("vnp_SecureHashType");
        String expected = hmacSha512(hashSecret, hashData(signed));
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                received.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------- phụ trợ (tĩnh, kiểm thử riêng được)

    /** Chuỗi dữ liệu cần ký (cũng chính là query string gửi đi). */
    static String hashData(Map<String, String> params) {
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (data.length() > 0) {
                data.append('&');
            }
            data.append(entry.getKey()).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        return data.toString();
    }

    static String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            StringBuilder hex = new StringBuilder();
            for (byte b : mac.doFinal(data.getBytes(StandardCharsets.UTF_8))) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không tính được chữ ký VNPay", e);
        }
    }

    /** Số tiền gửi cho VNPay: VND làm tròn đến đồng, nhân 100. */
    public static long toVnpAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).longValueExact() * 100;
    }

    /** Diễn giải mã vnp_ResponseCode thường gặp thành câu tiếng Việt cho khách. */
    public static String describeResponse(String code) {
        if (code == null) {
            return "Không rõ lý do";
        }
        return switch (code) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Giao dịch bị nghi ngờ gian lận";
            case "09" -> "Thẻ hoặc tài khoản chưa đăng ký Internet Banking";
            case "10" -> "Xác thực thông tin thẻ sai quá 3 lần";
            case "11" -> "Đã hết thời gian chờ thanh toán";
            case "12" -> "Thẻ hoặc tài khoản bị khóa";
            case "13" -> "Sai mật khẩu xác thực giao dịch (OTP)";
            case "24" -> "Bạn đã hủy giao dịch";
            case "51" -> "Tài khoản không đủ số dư";
            case "65" -> "Tài khoản vượt hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng đang bảo trì";
            case "79" -> "Nhập sai mật khẩu thanh toán quá số lần quy định";
            default -> "Giao dịch không thành công (mã lỗi " + code + ")";
        };
    }
}
