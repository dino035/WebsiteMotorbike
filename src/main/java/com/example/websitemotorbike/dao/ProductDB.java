package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class ProductDB {
    public static List<Product> selectAll(){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        String qString = "SELECT p FROM Product p";
        TypedQuery<Product> q = em.createQuery(qString, Product.class);
        try {
            return q.getResultList();
        }
        finally {
            em.close();
        }
    }

    public static void delete(String id){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try{
            Product product = em.find(Product.class, id);
            em.remove(product);
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

    public static Product selectById(String id){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try{
            Product product = em.find(Product.class, id);
            return product;
        }
        finally {
            em.close();
        }
    }
}
