package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.CartItem;
import com.example.websitemotorbike.model.ShoppingCart;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

public class CartDB {

    /** Thời gian tạm giữ một chiếc xe khi nó nằm trong giỏ của một khách (phút). */
    public static final int HOLD_MINUTES = 30;

    public enum AddResult {
        ADDED,
        UPDATED,
        HELD_BY_OTHER
    }

    public static List<CartItem> selectItems(String accountId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<CartItem> q = em.createQuery(
                    "SELECT i FROM CartItem i WHERE i.cart.accountId = :accountId ORDER BY i.addedAt",
                    CartItem.class);
            q.setParameter("accountId", accountId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public static CartItem selectItem(String accountId, String cartItemId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            return findOwnedItem(em, accountId, cartItemId);
        } finally {
            em.close();
        }
    }

    /**
     * Thêm sản phẩm vào giỏ của accountId (tạo giỏ nếu chưa có).
     * unique = true: sản phẩm độc bản (xe) - luôn số lượng 1 và bị tạm giữ HOLD_MINUTES phút.
     * Trả HELD_BY_OTHER nếu một khách khác đang giữ xe này.
     */
    public static AddResult addItem(String accountId, String productId, int quantity,
                                    boolean unique, int holdMinutes) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            LocalDateTime now = LocalDateTime.now();
            ShoppingCart cart = findCart(em, accountId);
            if (cart == null) {
                cart = new ShoppingCart();
                cart.setAccountId(accountId);
                cart.setCreatedAt(now);
                em.persist(cart);
            }

            CartItem existing = findItemByProduct(em, cart, productId);

            if (unique) {
                if (countHeldByOthers(em, productId, accountId, now.minusMinutes(holdMinutes)) > 0) {
                    trans.rollback();
                    return AddResult.HELD_BY_OTHER;
                }
                if (existing != null) {
                    existing.setAddedAt(now);
                    trans.commit();
                    return AddResult.UPDATED;
                }
            } else if (existing != null) {
                existing.setQuantity(existing.getQuantity() + quantity);
                trans.commit();
                return AddResult.UPDATED;
            }

            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProductId(productId);
            item.setQuantity(unique ? 1 : quantity);
            item.setAddedAt(now);
            em.persist(item);
            trans.commit();
            return AddResult.ADDED;
        } catch (Exception e) {
            System.out.println(e);
            if (trans.isActive()) {
                trans.rollback();
            }
            throw new IllegalStateException("Không thể cập nhật giỏ hàng, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /** Đặt số lượng mới cho một dòng; trả false nếu dòng không thuộc giỏ của accountId. */
    public static boolean updateQuantity(String accountId, String cartItemId, int quantity) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            CartItem item = findOwnedItem(em, accountId, cartItemId);
            if (item == null) {
                trans.rollback();
                return false;
            }
            item.setQuantity(quantity);
            trans.commit();
            return true;
        } catch (Exception e) {
            System.out.println(e);
            if (trans.isActive()) {
                trans.rollback();
            }
            throw new IllegalStateException("Không thể cập nhật giỏ hàng, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /** Xóa một dòng khỏi giỏ; trả false nếu dòng không thuộc giỏ của accountId. */
    public static boolean removeItem(String accountId, String cartItemId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            CartItem item = findOwnedItem(em, accountId, cartItemId);
            if (item == null) {
                trans.rollback();
                return false;
            }
            em.remove(item);
            trans.commit();
            return true;
        } catch (Exception e) {
            System.out.println(e);
            if (trans.isActive()) {
                trans.rollback();
            }
            throw new IllegalStateException("Không thể cập nhật giỏ hàng, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /** Có khách khác (không phải accountId) đang giữ sản phẩm này không (còn trong thời hạn)? */
    public static boolean isHeldByOther(String productId, String accountId, int holdMinutes) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            return countHeldByOthers(em, productId, accountId,
                    LocalDateTime.now().minusMinutes(holdMinutes)) > 0;
        } finally {
            em.close();
        }
    }

    private static ShoppingCart findCart(EntityManager em, String accountId) {
        TypedQuery<ShoppingCart> q = em.createQuery(
                "SELECT c FROM ShoppingCart c WHERE c.accountId = :accountId", ShoppingCart.class);
        q.setParameter("accountId", accountId);
        q.setMaxResults(1);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private static CartItem findItemByProduct(EntityManager em, ShoppingCart cart, String productId) {
        TypedQuery<CartItem> q = em.createQuery(
                "SELECT i FROM CartItem i WHERE i.cart = :cart AND i.productId = :productId",
                CartItem.class);
        q.setParameter("cart", cart);
        q.setParameter("productId", productId);
        q.setMaxResults(1);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /** Chỉ trả dòng nếu nó thuộc giỏ của accountId (chống sửa/xóa giỏ của người khác). */
    private static CartItem findOwnedItem(EntityManager em, String accountId, String cartItemId) {
        TypedQuery<CartItem> q = em.createQuery(
                "SELECT i FROM CartItem i WHERE i.cartItemId = :id AND i.cart.accountId = :accountId",
                CartItem.class);
        q.setParameter("id", cartItemId);
        q.setParameter("accountId", accountId);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /** Dùng chung cho OrderDB (cùng package) khi kiểm tra lại xe lúc đặt hàng. */
    static long countHeldByOthers(EntityManager em, String productId, String accountId,
                                  LocalDateTime cutoff) {
        TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(i) FROM CartItem i WHERE i.productId = :productId "
                        + "AND i.cart.accountId <> :accountId AND i.addedAt > :cutoff",
                Long.class);
        q.setParameter("productId", productId);
        q.setParameter("accountId", accountId);
        q.setParameter("cutoff", cutoff);
        return q.getSingleResult();
    }
}
