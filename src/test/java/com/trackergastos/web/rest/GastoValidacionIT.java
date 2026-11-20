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
 * Tests de integración de las validaciones de un gasto (spec 001, criterios 001-CA02 y CA03, y casos borde del §4).
 * Contrato (spec 001 §5.1): 400 con {@code fieldErrors[].field} = {@code monto} o {@code fecha}.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser("ana")
class GastoValidacionIT {

    private static final String API = "/api/gastos";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManager em;

    private User ana;
    private Categoria supermercado;

    @BeforeEach
    void setUp() {
        ana = usuario(em, "ana");
        supermercado = categoria(em, "Supermercado");
    }

    private String cuerpo(String monto, LocalDate fecha) {
        return "{\"monto\":" + monto + ",\"fecha\":\"" + fecha + "\",\"categoria\":{\"id\":" + supermercado.getId() + "}}";
    }

    private long cantidadDeGastos() {
        return em.createQuery("select count(g) from Gasto g", Long.class).getSingleResult();
    }

    @ParameterizedTest(name = "001-CA02 monto inválido al crear: {0}")
    @ValueSource(strings = { "0", "-10", "12.345", "10000000.01" })
    @DisplayName("001-CA02 monto inválido al crear")
    void ca02MontoInvalidoAlCrear(String monto) throws Exception {
        long antes = cantidadDeGastos();

        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(cuerpo(monto, hoy())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("monto")));

        assertThat(cantidadDeGastos()).isEqualTo(antes);
    }

    @ParameterizedTest(name = "001-CA02 monto inválido al editar: {0}")
    @ValueSource(strings = { "0", "-10", "12.345", "10000000.01" })
    @DisplayName("001-CA02 monto inválido al editar (mismas reglas del punto 2)")
    void ca02MontoInvalidoAlEditar(String monto) throws Exception {
        Gasto propio = gasto(em, ana, supermercado, "100", hoy());
        String body = "{\"id\":" + propio.getId() + "," + cuerpo(monto, hoy()).substring(1);

        mvc.perform(
            put(API + "/{id}", propio.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("monto")));

        em.flush();
        em.clear();
        assertThat(em.find(Gasto.class, propio.getId()).getMonto()).isEqualByComparingTo("100");
    }

    @ParameterizedTest(name = "001-CA02 borde válido: {0}")
    @ValueSource(strings = { "0.01", "10000000", "10000000.00" })
    @DisplayName("001-CA02 bordes válidos del monto (0,01 y 10.000.000)")
    void ca02BordesValidos(String monto) throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(cuerpo(monto, hoy()))).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("001-CA03 fecha futura (mañana en Buenos Aires): 400 sobre la fecha y no se crea nada")
    void ca03FechaFutura() throws Exception {
        long antes = cantidadDeGastos();

        mvc.perform(
            post(API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("100", hoy().plusDays(1)))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("fecha")));

        assertThat(cantidadDeGastos()).isEqualTo(antes);
    }

    @Test
    @DisplayName("001-CA03 fecha futura al editar: 400 sobre la fecha")
    void ca03FechaFuturaAlEditar() throws Exception {
        Gasto propio = gasto(em, ana, supermercado, "100", hoy());
        String body = "{\"id\":" + propio.getId() + "," + cuerpo("100", hoy().plusDays(1)).substring(1);

        mvc.perform(
            put(API + "/{id}", propio.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("fecha")));
    }
}
