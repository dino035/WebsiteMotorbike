package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.OrderStatus;
import com.example.websitemotorbike.model.ProductReview;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class ProductReviewDB {

    public static void insert(ProductReview review) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            em.persist(review);
            trans.commit();
        } catch (Exception e) {
            System.out.println(e);
            if (trans.isActive()) {
                trans.rollback();
            }
            throw new IllegalStateException("Không thể lưu đánh giá, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /** Đánh giá của một sản phẩm, mới nhất trước. */
    public static List<ProductReview> selectByProductId(String productId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<ProductReview> q = em.createQuery(
                    "SELECT r FROM ProductReview r WHERE r.productId = :productId ORDER BY r.createdAt DESC",
                    ProductReview.class);
            q.setParameter("productId", productId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Điểm trung bình, hoặc null nếu chưa có đánh giá nào. */
    public static Double averageRating(String productId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<Double> q = em.createQuery(
                    "SELECT AVG(r.rating) FROM ProductReview r WHERE r.productId = :productId",
                    Double.class);
            q.setParameter("productId", productId);
            return q.getSingleResult();
        } finally {
            em.close();
        }
    }

    public static boolean hasReviewed(String accountId, String productId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<Long> q = em.createQuery(
                    "SELECT COUNT(r) FROM ProductReview r "
                            + "WHERE r.accountId = :accountId AND r.productId = :productId",
                    Long.class);
            q.setParameter("accountId", accountId);
            q.setParameter("productId", productId);
            return q.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }

    /** Verified Purchase: có đơn COMPLETED của tài khoản này chứa sản phẩm này. */
    public static boolean hasPurchased(String accountId, String productId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<Long> q = em.createQuery(
                    "SELECT COUNT(i) FROM OrderItem i "
                            + "WHERE i.order.accountId = :accountId AND i.productId = :productId "
                            + "AND i.order.status = :status",
                    Long.class);
            q.setParameter("accountId", accountId);
            q.setParameter("productId", productId);
            q.setParameter("status", OrderStatus.COMPLETED);
            return q.getSingleResult() > 0;
        } finally {
            em.close();
        }
    }
}