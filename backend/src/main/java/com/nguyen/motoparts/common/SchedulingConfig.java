package com.nguyen.motoparts.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật các tác vụ chạy nền (@Scheduled), hiện có tự hủy đơn VNPay quá hạn. Tắt bằng app.scheduling.enabled=false
 * (bộ kiểm thử tắt để tác vụ không tự chạy giữa các ca test).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
