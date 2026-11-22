package com.nguyen.motoparts.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Luồng riêng để gửi email, tách khỏi luồng xử lý yêu cầu: gọi SMTP hay Brevo mất từ vài trăm mili giây đến vài giây,
 * nếu làm ngay trong yêu cầu thì thời gian phản hồi tố cáo email nào có tài khoản.
 *
 * <p>Hàng đợi có giới hạn. Đầy thì bỏ email đó và ghi log chứ không ném lỗi: lỗi ném ra lúc này nằm ở bước sau khi
 * giao dịch đã commit, sẽ biến thành lỗi 500 cho một yêu cầu thực ra đã xử lý xong.
 */
@Configuration
@EnableAsync
public class MailAsyncConfig {

    public static final String MAIL_EXECUTOR = "mailExecutor";

    private static final Logger log = LoggerFactory.getLogger(MailAsyncConfig.class);

    @Bean(name = MAIL_EXECUTOR)
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("mail-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setRejectedExecutionHandler((task, pool) ->
                log.error("Hang doi gui email da day, bo qua mot email"));
        executor.setWaitForTasksToCompleteOnShutdown(true);   // tắt máy thì gửi nốt các email đang chờ
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
