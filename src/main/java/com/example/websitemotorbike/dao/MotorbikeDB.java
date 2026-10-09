package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.Motorbike;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import com.example.websitemotorbike.model.VehiclePhoto;
import com.example.websitemotorbike.model.VehicleConditionAssessment;

import java.util.ArrayList;
import java.util.List;

public class MotorbikeDB {
    public static void insert(Motorbike motorbike){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try{
            // Tách ảnh ra, ghi xe trước để dòng motorbike có mặt khi ghi vehiclephoto
            List<VehiclePhoto> photos = new ArrayList<>(motorbike.getPhotos());
            motorbike.getPhotos().clear();
            VehicleConditionAssessment assessment = motorbike.getConditionAssessment();
            motorbike.setConditionAssessment(null);

            em.persist(motorbike);
            em.flush();

            for (VehiclePhoto photo : photos) {
                motorbike.addPhoto(photo);
                em.persist(photo);
            }
            if (assessment != null) {
                motorbike.setConditionAssessment(assessment);
                em.persist(assessment);
            }
            trans.commit();
        }
        catch (Exception e){
            System.out.println(e);
            if(trans.isActive()){
                trans.rollback();
            }
            throw new IllegalStateException("Không thể lưu xe, vui lòng thử lại", e);
        }
        finally {
            em.close();
        }
    }

    public static Motorbike selectById(String productId){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        String qString = "SELECT u FROM Motorbike u where u.productId = :productId";
        TypedQuery<Motorbike> q = em.createQuery(qString, Motorbike.class);
        q.setParameter("productId", productId);
        try {
            return q.getSingleResult();
        }
        catch (NoResultException e){
            return null;
        }
        finally {
            em.close();
        }
    }

    public static void update(Motorbike motorbike){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try{
            em.merge(motorbike);
            trans.commit();
        }
        catch (Exception e){
            System.out.println(e);
            if(trans.isActive()){
                trans.rollback();
            }
            throw new IllegalStateException("Không thể cập nhật xe, vui lòng thử lại", e);
        }
        finally {
            em.close();
        }
    }

    public static void delete(Motorbike motorbike){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try{
            em.remove(em.merge(motorbike));
            trans.commit();
        }
        catch (Exception e){
            System.out.println(e);
            if(trans.isActive()){
                trans.rollback();
            }
        }
        finally {
            em.close();
        }
    }
}
