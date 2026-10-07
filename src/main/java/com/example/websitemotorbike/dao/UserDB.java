package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class UserDB {

    public static boolean insert(User user){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try{
            em.persist(user);
            trans.commit();
            return true;
        }
        catch (Exception e){
            System.out.println(e);
            if(trans.isActive()){
                trans.rollback();
            }
            return false;
        }
        finally {
            em.close();
        }
    }

    public static User selectUser(String email){
        if (email == null) {
            return null;
        }
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        String qString = "SELECT u FROM User u where LOWER(u.email) = :email";
        TypedQuery<User> q = em.createQuery(qString, User.class);
        q.setParameter("email", email.trim().toLowerCase());
        q.setMaxResults(1);
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

    public static User selectByAccountId(String accountId){
        if (accountId == null) {
            return null;
        }
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        try {
            return em.find(User.class, accountId);
        }
        finally {
            em.close();
        }
    }

    public static boolean emailExists(String email){

        return selectUser(email) != null;
    }

    public static void update(User user){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try{
            em.merge(user);
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

    public static void delete(User user){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try{
            em.remove(em.merge(user));
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
