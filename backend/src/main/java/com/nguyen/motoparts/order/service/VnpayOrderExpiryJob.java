package com.nguyen.motoparts.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Tác vụ chạy nền, mỗi phút quét một lần tìm đơn VNPay quá hạn và hủy từng đơn trong giao dịch riêng (một đơn lỗi không
 * làm hỏng các đơn còn lại). Có nhiều bản chạy cùng lúc cũng an toàn vì {@link VnpayOrderExpiryService} khóa và kiểm tra lại.
 */
@Component
public class VnpayOrderExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(VnpayOrderExpiryJob.class);

    private final VnpayOrderExpiryService service;

    public VnpayOrderExpiryJob(VnpayOrderExpiryService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.vnpay.order-check-ms:60000}",
            initialDelayString = "${app.vnpay.order-check-ms:60000}")
    public void run() {
        runOnce(LocalDateTime.now());
    }

    /** Một lượt quét tính theo thời điểm now (tách ra để kiểm thử mà không phải chờ). Trả số đơn đã hủy. */
    public int runOnce(LocalDateTime now) {
        int cancelled = 0;
        try {
            for (String orderId : service.findCandidates(now)) {
                try {
                    if (service.expireIfStillUnpaid(orderId, now)) {
                        cancelled++;
                        log.info("Đã tự hủy đơn VNPay quá hạn chưa thanh toán: {}", orderId);
                    }
                } catch (RuntimeException e) {
                    log.warn("Không hủy được đơn quá hạn {}: {}", orderId, e.toString());
                }
            }
        } catch (RuntimeException e) {
            log.warn("Quét đơn VNPay quá hạn thất bại: {}", e.toString());
        }
        return cancelled;
    }
}
