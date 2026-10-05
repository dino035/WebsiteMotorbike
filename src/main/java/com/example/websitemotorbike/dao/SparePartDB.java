package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.SparePart;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class SparePartDB {
    public static void insert(SparePart sparePart){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try{
            em.persist(sparePart);
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
        }
        finally {
            em.close();
        }
    }
}
