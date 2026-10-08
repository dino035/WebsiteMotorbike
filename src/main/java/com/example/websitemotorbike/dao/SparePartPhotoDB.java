package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.SparePart;
import com.example.websitemotorbike.model.SparePartPhoto;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class SparePartPhotoDB {

    /** Ảnh của một phụ tùng. */
    public static List<SparePartPhoto> selectBySparePartId(String sparePartId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<SparePartPhoto> q = em.createQuery(
                    "SELECT p FROM SparePartPhoto p WHERE p.sparePart.productId = :id",
                    SparePartPhoto.class);
            q.setParameter("id", sparePartId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Thêm và xóa ảnh của một phụ tùng trong cùng một transaction. */
    public static void applyChanges(String sparePartId, List<SparePartPhoto> toAdd,
                                    List<String> removeIds) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            SparePart sparePart = em.find(SparePart.class, sparePartId);
            if (sparePart == null) {
                throw new IllegalStateException("Phụ tùng không tồn tại");
            }
            for (String photoId : removeIds) {
                SparePartPhoto photo = em.find(SparePartPhoto.class, photoId);
                // Chỉ xóa ảnh thật sự thuộc về phụ tùng này
                if (photo != null && photo.getSparePart().getProductId().equals(sparePartId)) {
                    sparePart.getPhotos().remove(photo);
                    em.remove(photo);
                }
            }
            for (SparePartPhoto photo : toAdd) {
                sparePart.addPhoto(photo);
                em.persist(photo);
            }
            trans.commit();
        } catch (Exception e) {
            System.out.println(e);
            if (trans.isActive()) {
                trans.rollback();
            }
            throw new IllegalStateException("Không thể cập nhật ảnh, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }
}