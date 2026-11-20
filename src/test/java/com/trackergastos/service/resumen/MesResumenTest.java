package com.trackergastos.service.resumen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests unitarios de la resolución y validación del mes del resumen (spec 002, criterios 002-CA10 y CA11).
 */
class MesResumenTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 20);

    @ParameterizedTest(name = "002-CA10 mes inválido: {0}")
    @ValueSource(strings = { "2099-01", "1999-12", "2026-13", "abc" })
    @DisplayName("002-CA10 mes inválido")
    void ca10MesInvalido(String mes) {
        assertThatThrownBy(() -> MesResumen.resolver(mes, HOY)).isInstanceOf(MesInvalidoException.class);
    }

    @Test
    @DisplayName("002-CA10 el mes siguiente al actual es inválido")
    void ca10MesSiguienteAlActual() {
        assertThatThrownBy(() -> MesResumen.resolver("2026-12", HOY)).isInstanceOf(MesInvalidoException.class);
    }

    @Test
    @DisplayName("002-CA10 los límites 2000-01 y el mes actual son válidos")
    void ca10LimitesValidos() {
        assertThat(MesResumen.resolver("2000-01", HOY)).isEqualTo(YearMonth.of(2000, 1));
        assertThat(MesResumen.resolver("2026-11", HOY)).isEqualTo(YearMonth.of(2026, 11));
    }

    @Test
    @DisplayName("002-CA11 sin mes se usa el mes actual")
    void ca11MesPorDefecto() {
        assertThat(MesResumen.resolver(null, HOY)).isEqualTo(YearMonth.of(2026, 11));
    }

    @Test
    @DisplayName("002-CA11 el mes actual depende de hoy (enero)")
    void ca11MesPorDefectoEnero() {
        assertThat(MesResumen.resolver(null, LocalDate.of(2027, 1, 1))).isEqualTo(YearMonth.of(2027, 1));
    }
}
