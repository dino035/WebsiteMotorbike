package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.SparePart;
import com.example.websitemotorbike.model.SparePartPhoto;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.List;

public class SparePartDB {
    public static void insert(SparePart sparePart){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try{
            // Tách ảnh ra, ghi phụ tùng trước để dòng sparepart có mặt khi ghi sparepartphoto
            List<SparePartPhoto> photos = new ArrayList<>(sparePart.getPhotos());
            sparePart.getPhotos().clear();

            em.persist(sparePart);
            em.flush();

            for (SparePartPhoto photo : photos) {
                sparePart.addPhoto(photo);
                em.persist(photo);
            }
            trans.commit();
        }
        catch (Exception e){
            System.out.println(e);
            if(trans.isActive()){
                trans.rollback();
            }
            throw new IllegalStateException("Không thể lưu phụ tùng, vui lòng thử lại", e);
        }
        finally {
            em.close();
        }
    }

    public static SparePart selectById(String productId){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        String qString = "SELECT u FROM SparePart u where u.productId = :productId";
        TypedQuery<SparePart> q = em.createQuery(qString, SparePart.class);
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

    public static void update(SparePart sparePart){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try{
            em.merge(sparePart);
            trans.commit();
        }
        catch (Exception e){
            System.out.println(e);
            if(trans.isActive()){
                trans.rollback();
            }
            throw new IllegalStateException("Không thể cập nhật phụ tùng, vui lòng thử lại", e);
        }
        finally {
            em.close();
        }
    }

    public static void delete(SparePart sparePart){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try{
            em.remove(em.merge(sparePart));
            trans.commit();
        }
        catch (Exception e){
            System.out.println(e);
            if(trans.isActive()){
                trans.rollback();
            }
            throw new IllegalStateException("Không thể xóa phụ tùng, vui lòng thử lại", e);
        }
        finally {
            em.close();
        }
    }
}
