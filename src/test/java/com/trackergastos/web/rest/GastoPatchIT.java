package com.trackergastos.web.rest;

import static com.trackergastos.DatosPruebaGastos.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.trackergastos.IntegrationTest;
import com.trackergastos.domain.Categoria;
import com.trackergastos.domain.Gasto;
import com.trackergastos.domain.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edición parcial (PATCH) y largo de la descripción (spec 001, regla 2 y criterios 001-CA02, CA03 y CA04 "al editar").
 * Hallazgos O1 y O3 del verify de la 001. Contrato (§5.1): 400 con {@code fieldErrors[].field}.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser("ana")
class GastoPatchIT {

    private static final String API = "/api/gastos";
    private static final String API_ID = API + "/{id}";
    private static final String MERGE_PATCH = "application/merge-patch+json";
    private static final String DESCRIPCION_141 = "x".repeat(141);
    private static final String DESCRIPCION_140 = "x".repeat(140);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManager em;

    private User ana;
    private User beto;
    private Categoria supermercado;

    @BeforeEach
    void setUp() {
        ana = usuario(em, "ana");
        beto = usuario(em, "beto");
        supermercado = categoria(em, "Supermercado");
    }

    private Gasto recargar(Gasto g) {
        em.flush();
        em.clear();
        return em.find(Gasto.class, g.getId());
    }

    private long cantidadDeGastos() {
        return em.createQuery("select count(g) from Gasto g", Long.class).getSingleResult();
    }

    private String cuerpoCompleto(Long id, String descripcion) {
        return (
            "{" +
            (id == null ? "" : "\"id\":" + id + ",") +
            "\"monto\":100,\"fecha\":\"" +
            hoy() +
            "\",\"categoria\":{\"id\":" +
            supermercado.getId() +
            "},\"descripcion\":\"" +
            descripcion +
            "\"}"
        );
    }

    @ParameterizedTest(name = "001-CA02 monto inválido por PATCH: {0}")
    @ValueSource(strings = { "0", "-10", "12.345", "10000000.01" })
    @DisplayName("001-CA02 monto inválido por PATCH: 400 sobre el monto y el gasto no cambia")
    void ca02MontoInvalidoPorPatch(String monto) throws Exception {
        Gasto propio = gasto(em, ana, supermercado, "100", hoy());

        mvc.perform(
            patch(API_ID, propio.getId())
                .contentType(MERGE_PATCH)
                .content("{\"id\":" + propio.getId() + ",\"monto\":" + monto + "}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("monto")));

        assertThat(recargar(propio).getMonto()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("001-CA03 fecha futura por PATCH: 400 sobre la fecha y el gasto no cambia")
    void ca03FechaFuturaPorPatch() throws Exception {
        LocalDate original = hoy().minusDays(2);
        Gasto propio = gasto(em, ana, supermercado, "100", original);

        mvc.perform(
            patch(API_ID, propio.getId())
                .contentType(MERGE_PATCH)
                .content("{\"id\":" + propio.getId() + ",\"fecha\":\"" + hoy().plusDays(1) + "\"}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("fecha")));

        assertThat(recargar(propio).getFecha()).isEqualTo(original);
    }

    @Test
    @DisplayName("001-CA04 dueño forzado por PATCH: se edita pero el dueño sigue siendo ana")
    void ca04DuenoForzadoPorPatch() throws Exception {
        Gasto propio = gasto(em, ana, supermercado, "100", hoy());

        mvc.perform(
            patch(API_ID, propio.getId())
                .contentType(MERGE_PATCH)
                .content("{\"id\":" + propio.getId() + ",\"monto\":250,\"user\":{\"id\":" + beto.getId() + ",\"login\":\"beto\"}}")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.login").value("ana"));

        Gasto enBase = recargar(propio);
        assertThat(enBase.getMonto()).isEqualByComparingTo("250");
        assertThat(enBase.getUser().getLogin()).isEqualTo("ana");
    }

    @Test
    @DisplayName("001-regla2 descripción de 141 caracteres al crear: 400 sobre la descripción y no se crea nada")
    void descripcionLargaAlCrear() throws Exception {
        long antes = cantidadDeGastos();

        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(cuerpoCompleto(null, DESCRIPCION_141)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("descripcion")));

        assertThat(cantidadDeGastos()).isEqualTo(antes);
    }

    @Test
    @DisplayName("001-regla2 descripción de 141 caracteres por PUT: 400 sobre la descripción y el gasto no cambia")
    void descripcionLargaPorPut() throws Exception {
        Gasto propio = gasto(em, ana, supermercado, "100", hoy());

        mvc.perform(
            put(API_ID, propio.getId()).contentType(MediaType.APPLICATION_JSON).content(cuerpoCompleto(propio.getId(), DESCRIPCION_141))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("descripcion")));

        assertThat(recargar(propio).getDescripcion()).isNull();
    }

    @Test
    @DisplayName("001-regla2 descripción de 141 caracteres por PATCH (O1): 400 sobre la descripción, no 500")
    void descripcionLargaPorPatch() throws Exception {
        Gasto propio = gasto(em, ana, supermercado, "100", hoy());

        mvc.perform(
            patch(API_ID, propio.getId())
                .contentType(MERGE_PATCH)
                .content("{\"id\":" + propio.getId() + ",\"descripcion\":\"" + DESCRIPCION_141 + "\"}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("descripcion")));

        assertThat(recargar(propio).getDescripcion()).isNull();
    }

    @Test
    @DisplayName("001-regla2 borde: descripción de 140 caracteres por PATCH se acepta")
    void descripcion140PorPatch() throws Exception {
        Gasto propio = gasto(em, ana, supermercado, "100", hoy());

        mvc.perform(
            patch(API_ID, propio.getId())
                .contentType(MERGE_PATCH)
                .content("{\"id\":" + propio.getId() + ",\"descripcion\":\"" + DESCRIPCION_140 + "\"}")
        ).andExpect(status().isOk());

        assertThat(recargar(propio).getDescripcion()).hasSize(140);
    }
}
