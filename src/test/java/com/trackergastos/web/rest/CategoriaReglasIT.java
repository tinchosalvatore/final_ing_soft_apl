package com.trackergastos.web.rest;

import static com.trackergastos.DatosPruebaGastos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.trackergastos.IntegrationTest;
import com.trackergastos.domain.Categoria;
import com.trackergastos.security.AuthoritiesConstants;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests de integración de las reglas de categorías (spec 001, criterios 001-CA11 a CA14).
 * Contrato (spec 001 §5.1): USER que escribe → 403; duplicado → 400 error.nombreduplicado; en uso → 400 error.categoriaenuso.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class CategoriaReglasIT {

    private static final String API = "/api/categorias";
    private static final String API_ID = API + "/{id}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManager em;

    private long cantidadDeCategorias() {
        return em.createQuery("select count(c) from Categoria c", Long.class).getSingleResult();
    }

    @Test
    @WithMockUser(username = "ana", authorities = AuthoritiesConstants.USER)
    @DisplayName("001-CA11 USER intenta crear, renombrar y borrar una categoría: 403 y nada cambia")
    void ca11UserGestionaCategorias() throws Exception {
        Categoria existente = categoria(em, "Ocio");
        long antes = cantidadDeCategorias();

        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Mascotas\"}")).andExpect(
            status().isForbidden()
        );
        mvc.perform(
            put(API_ID, existente.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + existente.getId() + ",\"nombre\":\"Renombrada\"}")
        ).andExpect(status().isForbidden());
        mvc.perform(
            patch(API_ID, existente.getId())
                .contentType("application/merge-patch+json")
                .content("{\"id\":" + existente.getId() + ",\"nombre\":\"Renombrada\"}")
        ).andExpect(status().isForbidden());
        mvc.perform(delete(API_ID, existente.getId())).andExpect(status().isForbidden());

        em.flush();
        em.clear();
        assertThat(cantidadDeCategorias()).isEqualTo(antes);
        assertThat(em.find(Categoria.class, existente.getId()).getNombre()).isEqualTo("Ocio");
    }

    @Test
    @WithMockUser(username = "ana", authorities = AuthoritiesConstants.USER)
    @DisplayName("001-CA11 USER sí puede listar las categorías (punto 9)")
    void ca11UserListaCategorias() throws Exception {
        categoria(em, "Ocio");
        mvc.perform(get(API))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].nombre").value(hasItems("Ocio")));
    }

    @Test
    @WithMockUser(username = "admin", authorities = { AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER })
    @DisplayName("001-CA12 nombre duplicado sin distinguir mayúsculas ni espacios: \" supermercado \" se rechaza")
    void ca12NombreDuplicado() throws Exception {
        categoria(em, "Supermercado");
        long antes = cantidadDeCategorias();

        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\" supermercado \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.nombreduplicado"));

        assertThat(cantidadDeCategorias()).isEqualTo(antes);
    }

    @Test
    @WithMockUser(username = "admin", authorities = { AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER })
    @DisplayName("001-CA12 renombrar a un nombre que ya existe (solo cambia mayúsculas) también se rechaza")
    void ca12RenombrarADuplicado() throws Exception {
        categoria(em, "Supermercado");
        Categoria otra = categoria(em, "Mascotas");

        mvc.perform(
            put(API_ID, otra.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + otra.getId() + ",\"nombre\":\"SUPERMERCADO\"}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.nombreduplicado"));

        em.flush();
        em.clear();
        assertThat(em.find(Categoria.class, otra.getId()).getNombre()).isEqualTo("Mascotas");
    }

    @Test
    @WithMockUser(username = "admin", authorities = { AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER })
    @DisplayName("001-CA13 borrar categoría en uso: 400 error.categoriaenuso y la categoría sigue existiendo")
    void ca13BorrarCategoriaEnUso() throws Exception {
        Categoria supermercado = categoria(em, "Supermercado");
        gasto(em, usuario(em, "ana"), supermercado, "100", hoy());

        mvc.perform(delete(API_ID, supermercado.getId()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.categoriaenuso"));

        em.flush();
        em.clear();
        assertThat(em.find(Categoria.class, supermercado.getId())).isNotNull();
    }

    @Test
    @WithMockUser(username = "admin", authorities = { AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER })
    @DisplayName("001-CA13 borde: borrar una categoría sin gastos se permite")
    void ca13BorrarCategoriaSinGastos() throws Exception {
        Categoria sinUso = categoria(em, "Mascotas");

        mvc.perform(delete(API_ID, sinUso.getId())).andExpect(status().isNoContent());

        em.flush();
        em.clear();
        assertThat(em.find(Categoria.class, sinUso.getId())).isNull();
    }

    @Test
    @WithMockUser(username = "ana", authorities = AuthoritiesConstants.USER)
    @DisplayName("001-CA14 categorías iniciales: existen las 6 del punto 13")
    void ca14CategoriasIniciales() throws Exception {
        // No crea datos: verifica lo que carga Liquibase al arrancar (también en el contexto de test).
        mvc.perform(get(API))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].nombre").value(hasItems("Supermercado", "Transporte", "Servicios", "Salud", "Ocio", "Otros")));
    }
}
