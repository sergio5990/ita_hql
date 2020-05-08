package com.it.util;

import org.hibernate.Session;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class EMUtil {
    private static EntityManagerFactory emFactory = null;

    public static EntityManager getEntityManager() {
        if (emFactory == null) {
            emFactory = Persistence.createEntityManagerFactory("by.it");
        }
        return emFactory.createEntityManager();
    }

    public static Session getSession() {
        return getEntityManager().unwrap(Session.class);
    }

    public static void closeEMFactory() {
        emFactory.close();
    }
}
