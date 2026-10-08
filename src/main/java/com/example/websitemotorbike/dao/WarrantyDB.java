package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.Notification;
import com.example.websitemotorbike.model.NotificationType;
import com.example.websitemotorbike.model.OrderItem;
import com.example.websitemotorbike.model.OrderStatus;
import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.model.UserRole;
import com.example.websitemotorbike.model.WarrantyRequest;
import com.example.websitemotorbike.model.WarrantyStatus;
import com.example.websitemotorbike.util.DBUtil;
import com.example.websitemotorbike.util.WarrantyPolicy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

public class WarrantyDB {

    // ----------------------------------------------------------------------------------
    // Khách hàng (use case 3.14)
    // ----------------------------------------------------------------------------------

    /** Các sản phẩm khách đã mua trong đơn Hoàn thành, mới nhất trước. */
    public static List<OrderItem> selectCompletedItems(String accountId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<OrderItem> q = em.createQuery(
                    "SELECT i FROM OrderItem i WHERE i.order.accountId = :accountId "
                            + "AND i.order.status = :status ORDER BY i.order.createdAt DESC, i.productName",
                    OrderItem.class);
            q.setParameter("accountId", accountId);
            q.setParameter("status", OrderStatus.COMPLETED);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Dòng đơn hàng với điều kiện thuộc về accountId (chống gửi bảo hành cho đơn người khác). */
    public static OrderItem selectOwnedItem(String accountId, String orderItemId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            return findOwnedItem(em, accountId, orderItemId);
        } finally {
            em.close();
        }
    }

    public static List<WarrantyRequest> selectByAccountId(String accountId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<WarrantyRequest> q = em.createQuery(
                    "SELECT w FROM WarrantyRequest w WHERE w.accountId = :accountId ORDER BY w.createdAt DESC",
                    WarrantyRequest.class);
            q.setParameter("accountId", accountId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Khách gửi yêu cầu bảo hành. Kiểm tra: sản phẩm thuộc khách, đơn đã Hoàn thành,
     * còn trong thời hạn (xe 2 năm, phụ tùng 6 tháng), chưa có yêu cầu đang chờ cho sản phẩm này.
     * Sau khi lưu sẽ thông báo cho mọi Admin.
     *
     * @throws IllegalArgumentException không đủ điều kiện gửi (đã rollback)
     * @throws IllegalStateException    lỗi CSDL (đã rollback)
     */
    public static WarrantyRequest create(String accountId, String orderItemId,
                                         String description, String evidenceUrl) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        WarrantyRequest request;
        try {
            OrderItem item = findOwnedItem(em, accountId, orderItemId);
            if (item == null) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm đã mua");
            }
            // Khóa dòng để hai lần bấm gửi cùng lúc không tạo hai yêu cầu đang chờ
            em.lock(item, LockModeType.PESSIMISTIC_WRITE);

            if (item.getOrder().getStatus() != OrderStatus.COMPLETED) {
                throw new IllegalArgumentException(
                        "Chỉ gửi được yêu cầu bảo hành cho sản phẩm trong đơn hàng đã hoàn thành");
            }
            Product product = ProductDB.selectById(item.getProductId());
            if (!WarrantyPolicy.isWithin(item.getOrder().getCreatedAt(), product, LocalDateTime.now())) {
                throw new IllegalArgumentException("Sản phẩm này đã hết thời hạn bảo hành");
            }
            if (countPending(em, item) > 0) {
                throw new IllegalArgumentException(
                        "Yêu cầu bảo hành của sản phẩm này đang chờ xử lý");
            }

            request = new WarrantyRequest();
            request.setAccountId(accountId);
            request.setOrderItem(item);
            request.setDescription(description);
            request.setEvidenceUrl(evidenceUrl);
            request.setStatus(WarrantyStatus.PENDING);
            request.setCreatedAt(LocalDateTime.now());
            em.persist(request);
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
            throw new IllegalStateException("Không thể gửi yêu cầu bảo hành, vui lòng thử lại", e);
        } finally {
            em.close();
        }

        notifyAdmins("Có yêu cầu bảo hành mới #" + request.getShortCode()
                + " cho sản phẩm \"" + request.getOrderItem().getProductName() + "\"");
        return request;
    }

