package com.nguyen.motoparts.auth;

import com.nguyen.motoparts.common.exception.EmailSendException;
import com.nguyen.motoparts.mail.EmailService;
import com.nguyen.motoparts.mail.MailAsyncConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gửi email đặt lại mật khẩu SAU KHI giao dịch commit và Ở LUỒNG KHÁC.
 *
 * <ul>
 *   <li>Sau commit: mã đã nằm trong DB trước khi email đi, nên khách bấm liên kết là dùng được ngay; giao dịch rollback
 *       thì email không đi.</li>
 *   <li>Luồng khác: yêu cầu trả lời ngay, thời gian phản hồi không phụ thuộc việc email có tài khoản hay không.</li>
 * </ul>
 * Đánh đổi: gửi lỗi thì chỉ ghi log, người dùng vẫn thấy câu trả lời trung lập (đúng chủ ý: không để lộ gì). Mã nằm lại
 * trong DB nhưng vô hại, hết hạn sau vài chục phút và không ai biết nó để mà dùng.
 */
@Component
public class PasswordResetMailListener {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailListener.class);

    private final EmailService emailService;

    public PasswordResetMailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async(MailAsyncConfig.MAIL_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(PasswordResetMail mail) {
        try {
            emailService.send(mail.to(), mail.subject(), mail.body());
        } catch (EmailSendException e) {
            log.error("Gui email dat lai mat khau that bai", e);
        }
    }
}
