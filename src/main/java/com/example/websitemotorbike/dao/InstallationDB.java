package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.InstallationRequest;
import com.example.websitemotorbike.model.InstallationStatus;
import com.example.websitemotorbike.model.Notification;
import com.example.websitemotorbike.model.NotificationType;
import com.example.websitemotorbike.model.Order;
import com.example.websitemotorbike.model.OrderStatus;
import com.example.websitemotorbike.util.DBUtil;
import com.example.websitemotorbike.util.InstallationPolicy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.List;

public class InstallationDB {

    public enum Action {
        DONE,
        CANCEL,
        RESCHEDULE
    }

    public static InstallationRequest selectByOrderId(String orderId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<InstallationRequest> q = em.createQuery(
                    "SELECT r FROM InstallationRequest r WHERE r.order.orderId = :orderId",
                    InstallationRequest.class);
            q.setParameter("orderId", orderId);
            q.setMaxResults(1);
            try {
                return q.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
        } finally {
            em.close();
        }
    }

    /** Mọi lịch lắp đặt, ngày hẹn gần nhất trước; status = null thì không lọc. */
    public static List<InstallationRequest> selectAll(InstallationStatus status) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            String jpql = "SELECT r FROM InstallationRequest r"
                    + (status == null ? "" : " WHERE r.status = :status")
                    + " ORDER BY r.scheduledDate, r.createdAt";
            TypedQuery<InstallationRequest> q = em.createQuery(jpql, InstallationRequest.class);
            if (status != null) {
                q.setParameter("status", status);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Admin xử lý một lịch lắp đặt đang ở SCHEDULED (SRS 3.21):
     * RESCHEDULE đổi ngày hẹn (kiểm tra lịch trống), DONE đánh dấu đã lắp đặt xong,
     * CANCEL hủy lịch. Sau khi lưu sẽ thông báo cho khách.
     */
    public static InstallationRequest update(String installationId, Action action, LocalDate newDate) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        InstallationRequest request;
        String message;
        try {
            request = em.find(InstallationRequest.class, installationId, LockModeType.PESSIMISTIC_WRITE);
            if (request == null) {
                throw new IllegalArgumentException("Không tìm thấy lịch lắp đặt");
            }
            if (request.getStatus() != InstallationStatus.SCHEDULED) {
                throw new IllegalArgumentException("Lịch lắp đặt này đã được xử lý");
            }
            Order order = request.getOrder();
            String code = order.getShortCode();

            switch (action) {
                case DONE:
                    if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CANCELLED) {
                        throw new IllegalArgumentException(
                                "Hãy xác nhận đơn hàng trước khi đánh dấu đã lắp đặt");
                    }
                    request.setStatus(InstallationStatus.DONE);
                    message = "Đã lắp đặt xong phụ tùng cho đơn hàng #" + code;
                    break;
                case CANCEL:
                    request.setStatus(InstallationStatus.CANCELLED);
                    message = "Lịch lắp đặt của đơn hàng #" + code + " đã bị hủy";
                    break;
                case RESCHEDULE:
                    InstallationPolicy.validateDate(newDate, LocalDate.now());
                    if (newDate.equals(request.getScheduledDate())) {
                        throw new IllegalArgumentException("Lịch hẹn vẫn là ngày này, hãy chọn ngày khác");
                    }
                    lockDay(em, newDate);
                    if (countActiveOnDate(em, newDate) >= InstallationPolicy.MAX_PER_DAY) {
                        throw new IllegalArgumentException("Ngày này đã kín lịch, vui lòng chọn ngày khác");
                    }
                    request.setScheduledDate(newDate);
                    message = "Lịch lắp đặt của đơn hàng #" + code + " đã được đổi sang ngày "
                            + request.getScheduledDateText();
                    break;
                default:
                    throw new IllegalArgumentException("Thao tác không hợp lệ");
            }
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
            throw new IllegalStateException("Không thể cập nhật lịch lắp đặt, vui lòng thử lại", e);
        } finally {
            em.close();
        }

        notifyCustomer(request.getAccountId(), message);
        return request;
    }

    // ----------------------------------------------------------------------------------
    // Dùng chung cho OrderDB (cùng package) khi đặt hàng có lắp đặt
    // ----------------------------------------------------------------------------------

    /**
     * Khóa theo ngày (advisory lock của PostgreSQL, tự nhả khi kết thúc giao dịch) để hai khách
     * cùng đặt vào một ngày không làm vượt số lượt tối đa.
     */
    static void lockDay(EntityManager em, LocalDate date) {
        em.createNativeQuery("SELECT 1 FROM (SELECT pg_advisory_xact_lock(?1) AS l) t")
                .setParameter(1, date.toEpochDay())
                .getResultList();
    }

    /** Số lịch còn hiệu lực (chưa hủy) trong một ngày. */
    static long countActiveOnDate(EntityManager em, LocalDate date) {
        TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(r) FROM InstallationRequest r "
                        + "WHERE r.scheduledDate = :date AND r.status <> :cancelled", Long.class);
        q.setParameter("date", date);
        q.setParameter("cancelled", InstallationStatus.CANCELLED);
        return q.getSingleResult();
    }

    /** Khi đơn hàng bị hủy: hủy luôn lịch lắp đặt (nếu còn SCHEDULED) để trả lại chỗ trống. */
    static void cancelForOrder(EntityManager em, String orderId) {
        TypedQuery<InstallationRequest> q = em.createQuery(
                "SELECT r FROM InstallationRequest r WHERE r.order.orderId = :orderId", InstallationRequest.class);
        q.setParameter("orderId", orderId);
        for (InstallationRequest r : q.getResultList()) {
            if (r.getStatus() == InstallationStatus.SCHEDULED) {
                r.setStatus(InstallationStatus.CANCELLED);
            }
        }
    }

    private static void notifyCustomer(String accountId, String content) {
        Notification notification = new Notification();
        notification.setAccountId(accountId);
        notification.setType(NotificationType.ORDER_STATUS_CHANGED);
        notification.setContent(content);
        NotificationDB.insert(notification);
    }
}
