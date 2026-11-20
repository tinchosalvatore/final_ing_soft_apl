package com.trackergastos;

import com.trackergastos.domain.Categoria;
import com.trackergastos.domain.Gasto;
import com.trackergastos.domain.User;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Datos de prueba de los tests de integración de las specs 001 y 002 (agente testing).
 * Crea usuarios, categorías y gastos directamente con el EntityManager, sin pasar por la lógica bajo prueba.
 */
public final class DatosPruebaGastos {

    /** "Hoy" según la spec 001: zona America/Argentina/Buenos_Aires. */
    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    private DatosPruebaGastos() {}

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    public static User usuario(EntityManager em, String login) {
        User u = new User();
        u.setLogin(login);
        u.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        u.setActivated(true);
        u.setEmail(login + "@prueba.local");
        u.setLangKey("es");
        em.persist(u);
        em.flush();
        return u;
    }

    /** Devuelve la categoría con ese nombre (sin distinguir mayúsculas) o la crea si no existe. */
    public static Categoria categoria(EntityManager em, String nombre) {
        List<Categoria> existentes = em
            .createQuery("select c from Categoria c where lower(trim(c.nombre)) = lower(trim(:n))", Categoria.class)
            .setParameter("n", nombre)
            .getResultList();
        if (!existentes.isEmpty()) {
            return existentes.getFirst();
        }
        Categoria c = new Categoria().nombre(nombre);
        em.persist(c);
        em.flush();
        return c;
    }

    public static Gasto gasto(EntityManager em, User dueno, Categoria categoria, String monto, LocalDate fecha) {
        Gasto g = new Gasto().monto(new BigDecimal(monto)).fecha(fecha);
        g.setCategoria(categoria);
        g.setUser(dueno);
        em.persist(g);
        em.flush();
        return g;
    }
}
