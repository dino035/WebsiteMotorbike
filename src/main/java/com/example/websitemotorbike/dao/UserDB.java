package com.example.websitemotorbike.dao;

import com.example.websitemotorbike.model.User;
import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class UserDB {

    public static void insert(User user){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try{
            em.persist(user);
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

    public static User selectUser(String email){
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        String qString = "SELECT u FROM User u where u.email = :email";
        TypedQuery<User> q = em.createQuery(qString, User.class);
        q.setParameter("email", email);
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
