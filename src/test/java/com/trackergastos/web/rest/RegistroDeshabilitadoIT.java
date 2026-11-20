package com.trackergastos.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackergastos.IntegrationTest;
import com.trackergastos.domain.User;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro público deshabilitado, como en producción (spec 001, regla 16 y criterio 001-CA16).
 * Contrato asumido para el coder: la propiedad {@code application.registro.habilitado} (por defecto {@code true}
 * en dev y test; {@code false} en prod). Ver docs/specs/001-gastos/tasks.md.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "application.registro.habilitado=false")
@WithUnauthenticatedMockUser
class RegistroDeshabilitadoIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManager em;

    private long cantidadDeUsuarios() {
        return em.createQuery("select count(u) from User u", Long.class).getSingleResult();
    }

    private static String registro(String login, String email) {
        return "{\"login\":\"" + login + "\",\"email\":\"" + email + "\",\"password\":\"Otra-Clave-2026\",\"langKey\":\"es\"}";
    }

    @Test
    @DisplayName("001-CA16 registro deshabilitado: el login de una cuenta desactivada → 401 y la cuenta queda intacta")
    void ca16NoReemplazaCuentaDesactivada() throws Exception {
        User desactivada = new User();
        desactivada.setLogin("desactivada");
        desactivada.setPassword(RandomStringUtils.insecure().nextAlphanumeric(60));
        desactivada.setActivated(false);
        desactivada.setActivationKey("clave-original");
        desactivada.setEmail("desactivada@prueba.local");
        desactivada.setLangKey("es");
        em.persist(desactivada);
        em.flush();
        Long id = desactivada.getId();
        String passwordOriginal = desactivada.getPassword();
        long antes = cantidadDeUsuarios();

        mvc.perform(
            post("/api/register").contentType(MediaType.APPLICATION_JSON).content(registro("desactivada", "otro@prueba.local"))
        ).andExpect(status().isUnauthorized());

        em.flush();
        em.clear();
        User enBase = em.find(User.class, id);
        assertThat(enBase).as("la cuenta original sigue existiendo").isNotNull();
        assertThat(enBase.getLogin()).isEqualTo("desactivada");
        assertThat(enBase.getEmail()).isEqualTo("desactivada@prueba.local");
        assertThat(enBase.getPassword()).isEqualTo(passwordOriginal);
        assertThat(enBase.getActivationKey()).isEqualTo("clave-original");
        assertThat(enBase.isActivated()).isFalse();
        assertThat(cantidadDeUsuarios()).as("no se crea ningún usuario").isEqualTo(antes);
    }

    @Test
    @DisplayName("001-CA16 registro deshabilitado: un login nuevo → 401 y no se crea ningún usuario")
    void ca16NoCreaUsuarioNuevo() throws Exception {
        long antes = cantidadDeUsuarios();

        mvc.perform(
            post("/api/register").contentType(MediaType.APPLICATION_JSON).content(registro("nuevo-ca16", "nuevo-ca16@prueba.local"))
        ).andExpect(status().isUnauthorized());

        assertThat(cantidadDeUsuarios()).isEqualTo(antes);
    }
}