    // ----------------------------------------------------------------------------------
    // Admin (use case 3.22)
    // ----------------------------------------------------------------------------------

    /** Mọi yêu cầu bảo hành, mới nhất trước; status = null thì không lọc. */
    public static List<WarrantyRequest> selectAll(WarrantyStatus status) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            String jpql = "SELECT w FROM WarrantyRequest w"
                    + (status == null ? "" : " WHERE w.status = :status")
                    + " ORDER BY w.createdAt DESC";
            TypedQuery<WarrantyRequest> q = em.createQuery(jpql, WarrantyRequest.class);
            if (status != null) {
                q.setParameter("status", status);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public static WarrantyRequest selectById(String warrantyId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            return em.find(WarrantyRequest.class, warrantyId);
        } finally {
            em.close();
        }
    }

    /**
     * Admin phê duyệt (thay mới) hoặc từ chối yêu cầu đang chờ. Từ chối bắt buộc có lý do.
     * Sau khi lưu sẽ thông báo quyết định cho khách.
     */
    public static WarrantyRequest decide(String warrantyId, boolean approve, String note) {
        String cleanNote = note == null ? "" : note.trim();
        if (!approve && cleanNote.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối");
        }
        if (cleanNote.length() > 500) {
            throw new IllegalArgumentException("Ghi chú quá dài (tối đa 500 ký tự)");
        }

        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        WarrantyRequest request;
        try {
            request = em.find(WarrantyRequest.class, warrantyId, LockModeType.PESSIMISTIC_WRITE);
            if (request == null) {
                throw new IllegalArgumentException("Không tìm thấy yêu cầu bảo hành");
            }
            if (request.getStatus() != WarrantyStatus.PENDING) {
                throw new IllegalArgumentException("Yêu cầu này đã được xử lý");
            }
            request.setStatus(approve ? WarrantyStatus.APPROVED : WarrantyStatus.REJECTED);
            request.setAdminNote(cleanNote.isEmpty() ? null : cleanNote);
            request.setResolvedAt(LocalDateTime.now());
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
            throw new IllegalStateException("Không thể xử lý yêu cầu bảo hành, vui lòng thử lại", e);
        } finally {
            em.close();
        }

        String content = approve
                ? "Yêu cầu bảo hành #" + request.getShortCode() + " đã được duyệt, sản phẩm sẽ được thay mới"
                : "Yêu cầu bảo hành #" + request.getShortCode() + " bị từ chối. Lý do: " + cleanNote;
        notifyAccount(request.getAccountId(), content);
        return request;
    }

    // ----------------------------------------------------------------------------------

    private static OrderItem findOwnedItem(EntityManager em, String accountId, String orderItemId) {
        TypedQuery<OrderItem> q = em.createQuery(
                "SELECT i FROM OrderItem i WHERE i.orderItemId = :id AND i.order.accountId = :accountId",
                OrderItem.class);
        q.setParameter("id", orderItemId);
        q.setParameter("accountId", accountId);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private static long countPending(EntityManager em, OrderItem item) {
        TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(w) FROM WarrantyRequest w WHERE w.orderItem = :item AND w.status = :status",
                Long.class);
        q.setParameter("item", item);
        q.setParameter("status", WarrantyStatus.PENDING);
        return q.getSingleResult();
    }

    /** Thông báo cho mọi Admin qua DAO của Nhóm 2; lỗi thông báo không làm hỏng thao tác chính. */
    private static void notifyAdmins(String content) {
        for (User admin : UserDB.selectByRole(UserRole.ADMIN)) {
            notifyAccount(admin.getAccountId(), content);
        }
    }

    private static void notifyAccount(String accountId, String content) {
        Notification notification = new Notification();
        notification.setAccountId(accountId);
        notification.setType(NotificationType.WARRANTY_UPDATED);
        notification.setContent(content);
        NotificationDB.insert(notification);
    }
}
