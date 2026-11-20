package com.trackergastos.service.resumen;

import static com.trackergastos.DatosPruebaGastos.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.trackergastos.IntegrationTest;
import com.trackergastos.domain.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests de integración del resumen mensual contra Postgres (spec 002, criterios 002-CA07, CA08 y CA11).
 */
@IntegrationTest
@Transactional
class ResumenMensualServiceIT {

    @Autowired
    private EntityManager em;

    @Autowired
    private ResumenMensualService resumenMensualService;

    private User ana;
    private User beto;

    @BeforeEach
    void setUp() {
        ana = usuario(em, "ana");
        beto = usuario(em, "beto");
    }

    @Test
    @WithMockUser("ana")
    @DisplayName("002-CA08 aislamiento: el resumen de ana no incluye nada de beto")
    void ca08Aislamiento() {
        gasto(em, ana, categoria(em, "Supermercado"), "1000", LocalDate.of(2026, 10, 5));
        gasto(em, ana, categoria(em, "Transporte"), "500", LocalDate.of(2026, 10, 6));
        gasto(em, beto, categoria(em, "Supermercado"), "7000", LocalDate.of(2026, 10, 5));
        gasto(em, beto, categoria(em, "Ocio"), "300", LocalDate.of(2026, 10, 7));
        gasto(em, beto, categoria(em, "Ocio"), "999", LocalDate.of(2026, 9, 7)); // mes anterior de beto

        ResumenMensualDTO r = resumenMensualService.obtenerResumen("2026-10");

        assertThat(r.getMes()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(r.getTotal()).isEqualByComparingTo("1500");
        assertThat(r.getCantidad()).isEqualTo(2);
        assertThat(r.getCategorias()).extracting(CategoriaResumenDTO::getNombre).containsExactly("Supermercado", "Transporte");
        assertThat(r.getCategorias().get(0).getTotal()).isEqualByComparingTo("1000");
        // el mes anterior de ana está en 0: los 999 de beto no cuentan
        assertThat(r.getDiferencia()).isEqualByComparingTo("1500");
        assertThat(r.getVariacionPorcentual()).isNull();
    }

    @Test
    @WithMockUser("ana")
    @DisplayName("002-CA07 límites del mes contra la base: cuenta el 28/02 y no el 01/03")
    void ca07LimitesDelMes() {
        gasto(em, ana, categoria(em, "Salud"), "100", LocalDate.of(2026, 2, 28));
        gasto(em, ana, categoria(em, "Salud"), "50", LocalDate.of(2026, 3, 1));

        ResumenMensualDTO r = resumenMensualService.obtenerResumen("2026-02");

        assertThat(r.getTotal()).isEqualByComparingTo("100");
        assertThat(r.getCantidad()).isEqualTo(1);
    }

    @Test
    @WithMockUser("ana")
    @DisplayName("002-CA11 sin mes se devuelve el resumen del mes actual")
    void ca11MesPorDefecto() {
        gasto(em, ana, categoria(em, "Ocio"), "250", hoy());

        ResumenMensualDTO r = resumenMensualService.obtenerResumen(null);

        assertThat(r.getMes()).isEqualTo(YearMonth.from(hoy()));
        assertThat(r.getTotal()).isEqualByComparingTo("250");
    }
}
