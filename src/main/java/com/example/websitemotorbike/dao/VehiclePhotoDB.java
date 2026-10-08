package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.VehiclePhoto;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import com.example.websitemotorbike.model.Motorbike;
import jakarta.persistence.EntityTransaction;

import java.util.List;

public class VehiclePhotoDB {

    /** Ảnh của một xe. */
    public static List<VehiclePhoto> selectByMotorbikeId(String motorbikeId) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            TypedQuery<VehiclePhoto> q = em.createQuery(
                    "SELECT p FROM VehiclePhoto p WHERE p.motorbike.productId = :id",
                    VehiclePhoto.class);
            q.setParameter("id", motorbikeId);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Thêm và xóa ảnh của một xe trong cùng một transaction. */
    public static void applyChanges(String motorbikeId, List<VehiclePhoto> toAdd,
                                    List<String> removeIds) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            Motorbike motorbike = em.find(Motorbike.class, motorbikeId);
            if (motorbike == null) {
                throw new IllegalStateException("Xe không tồn tại");
            }
            for (String photoId : removeIds) {
                VehiclePhoto photo = em.find(VehiclePhoto.class, photoId);
                // Chỉ xóa ảnh thật sự thuộc về xe này
                if (photo != null && photo.getMotorbike().getProductId().equals(motorbikeId)) {
                    motorbike.getPhotos().remove(photo);
                    em.remove(photo);
                }
            }
            for (VehiclePhoto photo : toAdd) {
                motorbike.addPhoto(photo);
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