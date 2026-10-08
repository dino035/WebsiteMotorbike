package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.AccountLock;
import com.example.websitemotorbike.model.OrderStatus;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.model.UserRole;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountLockDB {

    private static final List<OrderStatus> UNFINISHED = List.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING);

    public static boolean isLocked(String accountId) {
        if (accountId == null) {
            return false;
        }
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            return em.find(AccountLock.class, accountId) != null;
        } finally {
            em.close();
        }
    }

    /** Mọi tài khoản đang bị khóa, theo mã tài khoản. */
    public static Map<String, AccountLock> selectAllLocks() {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            Map<String, AccountLock> map = new HashMap<>();
            for (AccountLock lock : em.createQuery("SELECT l FROM AccountLock l", AccountLock.class).getResultList()) {
                map.put(lock.getAccountId(), lock);
            }
            return map;
        } finally {
            em.close();
        }
    }

    /** Số đơn chưa hoàn tất của từng tài khoản (chỉ gồm tài khoản có ít nhất một đơn như vậy). */
    public static Map<String, Long> countUnfinishedOrders() {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<Object[]> q = em.createQuery(
                    "SELECT o.accountId, COUNT(o) FROM Orders o WHERE o.status IN :statuses GROUP BY o.accountId",
                    Object[].class);
            q.setParameter("statuses", UNFINISHED);
            Map<String, Long> map = new HashMap<>();
            for (Object[] row : q.getResultList()) {
                map.put((String) row[0], (Long) row[1]);
            }
            return map;
        } finally {
            em.close();
        }
    }

    /**
     * Admin khóa một tài khoản khách hàng (SRS 3.23). Không khóa Admin. Nếu tài khoản còn đơn hàng chưa
     * hoàn tất thì phải có force = true (Admin đã xác nhận sau khi xem cảnh báo, ngoại lệ 2.1).
     *
     * @throws IllegalArgumentException không đủ điều kiện khóa (đã rollback)
     * @throws IllegalStateException    lỗi CSDL (đã rollback)
     */
    public static AccountLock lock(String adminAccountId, String accountId, String reason, boolean force) {
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.length() > 200) {
            throw new IllegalArgumentException("Lý do khóa quá dài (tối đa 200 ký tự)");
        }

        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            User target = em.find(User.class, accountId);
            if (target == null) {
                throw new IllegalArgumentException("Không tìm thấy tài khoản");
            }
            if (target.getUserRole() != UserRole.CUSTOMER) {
                throw new IllegalArgumentException("Chỉ khóa được tài khoản khách hàng");
            }
            if (em.find(AccountLock.class, accountId) != null) {
                throw new IllegalArgumentException("Tài khoản này đã bị khóa");
            }

            TypedQuery<Long> q = em.createQuery(
                    "SELECT COUNT(o) FROM Orders o WHERE o.accountId = :accountId AND o.status IN :statuses",
                    Long.class);
            q.setParameter("accountId", accountId);
            q.setParameter("statuses", UNFINISHED);
            long unfinished = q.getSingleResult();
            if (unfinished > 0 && !force) {
                throw new IllegalArgumentException("Tài khoản đang có " + unfinished
                        + " đơn hàng chưa hoàn tất. Hãy tích ô xác nhận nếu vẫn muốn khóa");
            }

            AccountLock lock = new AccountLock();
            lock.setAccountId(accountId);
            lock.setReason(cleanReason.isEmpty() ? null : cleanReason);
            lock.setLockedBy(adminAccountId);
            lock.setLockedAt(LocalDateTime.now());
            em.persist(lock);
            trans.commit();
            return lock;
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
            throw new IllegalStateException("Không thể khóa tài khoản, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /** Admin mở khóa: xóa dòng khóa của tài khoản. */
    public static void unlock(String accountId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            AccountLock lock = em.find(AccountLock.class, accountId);
            if (lock == null) {
                throw new IllegalArgumentException("Tài khoản này chưa bị khóa");
            }
            em.remove(lock);
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
            throw new IllegalStateException("Không thể mở khóa tài khoản, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }
}
