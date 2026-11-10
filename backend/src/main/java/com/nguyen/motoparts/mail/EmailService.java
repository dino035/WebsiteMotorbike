package com.nguyen.motoparts.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nguyen.motoparts.common.exception.EmailSendException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 * Gửi email văn bản. Có ba chế độ, chọn theo cấu hình (xem application.yml, mục app.mail):
 * <ol>
 *   <li>Brevo API (BREVO_API_KEY): gửi qua HTTPS cổng 443, dùng được trên Render vì Render chặn cổng SMTP.</li>
 *   <li>SMTP (MAIL_HOST): dùng khi chạy máy cá nhân hoặc máy chủ không chặn cổng SMTP.</li>
 *   <li>Chế độ phát triển (không cấu hình gì): không gửi, chỉ ghi nội dung email vào log để thử nghiệm.</li>
 * </ol>
 * Có cả Brevo và SMTP thì dùng Brevo. Cấu hình lấy từ biến môi trường, không ghi khóa vào Git.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final ObjectMapper mapper;
    private final String from;
    private final String fromName;
    private final String brevoApiKey;
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;
    private final boolean smtpStartTls;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public EmailService(ObjectMapper mapper,
                        @Value("${app.mail.from:}") String from,
                        @Value("${app.mail.from-name:MotoParts}") String fromName,
                        @Value("${app.mail.brevo-api-key:}") String brevoApiKey,
                        @Value("${app.mail.smtp-host:}") String smtpHost,
                        @Value("${app.mail.smtp-port:587}") int smtpPort,
                        @Value("${app.mail.smtp-username:}") String smtpUsername,
                        @Value("${app.mail.smtp-password:}") String smtpPassword,
                        @Value("${app.mail.smtp-starttls:true}") boolean smtpStartTls) {
        this.mapper = mapper;
        this.from = from.trim();
        this.fromName = fromName.trim();
        this.brevoApiKey = brevoApiKey.trim();
        this.smtpHost = smtpHost.trim();
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername.trim();
        this.smtpPassword = smtpPassword;
        this.smtpStartTls = smtpStartTls;
    }

    /** Đã cấu hình gửi email thật chưa (Brevo hoặc SMTP). */
    public boolean isConfigured() {
        return !brevoApiKey.isEmpty() || !smtpHost.isEmpty();
    }

    /** @throws EmailSendException không gửi được */
    public void send(String to, String subject, String body) {
        if (!isConfigured()) {
            log.info("[MAIL - che do phat trien, chua cau hinh Brevo/SMTP]\nTo: {}\nSubject: {}\n{}\n[HET EMAIL]",
                    to, subject, body);
            return;
        }
        if (from.isEmpty()) {
            throw new EmailSendException("Chua cau hinh MAIL_FROM", null);
        }
        if (!brevoApiKey.isEmpty()) {
            sendByBrevo(to, subject, body);
        } else {
            sendBySmtp(to, subject, body);
        }
    }

    private void sendByBrevo(String to, String subject, String body) {
        try {
            ObjectNode json = mapper.createObjectNode();
            ObjectNode sender = json.putObject("sender").put("email", from);
            if (!fromName.isEmpty()) {
                sender.put("name", fromName);
            }
            json.putArray("to").addObject().put("email", to);
            json.put("subject", subject);
            json.put("textContent", body);

            HttpRequest request = HttpRequest.newBuilder(URI.create(BREVO_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("api-key", brevoApiKey)
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(json), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                // Nội dung trả về chỉ nêu lý do lỗi (sai khóa, người gửi chưa xác minh...), không chứa khóa API.
                throw new EmailSendException("Brevo tra ve HTTP " + response.statusCode() + ": " + response.body(), null);
            }
        } catch (EmailSendException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmailSendException("Gui email qua Brevo bi gian doan", e);
        } catch (Exception e) {
            throw new EmailSendException("Gui email qua Brevo that bai", e);
        }
    }

    private void sendBySmtp(String to, String subject, String body) {
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(smtpHost);
            sender.setPort(smtpPort);
            sender.setDefaultEncoding("UTF-8");
            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.writetimeout", "10000");
            props.put("mail.smtp.starttls.enable", String.valueOf(smtpStartTls));
            if (!smtpUsername.isEmpty()) {
                sender.setUsername(smtpUsername);
                sender.setPassword(smtpPassword);
                props.put("mail.smtp.auth", "true");
            }
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false);
            sender.send(message);
        } catch (Exception e) {
            throw new EmailSendException("Gui email qua SMTP that bai", e);
        }
    }
}
