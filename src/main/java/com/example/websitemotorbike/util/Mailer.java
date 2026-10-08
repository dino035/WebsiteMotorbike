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

import java.util.Properties;

/**
 * Gửi email và đọc cấu hình từ mail.properties (xem mail.example.properties). Tệp này không nằm trong Git
 * vì chứa mật khẩu SMTP, giống persistence.xml.
 *
 * Chưa có mail.properties hoặc mail.host trống thì chạy CHẾ ĐỘ PHÁT TRIỂN: không gửi email thật mà in nội dung
 * (kèm liên kết đặt lại mật khẩu) ra console của Tomcat để thử nghiệm.
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

    /** Đã cấu hình SMTP thật chưa (false = chế độ phát triển, chỉ in email ra console). */
    public static boolean isConfigured() {
        return !get("mail.host").isEmpty();
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
            System.out.println("[MAIL - chế độ phát triển, chưa cấu hình SMTP]");
            System.out.println("To: " + to);
            System.out.println("Subject: " + subject);
            System.out.println(body);
            System.out.println("[HẾT EMAIL]");
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
}
