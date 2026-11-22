package com.nguyen.motoparts.auth;

import com.nguyen.motoparts.common.exception.BadRequestException;

import java.nio.charset.StandardCharsets;

/**
 * Luật về độ dài mật khẩu, dùng chung cho đăng ký và đặt lại mật khẩu.
 * BCrypt chỉ xử lý tối đa 72 BYTE, còn {@code @Size} đếm KÝ TỰ: chữ tiếng Việt có dấu chiếm 2-3 byte
 * trong UTF-8, nên mật khẩu qua được {@code @Size} vẫn có thể vượt giới hạn của thuật toán băm.
 * Phải chặn trước khi gọi {@code passwordEncoder.encode(...)}.
 */
public final class PasswordPolicy {

    public static final int BCRYPT_MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static boolean fitsBcrypt(String rawPassword) {
        return rawPassword.getBytes(StandardCharsets.UTF_8).length <= BCRYPT_MAX_BYTES;
    }

    /** @throws BadRequestException mật khẩu dài quá 72 byte (tính theo UTF-8) */
    public static void requireFitsBcrypt(String rawPassword) {
        if (!fitsBcrypt(rawPassword)) {
            throw new BadRequestException("Mật khẩu quá dài. Hãy dùng mật khẩu ngắn hơn, "
                    + "hoặc bỏ dấu tiếng Việt vì chữ có dấu chiếm nhiều chỗ hơn.");
        }
    }
}
