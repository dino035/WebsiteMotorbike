package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.PasswordResetToken;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

public class PasswordResetDB {

    public static final String INVALID_LINK_MESSAGE = "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn";

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Tạo mã đặt lại mật khẩu cho một tài khoản và trả về MÃ GỐC (chỉ dùng để đưa vào email; CSDL chỉ lưu SHA-256).
     * Các mã cũ chưa dùng của tài khoản bị vô hiệu. Giới hạn số lần yêu cầu mỗi giờ để chống gửi email liên tục.
     *
     * @throws IllegalArgumentException yêu cầu quá nhiều lần (đã rollback)
     * @throws IllegalStateException    lỗi CSDL (đã rollback)
     */
    public static String createToken(String accountId, int expireMinutes, int maxPerHour) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            LocalDateTime now = LocalDateTime.now();

            // Dọn các mã đã quá một ngày tuổi cho gọn bảng
            em.createQuery("DELETE FROM PasswordResetToken t WHERE t.createdAt < :limit")
                    .setParameter("limit", now.minusDays(1))
                    .executeUpdate();

            TypedQuery<Long> count = em.createQuery(
                    "SELECT COUNT(t) FROM PasswordResetToken t WHERE t.accountId = :accountId AND t.createdAt > :since",
                    Long.class);
            count.setParameter("accountId", accountId);
            count.setParameter("since", now.minusHours(1));
            if (count.getSingleResult() >= maxPerHour) {
                throw new IllegalArgumentException("Bạn đã yêu cầu quá nhiều lần, vui lòng thử lại sau");
            }

            // Mã mới thay thế các mã cũ chưa dùng
            em.createQuery("UPDATE PasswordResetToken t SET t.usedAt = :now WHERE t.accountId = :accountId AND t.usedAt IS NULL")
                    .setParameter("now", now)
                    .setParameter("accountId", accountId)
                    .executeUpdate();

            String rawToken = newRawToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setTokenHash(hash(rawToken));
            token.setAccountId(accountId);
            token.setCreatedAt(now);
            token.setExpiresAt(now.plusMinutes(expireMinutes));
            em.persist(token);
            trans.commit();
            return rawToken;
        } catch (IllegalArgumentException e) {
            if (trans.isActive()) {
                trans.rollback();
            }
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            if (trans.isActive()) {
                trans.rollback();
            }
            throw new IllegalStateException("Không thể tạo liên kết đặt lại mật khẩu, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /** Mã gốc có còn dùng được không (tồn tại, chưa dùng, chưa hết hạn). */
    public static boolean isValid(String rawToken) {
        if (rawToken == null || rawToken.trim().isEmpty()) {
            return false;
        }
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            return findUsable(em, rawToken.trim(), false) != null;
        } finally {
            em.close();
        }
    }

    /**
     * Đặt mật khẩu mới (đã băm) cho tài khoản của mã. Mã chỉ dùng được một lần: sau khi đặt xong mã và mọi
     * mã còn lại của tài khoản bị vô hiệu.
     *
     * @throws IllegalArgumentException mã không hợp lệ, đã dùng hoặc hết hạn (đã rollback)
     * @throws IllegalStateException    lỗi CSDL (đã rollback)
     */
    public static void resetPassword(String rawToken, String newPasswordHash) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            PasswordResetToken token = rawToken == null ? null : findUsable(em, rawToken.trim(), true);
            if (token == null) {
                throw new IllegalArgumentException(INVALID_LINK_MESSAGE);
            }
            User user = em.find(User.class, token.getAccountId());
            if (user == null) {
                throw new IllegalArgumentException(INVALID_LINK_MESSAGE);
            }

            user.setPasswordHash(newPasswordHash);
            LocalDateTime now = LocalDateTime.now();
            token.setUsedAt(now);
            em.createQuery("UPDATE PasswordResetToken t SET t.usedAt = :now WHERE t.accountId = :accountId AND t.usedAt IS NULL")
                    .setParameter("now", now)
                    .setParameter("accountId", token.getAccountId())
                    .executeUpdate();
            trans.commit();
        } catch (IllegalArgumentException e) {
            if (trans.isActive()) {
                trans.rollback();
            }
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            if (trans.isActive()) {
                trans.rollback();
            }
            throw new IllegalStateException("Không thể đặt lại mật khẩu, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /** Xóa mã vừa tạo khi gửi email thất bại, để không để lại mã không ai nhận được. */
    public static void discard(String rawToken) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            em.createQuery("DELETE FROM PasswordResetToken t WHERE t.tokenHash = :hash")
                    .setParameter("hash", hash(rawToken))
                    .executeUpdate();
            trans.commit();
        } catch (Exception e) {
            System.out.println(e);
            if (trans.isActive()) {
                trans.rollback();
            }
        } finally {
            em.close();
        }
    }

    private static PasswordResetToken findUsable(EntityManager em, String rawToken, boolean lock) {
        TypedQuery<PasswordResetToken> q = em.createQuery(
                "SELECT t FROM PasswordResetToken t WHERE t.tokenHash = :hash "
                        + "AND t.usedAt IS NULL AND t.expiresAt > :now", PasswordResetToken.class);
        q.setParameter("hash", hash(rawToken));
        q.setParameter("now", LocalDateTime.now());
        if (lock) {
            q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /** 32 byte ngẫu nhiên mật mã học, mã hóa Base64 an toàn cho URL (khoảng 43 ký tự). */
    private static String newRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
