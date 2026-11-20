package com.trackergastos.web.rest;

import static com.trackergastos.DatosPruebaGastos.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.trackergastos.IntegrationTest;
import com.trackergastos.domain.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.YearMonth;
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
 * Tests de integración del endpoint del resumen mensual (spec 002, sección 5.1 "Contrato observable").
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class ResumenResourceIT {

    private static final String API = "/api/resumen";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EntityManager em;

    @Test
    @WithMockUser("ana")
    @DisplayName("002-CA01 GET /api/resumen?mes devuelve total, cantidad y detalle con la forma del contrato")
    void ca01FormaDelResumen() throws Exception {
        User ana = usuario(em, "ana");
        gasto(em, ana, categoria(em, "Supermercado"), "1000", LocalDate.of(2026, 10, 5));
        gasto(em, ana, categoria(em, "Supermercado"), "500", LocalDate.of(2026, 10, 6));
        gasto(em, ana, categoria(em, "Transporte"), "500", LocalDate.of(2026, 10, 7));

        mvc.perform(get(API).param("mes", "2026-10").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mes").value("2026-10"))
            .andExpect(jsonPath("$.total").value(2000))
            .andExpect(jsonPath("$.cantidad").value(3))
            .andExpect(jsonPath("$.categorias[0].nombre").value("Supermercado"))
            .andExpect(jsonPath("$.categorias[0].total").value(1500))
            .andExpect(jsonPath("$.categorias[0].cantidad").value(2))
            .andExpect(jsonPath("$.categorias[0].participacion").value(75.0))
            .andExpect(jsonPath("$.categorias[1].nombre").value("Transporte"))
            .andExpect(jsonPath("$.categorias[1].participacion").value(25.0))
            .andExpect(jsonPath("$.diferencia").value(2000))
            .andExpect(jsonPath("$.variacionPorcentual").doesNotExist());
    }

    @ParameterizedTest(name = "002-CA10 mes inválido por API: {0}")
    @ValueSource(strings = { "2099-01", "1999-12", "2026-13", "abc" })
    @WithMockUser("ana")
    @DisplayName("002-CA10 mes inválido por API → 400 error.mesinvalido")
    void ca10MesInvalido(String mes) throws Exception {
        usuario(em, "ana");
        mvc.perform(get(API).param("mes", mes).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.mesinvalido"));
    }

    @Test
    @WithMockUser("ana")
    @DisplayName("002-CA11 sin parámetro mes devuelve el mes actual")
    void ca11MesPorDefecto() throws Exception {
        usuario(em, "ana");
        mvc.perform(get(API).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mes").value(YearMonth.from(hoy()).toString()));
    }

    @Test
    @DisplayName("002 sin sesión → 401")
    void sinSesion() throws Exception {
        mvc.perform(get(API).param("mes", "2026-10")).andExpect(status().isUnauthorized());
    }
}
