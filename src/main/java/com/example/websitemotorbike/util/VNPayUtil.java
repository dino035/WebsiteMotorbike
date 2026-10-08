package com.example.websitemotorbike.util;

import com.example.websitemotorbike.model.OnlinePayment;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
 * Tạo liên kết thanh toán và kiểm tra chữ ký theo tài liệu tích hợp VNPay phiên bản 2.1.0:
 * các tham số vnp_* được sắp theo tên, bỏ tham số rỗng, mã hóa URL giá trị, nối bằng "&" rồi ký HMAC-SHA512
 * bằng HashSecret. Chữ ký được gửi kèm trong vnp_SecureHash.
 */
public class VNPayUtil {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Chuỗi dữ liệu cần ký (cũng chính là query string gửi đi). */
    public static String hashData(Map<String, String> params) {
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

    public static String hmacSha512(String key, String data) {
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

    /** Liên kết chuyển khách sang cổng VNPay cho một lần thanh toán. */
    public static String buildPaymentUrl(OnlinePayment attempt, String orderCode, String ipAddress) {
        LocalDateTime created = ZonedDateTime.now(VN_ZONE).toLocalDateTime();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", VNPayConfig.tmnCode());
        params.put("vnp_Amount", String.valueOf(toVnpAmount(attempt)));   // số tiền VND nhân 100
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", attempt.getTxnRef());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + orderCode);  // không dấu theo khuyến nghị của VNPay
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", VNPayConfig.returnUrl());
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", created.format(DATE_FORMAT));
        params.put("vnp_ExpireDate", created.plusMinutes(VNPayConfig.expireMinutes()).format(DATE_FORMAT));

        String data = hashData(params);
        return VNPayConfig.payUrl() + "?" + data + "&vnp_SecureHash=" + hmacSha512(VNPayConfig.hashSecret(), data);
    }

    /** Số tiền gửi cho VNPay: VND làm tròn đến đồng, nhân 100. */
    public static long toVnpAmount(OnlinePayment attempt) {
        return attempt.getAmount().setScale(0, java.math.RoundingMode.HALF_UP).longValueExact() * 100;
    }

    /** Lấy mọi tham số vnp_* mà VNPay gửi về (trình duyệt quay lại hoặc IPN). */
    public static Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (entry.getKey().startsWith("vnp_") && entry.getValue().length > 0) {
                params.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        return params;
    }

    /** Chữ ký vnp_SecureHash có khớp với các tham số còn lại không (so sánh hằng thời gian). */
    public static boolean verifySignature(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");
        if (received == null || received.isEmpty()) {
            return false;
        }
        Map<String, String> signed = new HashMap<>(params);
        signed.remove("vnp_SecureHash");
        signed.remove("vnp_SecureHashType");
        String expected = hmacSha512(VNPayConfig.hashSecret(), hashData(signed));
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                received.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    /** Địa chỉ IP gửi cho VNPay; không tin tiêu đề X-Forwarded-For vì người dùng tự đặt được. */
    public static String clientIp(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return ip == null || ip.contains(":") ? "127.0.0.1" : ip;
    }

    /** Diễn giải mã vnp_ResponseCode thường gặp thành câu tiếng Việt cho khách. */
    public static String describeResponse(String code) {
        if (code == null) {
            return "Không rõ lý do";
        }
        switch (code) {
            case "00": return "Giao dịch thành công";
            case "07": return "Giao dịch bị nghi ngờ gian lận";
            case "09": return "Thẻ hoặc tài khoản chưa đăng ký Internet Banking";
            case "10": return "Xác thực thông tin thẻ sai quá 3 lần";
            case "11": return "Đã hết thời gian chờ thanh toán";
            case "12": return "Thẻ hoặc tài khoản bị khóa";
            case "13": return "Sai mật khẩu xác thực giao dịch (OTP)";
            case "24": return "Bạn đã hủy giao dịch";
            case "51": return "Tài khoản không đủ số dư";
            case "65": return "Tài khoản vượt hạn mức giao dịch trong ngày";
            case "75": return "Ngân hàng đang bảo trì";
            case "79": return "Nhập sai mật khẩu thanh toán quá số lần quy định";
            default: return "Giao dịch không thành công (mã lỗi " + code + ")";
        }
    }
}
