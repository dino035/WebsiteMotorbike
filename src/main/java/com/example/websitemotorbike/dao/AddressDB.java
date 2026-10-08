package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.Address;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.List;

public class AddressDB {

    /** Số địa chỉ tối đa mỗi tài khoản (giả định, cần chốt với nhóm). */
    public static final int MAX_PER_ACCOUNT = 10;

    /** Sổ địa chỉ của tài khoản: địa chỉ mặc định lên đầu, rồi theo thứ tự tạo. */
    public static List<Address> selectByAccountId(String accountId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<Address> q = em.createQuery(
                    "SELECT a FROM Address a WHERE a.accountId = :accountId "
                            + "ORDER BY a.defaultAddress DESC, a.createdAt", Address.class);
            q.setParameter("accountId", accountId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Chỉ trả địa chỉ nếu thuộc về accountId (chống xem hoặc sửa địa chỉ của người khác). */
    public static Address selectOwned(String accountId, String addressId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            return findOwned(em, accountId, addressId);
        } finally {
            em.close();
        }
    }

    /**
     * Thêm địa chỉ. Địa chỉ đầu tiên của tài khoản tự động là mặc định; chọn makeDefault thì địa chỉ này
     * trở thành mặc định và địa chỉ mặc định cũ bị bỏ.
     *
     * @throws IllegalArgumentException đã đủ số địa chỉ tối đa (đã rollback)
     * @throws IllegalStateException    lỗi CSDL (đã rollback)
     */
    public static Address create(String accountId, String receiverName, String receiverPhone,
                                 String addressLine, boolean makeDefault) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            lockAccount(em, accountId);   // tuần tự hóa các thao tác trên sổ địa chỉ của cùng một tài khoản

            long count = countOf(em, accountId);
            if (count >= MAX_PER_ACCOUNT) {
                throw new IllegalArgumentException("Mỗi tài khoản chỉ lưu tối đa " + MAX_PER_ACCOUNT + " địa chỉ");
            }
            boolean isDefault = makeDefault || count == 0;
            if (isDefault) {
                clearDefault(em, accountId);
            }

            Address address = new Address();
            address.setAccountId(accountId);
            address.setReceiverName(receiverName);
            address.setReceiverPhone(receiverPhone);
            address.setAddressLine(addressLine);
            address.setDefaultAddress(isDefault);
            address.setCreatedAt(LocalDateTime.now());
            em.persist(address);
            trans.commit();
            return address;
        } catch (IllegalArgumentException e) {
            rollback(trans);
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            rollback(trans);
            throw new IllegalStateException("Không thể lưu địa chỉ, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /** Như create nhưng bỏ qua nếu tài khoản đã có địa chỉ y hệt (dùng khi lưu địa chỉ từ trang thanh toán). */
    public static void createIfAbsent(String accountId, String receiverName, String receiverPhone,
                                      String addressLine) {
        for (Address existing : selectByAccountId(accountId)) {
            if (existing.getReceiverName().equals(receiverName)
                    && existing.getReceiverPhone().equals(receiverPhone)
                    && existing.getAddressLine().equals(addressLine)) {
                return;
            }
        }
        create(accountId, receiverName, receiverPhone, addressLine, false);
    }

    /** Sửa một địa chỉ của chính khách; makeDefault = true thì đặt làm mặc định. */
    public static void update(String accountId, String addressId, String receiverName, String receiverPhone,
                              String addressLine, boolean makeDefault) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            lockAccount(em, accountId);
            Address address = findOwned(em, accountId, addressId);
            if (address == null) {
                throw new IllegalArgumentException("Không tìm thấy địa chỉ");
            }
            if (makeDefault && !address.isDefaultAddress()) {
                clearDefault(em, accountId);
                address.setDefaultAddress(true);
            }
            address.setReceiverName(receiverName);
            address.setReceiverPhone(receiverPhone);
            address.setAddressLine(addressLine);
            trans.commit();
        } catch (IllegalArgumentException e) {
            rollback(trans);
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            rollback(trans);
            throw new IllegalStateException("Không thể lưu địa chỉ, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /** Đặt một địa chỉ làm mặc định. */
    public static void setDefault(String accountId, String addressId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            lockAccount(em, accountId);
            Address address = findOwned(em, accountId, addressId);
            if (address == null) {
                throw new IllegalArgumentException("Không tìm thấy địa chỉ");
            }
            if (!address.isDefaultAddress()) {
                clearDefault(em, accountId);
                address.setDefaultAddress(true);
            }
            trans.commit();
        } catch (IllegalArgumentException e) {
            rollback(trans);
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            rollback(trans);
            throw new IllegalStateException("Không thể cập nhật địa chỉ, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    /** Xóa một địa chỉ; nếu xóa địa chỉ mặc định mà còn địa chỉ khác thì địa chỉ cũ nhất còn lại thành mặc định. */
    public static void delete(String accountId, String addressId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            lockAccount(em, accountId);
            Address address = findOwned(em, accountId, addressId);
            if (address == null) {
                throw new IllegalArgumentException("Không tìm thấy địa chỉ");
            }
            boolean wasDefault = address.isDefaultAddress();
            em.remove(address);
            em.flush();

            if (wasDefault) {
                TypedQuery<Address> q = em.createQuery(
                        "SELECT a FROM Address a WHERE a.accountId = :accountId ORDER BY a.createdAt", Address.class);
                q.setParameter("accountId", accountId);
                q.setMaxResults(1);
                List<Address> rest = q.getResultList();
                if (!rest.isEmpty()) {
                    rest.get(0).setDefaultAddress(true);
                }
            }
            trans.commit();
        } catch (IllegalArgumentException e) {
            rollback(trans);
            throw e;
        } catch (Exception e) {
            System.out.println(e);
            rollback(trans);
            throw new IllegalStateException("Không thể xóa địa chỉ, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }

    // ----------------------------------------------------------------------------------

    /** Khóa dòng tài khoản (SELECT ... FOR UPDATE) để các thao tác trên sổ địa chỉ của một khách chạy lần lượt. */
    private static void lockAccount(EntityManager em, String accountId) {
        em.createNativeQuery("SELECT accountid FROM users WHERE accountid = ?1 FOR UPDATE")
                .setParameter(1, accountId)
                .getResultList();
    }

    private static Address findOwned(EntityManager em, String accountId, String addressId) {
        TypedQuery<Address> q = em.createQuery(
                "SELECT a FROM Address a WHERE a.addressId = :id AND a.accountId = :accountId", Address.class);
        q.setParameter("id", addressId);
        q.setParameter("accountId", accountId);
        try {
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private static long countOf(EntityManager em, String accountId) {
        TypedQuery<Long> q = em.createQuery(
                "SELECT COUNT(a) FROM Address a WHERE a.accountId = :accountId", Long.class);
        q.setParameter("accountId", accountId);
        return q.getSingleResult();
    }

    private static void clearDefault(EntityManager em, String accountId) {
        em.createQuery("UPDATE Address a SET a.defaultAddress = false WHERE a.accountId = :accountId")
                .setParameter("accountId", accountId)
                .executeUpdate();
    }

    private static void rollback(EntityTransaction trans) {
        if (trans.isActive()) {
            trans.rollback();
        }
    }
}
