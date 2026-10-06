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
}