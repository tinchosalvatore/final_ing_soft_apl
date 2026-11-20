package com.trackergastos.web.rest;

import static com.trackergastos.DatosPruebaGastos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.trackergastos.IntegrationTest;
import com.trackergastos.domain.Gasto;
import com.trackergastos.domain.User;
import com.trackergastos.security.AuthoritiesConstants;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Borrar un usuario que tiene gastos (spec 001, regla 17 y criterio 001-CA17).
 * Contrato (§5.1): {@code DELETE /api/admin/users/{login}} → 400 {@code error.usuariocongastos}; no se borra nada.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", authorities = { AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER })
class UsuarioConGastosIT {

    private static final String API = "/api/admin/users/{login}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("001-CA17 borrar un usuario con gastos: 400 error.usuariocongastos y siguen el usuario y sus gastos")
    void ca17BorrarUsuarioConGastos() throws Exception {
        User ana = usuario(em, "ana");
        Gasto g1 = gasto(em, ana, categoria(em, "Supermercado"), "100", hoy());
        Gasto g2 = gasto(em, ana, categoria(em, "Transporte"), "50", hoy().minusDays(1));

        mvc.perform(delete(API, "ana")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("error.usuariocongastos"));

        em.flush();
        em.clear();
        assertThat(em.find(User.class, ana.getId())).as("el usuario sigue existiendo").isNotNull();
        assertThat(em.find(Gasto.class, g1.getId())).as("sus gastos siguen existiendo").isNotNull();
        assertThat(em.find(Gasto.class, g2.getId())).isNotNull();
        assertThat(em.find(Gasto.class, g1.getId()).getUser().getLogin()).isEqualTo("ana");
    }

    @Test
    @DisplayName("001-CA17 borde: borrar un usuario sin gastos se permite")
    void ca17BorrarUsuarioSinGastos() throws Exception {
        User sinGastos = usuario(em, "sin-gastos");

        mvc.perform(delete(API, "sin-gastos")).andExpect(status().isNoContent());

        em.flush();
        em.clear();
        assertThat(em.find(User.class, sinGastos.getId())).isNull();
    }
}
