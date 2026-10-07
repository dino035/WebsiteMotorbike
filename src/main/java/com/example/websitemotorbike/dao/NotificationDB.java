package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.Notification;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class NotificationDB {

    public static void insert(Notification notification) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            em.persist(notification);
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

    public static List<Notification> selectByAccountId(String accountId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<Notification> q = em.createQuery(
                    "SELECT n FROM Notification n WHERE n.accountId = :accountId ORDER BY n.createdAt DESC",
                    Notification.class);
            q.setParameter("accountId", accountId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public static long countUnread(String accountId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<Long> q = em.createQuery(
                    "SELECT COUNT(n) FROM Notification n WHERE n.accountId = :accountId AND n.isRead = false",
                    Long.class);
            q.setParameter("accountId", accountId);
            return q.getSingleResult();
        } finally {
            em.close();
        }
    }

    /** Chỉ đánh dấu đã đọc nếu thông báo thuộc về accountId (chống sửa thông báo người khác). */
    public static void markAsRead(String accountId, String notificationId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            Notification n = em.find(Notification.class, notificationId);
            if (n != null && accountId.equals(n.getAccountId())) {
                n.setIsRead(true);
            }
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

    public static void markAllAsRead(String accountId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            em.createQuery("UPDATE Notification n SET n.isRead = true "
                            + "WHERE n.accountId = :accountId AND n.isRead = false")
                    .setParameter("accountId", accountId)
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
}