package com.example.websitemotorbike.util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * Gửi email và đọc cấu hình từ mail.properties (xem mail.example.properties). Tệp này không nằm trong Git
 * vì chứa mật khẩu SMTP hoặc khóa API, giống persistence.xml.
 *
 * Có hai cách gửi email thật:
 * - Brevo API (mail.brevo.apiKey): gửi qua HTTPS cổng 443, dùng khi lên hosting chặn cổng SMTP (Render gói miễn phí).
 * - SMTP (mail.host): dùng khi chạy trên máy cá nhân hoặc máy chủ không chặn cổng SMTP.
 * Có cả hai thì dùng Brevo API.
 *
 * Chưa có mail.properties hoặc cả mail.brevo.apiKey và mail.host đều trống thì chạy CHẾ ĐỘ PHÁT TRIỂN: không gửi
 * email thật mà in nội dung (kèm liên kết đặt lại mật khẩu) ra console của Tomcat để thử nghiệm.
 */
public class Mailer {

    private static final Properties CONFIG = loadConfig();

    private static Properties loadConfig() {
        return ConfigFile.load("mail.properties");
    }

    private static String get(String key) {
        String value = CONFIG.getProperty(key);
        return value == null ? "" : value.trim();
    }

    private static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean useBrevo() {
        return !get("mail.brevo.apiKey").isEmpty();
    }

    /** Đã cấu hình gửi email thật chưa (Brevo API hoặc SMTP; false = chế độ phát triển, chỉ in ra console). */
    public static boolean isConfigured() {
        return useBrevo() || !get("mail.host").isEmpty();
    }

    /** Thời hạn của liên kết đặt lại mật khẩu, tính bằng phút. */
    public static int resetExpireMinutes() {
        return getInt("reset.expireMinutes", 30);
    }

    /** Số lần yêu cầu đặt lại mật khẩu tối đa mỗi giờ cho một tài khoản. */
    public static int resetMaxPerHour() {
        return getInt("reset.maxPerHour", 5);
    }

    /**
     * Địa chỉ gốc của website dùng để tạo liên kết trong email. Khi gửi email thật bắt buộc phải cấu hình
     * app.baseUrl (không lấy từ yêu cầu của người dùng để tránh bị giả mạo tiêu đề Host); ở chế độ phát triển
     * thì lấy từ chính yêu cầu hiện tại.
     */
    public static String baseUrl(HttpServletRequest request) {
        String configured = get("app.baseUrl");
        if (!configured.isEmpty()) {
            return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
        }
        if (isConfigured()) {
            throw new IllegalStateException("Hệ thống chưa cấu hình app.baseUrl nên chưa gửi được email");
        }
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(request.getScheme()) && port == 80)
                || ("https".equals(request.getScheme()) && port == 443);
        return request.getScheme() + "://" + request.getServerName() + (defaultPort ? "" : ":" + port)
                + request.getContextPath();
    }

    /**
     * Gửi một email văn bản (UTF-8).
     *
     * @throws IllegalStateException không gửi được (đã có thông báo tiếng Việt)
     */
    public static void send(String to, String subject, String body) {
        if (!isConfigured()) {
            System.out.println("[MAIL - chế độ phát triển, chưa cấu hình Brevo hoặc SMTP]");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println(body);
            System.out.println("[HẾT EMAIL]");
            return;
        }
        if (useBrevo()) {
            sendByBrevo(to, subject, body);
            return;
        }

        Properties smtp = new Properties();
        smtp.put("mail.smtp.host", get("mail.host"));
        smtp.put("mail.smtp.port", String.valueOf(getInt("mail.port", 587)));
        smtp.put("mail.smtp.starttls.enable", String.valueOf("true".equalsIgnoreCase(get("mail.starttls"))));
        smtp.put("mail.smtp.ssl.enable", String.valueOf("true".equalsIgnoreCase(get("mail.ssl"))));
        smtp.put("mail.smtp.connectiontimeout", "5000");
        smtp.put("mail.smtp.timeout", "10000");
        smtp.put("mail.smtp.writetimeout", "10000");

        Authenticator authenticator = null;
        if (!get("mail.username").isEmpty()) {
            smtp.put("mail.smtp.auth", "true");
            final String user = get("mail.username");
            final String password = get("mail.password");
            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, password);
                }
            };
        }

        try {
            Session session = Session.getInstance(smtp, authenticator);
            MimeMessage message = new MimeMessage(session);
            String from = get("mail.from").isEmpty() ? get("mail.username") : get("mail.from");
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8");
            Transport.send(message);
        } catch (MessagingException e) {
            System.out.println("Gửi email thất bại: " + e);
            throw new IllegalStateException("Không thể gửi email, vui lòng thử lại sau", e);
        }
    }

    private static final String BREVO_DEFAULT_URL = "https://api.brevo.com/v3/smtp/email";

    /**
     * Gửi qua API của Brevo (POST HTTPS). mail.from là địa chỉ người gửi đã được xác minh trong Brevo
     * (Senders &amp; IP). Brevo trả 201 khi nhận thư để gửi.
     */
    private static void sendByBrevo(String to, String subject, String body) {
        String from = get("mail.from");
        if (from.isEmpty()) {
            throw new IllegalStateException("Hệ thống chưa cấu hình mail.from nên chưa gửi được email");
        }
        String fromName = get("mail.fromName");

        StringBuilder json = new StringBuilder("{\"sender\":{");
        if (!fromName.isEmpty()) {
            json.append("\"name\":").append(jsonString(fromName)).append(',');
        }
        json.append("\"email\":").append(jsonString(from)).append("},")
                .append("\"to\":[{\"email\":").append(jsonString(to.trim())).append("}],")
                .append("\"subject\":").append(jsonString(subject)).append(',')
                .append("\"textContent\":").append(jsonString(body)).append('}');

        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(get("mail.brevo.url").isEmpty() ? BREVO_DEFAULT_URL : get("mail.brevo.url")))
                    .timeout(Duration.ofSeconds(10))
                    .header("api-key", get("mail.brevo.apiKey"))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString(), java.nio.charset.StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                // Nội dung trả về của Brevo chỉ nêu lý do lỗi (sai khóa, người gửi chưa xác minh...), không chứa khóa
                System.out.println("Gửi email qua Brevo thất bại: HTTP " + response.statusCode() + " " + response.body());
                throw new IllegalStateException("Không thể gửi email, vui lòng thử lại sau");
            }
        } catch (IOException e) {
            System.out.println("Gửi email qua Brevo thất bại: " + e);
            throw new IllegalStateException("Không thể gửi email, vui lòng thử lại sau", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Không thể gửi email, vui lòng thử lại sau", e);
        }
    }

    /** Chuỗi JSON có dấu nháy kép, thoát các ký tự đặc biệt (không dùng thư viện JSON). */
    private static String jsonString(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}
