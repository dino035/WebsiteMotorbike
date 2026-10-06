package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.Product;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.util.List;
import com.example.websitemotorbike.model.ProductStatus;
import java.math.BigDecimal;

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
            if (product != null) {
                em.remove(product);
            }
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

    public static List<Product> search(String keyword, String type, ProductStatus status,
                                       BigDecimal minPrice, BigDecimal maxPrice) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT p FROM Product p WHERE 1 = 1");

            boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
            if (hasKeyword) {
                jpql.append(" AND LOWER(p.name) LIKE :keyword");
            }
            if ("Motorbike".equals(type)) {
                jpql.append(" AND TYPE(p) = Motorbike");
            } else if ("SparePart".equals(type)) {
                jpql.append(" AND TYPE(p) = SparePart");
            }
            if (status != null) {
                jpql.append(" AND p.status = :status");
            }
            if (minPrice != null) {
                jpql.append(" AND p.price >= :minPrice");
            }
            if (maxPrice != null) {
                jpql.append(" AND p.price <= :maxPrice");
            }
            jpql.append(" ORDER BY p.name");

            TypedQuery<Product> q = em.createQuery(jpql.toString(), Product.class);
            if (hasKeyword) {
                q.setParameter("keyword", "%" + keyword.trim().toLowerCase() + "%");
            }
            if (status != null) {
                q.setParameter("status", status);
            }
            if (minPrice != null) {
                q.setParameter("minPrice", minPrice);
            }
            if (maxPrice != null) {
                q.setParameter("maxPrice", maxPrice);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }
}
