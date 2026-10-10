package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.VehicleProfile;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

public class VehicleProfileDB {

    /** Số hồ sơ xe tối đa mỗi tài khoản (giả định, cần chốt với nhóm). */
    public static final int MAX_PER_ACCOUNT = 5;

    /** Hồ sơ xe của tài khoản, cũ trước. */
    public static List<VehicleProfile> selectByAccountId(String accountId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<VehicleProfile> q = em.createQuery(
                    "SELECT v FROM VehicleProfile v WHERE v.accountId = :accountId ORDER BY v.createdAt",
                    VehicleProfile.class);
            q.setParameter("accountId", accountId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Chỉ trả hồ sơ nếu thuộc về accountId (chống xem hoặc sửa hồ sơ của người khác). */
    public static VehicleProfile selectOwned(String accountId, String profileId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            return findOwned(em, accountId, profileId);
        } finally {
            em.close();
        }
    }

    /**
     * Thêm hồ sơ xe. Biệt danh phải khác các hồ sơ khác của cùng khách (không phân biệt hoa thường).
     *
     * @throws IllegalArgumentException đủ số hồ sơ tối đa hoặc trùng biệt danh (đã rollback)
     * @throws IllegalStateException    lỗi CSDL (đã rollback)
     */
    public static VehicleProfile create(String accountId, String nickname, String brand, String model) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            lockAccount(em, accountId);   // tuần tự hóa các thao tác trên hồ sơ xe của cùng một tài khoản

            if (countOf(em, accountId) >= MAX_PER_ACCOUNT) {
                throw new IllegalArgumentException("Mỗi tài khoản chỉ lưu tối đa " + MAX_PER_ACCOUNT + " xe");
            }
            requireUniqueNickname(em, accountId, nickname, null);

            VehicleProfile profile = new VehicleProfile();
            profile.setAccountId(accountId);
            profile.setNickname(nickname);
            profile.setBrand(brand);
            profile.setModel(model);
            profile.setCreatedAt(LocalDateTime.now());
            em.persist(profile);
            trans.commit();
            return profile;
        } catch (IllegalArgumentException e) {
            rollback(trans);
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            rollback(trans);
            throw new IllegalStateException("Không thể lưu xe, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /** Sửa hồ sơ xe của chính khách. */
    public static void update(String accountId, String profileId, String nickname, String brand, String model) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            lockAccount(em, accountId);
            VehicleProfile profile = findOwned(em, accountId, profileId);
            if (profile == null) {
                throw new IllegalArgumentException("Không tìm thấy xe");
            }
            requireUniqueNickname(em, accountId, nickname, profileId);
            profile.setNickname(nickname);
            profile.setBrand(brand);
            profile.setModel(model);
            trans.commit();
        } catch (IllegalArgumentException e) {
            rollback(trans);
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            rollback(trans);
            throw new IllegalStateException("Không thể lưu xe, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /** Xóa hồ sơ xe của chính khách. */
    public static void delete(String accountId, String profileId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            lockAccount(em, accountId);
            VehicleProfile profile = findOwned(em, accountId, profileId);
            if (profile == null) {
                throw new IllegalArgumentException("Không tìm thấy xe");
            }
            em.remove(profile);
            trans.commit();
        } catch (IllegalArgumentException e) {
            rollback(trans);
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            rollback(trans);
            throw new IllegalStateException("Không thể xóa xe, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    // ----------------------------------------------------------------------------------

    /** Khóa dòng tài khoản (SELECT ... FOR UPDATE) để các thao tác trên hồ sơ xe của một khách chạy lần lượt. */
    private static void lockAccount(EntityManager em, String accountId) {
        em.createNativeQuery("SELECT accountid FROM users WHERE accountid = ?1 FOR UPDATE")
                .setParameter(1, accountId)
                .getResultList();
    }

    private static VehicleProfile findOwned(EntityManager em, String accountId, String profileId) {
        TypedQuery<VehicleProfile> q = em.createQuery(
                "SELECT v FROM VehicleProfile v WHERE v.profileId = :id AND v.accountId = :accountId",
                VehicleProfile.class);
        q.setParameter("id", profileId);
        q.setParameter("accountId", accountId);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private static long countOf(EntityManager em, String accountId) {
        TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(v) FROM VehicleProfile v WHERE v.accountId = :accountId", Long.class);
        q.setParameter("accountId", accountId);
        return q.getSingleResult();
    }

    /** exceptProfileId: hồ sơ đang sửa (được giữ nguyên biệt danh của chính nó), null khi thêm mới. */
    private static void requireUniqueNickname(EntityManager em, String accountId, String nickname,
                                              String exceptProfileId) {
        TypedQuery<VehicleProfile> q = em.createQuery(
                "SELECT v FROM VehicleProfile v WHERE v.accountId = :accountId "
                        + "AND LOWER(v.nickname) = :nickname", VehicleProfile.class);
        q.setParameter("accountId", accountId);
        q.setParameter("nickname", nickname.toLowerCase());
        for (VehicleProfile other : q.getResultList()) {
            if (!other.getProfileId().equals(exceptProfileId)) {
                throw new IllegalArgumentException("Bạn đã có xe với biệt danh này, vui lòng chọn biệt danh khác");
            }
        }
    }

    private static void rollback(EntityTransaction trans) {
        if (trans.isActive()) {
            trans.rollback();
        }
    }
}
