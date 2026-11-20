package com.trackergastos.service.resumen;

import static org.assertj.core.api.Assertions.assertThat;

import com.trackergastos.service.dto.CategoriaDTO;
import com.trackergastos.service.dto.GastoDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios del cálculo del resumen mensual (spec 002, criterios 002-CA01 a CA07 y CA09, más casos borde).
 * Escritos en la fase roja, a partir de docs/specs/002-resumen-mensual/spec.md.
 */
class ResumenMensualCalculadoraTest {

    private ResumenMensualCalculadora calculadora;

    @BeforeEach
    void setUp() {
        calculadora = new ResumenMensualCalculadora();
    }

    private static GastoDTO gasto(String monto, String fecha, String categoria) {
        CategoriaDTO c = new CategoriaDTO();
        c.setNombre(categoria);
        GastoDTO g = new GastoDTO();
        g.setMonto(new BigDecimal(monto));
        g.setFecha(LocalDate.parse(fecha));
        g.setCategoria(c);
        return g;
    }

    private static void assertFila(CategoriaResumenDTO fila, String nombre, String total, long cantidad, String participacion) {
        assertThat(fila.getNombre()).isEqualTo(nombre);
        assertThat(fila.getTotal()).isEqualByComparingTo(total);
        assertThat(fila.getCantidad()).isEqualTo(cantidad);
        assertThat(fila.getParticipacion()).isEqualByComparingTo(participacion);
    }

    @Test
    @DisplayName("002-CA01 total y detalle por categoría")
    void ca01TotalYDetalle() {
        List<GastoDTO> gastos = List.of(
            gasto("500", "2026-10-03", "Transporte"),
            gasto("1000", "2026-10-05", "Supermercado"),
            gasto("500", "2026-10-20", "Supermercado")
        );

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 10), gastos);

        assertThat(r.getMes()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(r.getTotal()).isEqualByComparingTo("2000");
        assertThat(r.getCantidad()).isEqualTo(3);
        assertThat(r.getCategorias()).hasSize(2);
        assertFila(r.getCategorias().get(0), "Supermercado", "1500", 2, "75.0");
        assertFila(r.getCategorias().get(1), "Transporte", "500", 1, "25.0");
    }

    @Test
    @DisplayName("002-CA02 suma exacta sin errores de redondeo")
    void ca02SumaExacta() {
        List<GastoDTO> gastos = List.of(gasto("0.10", "2026-10-01", "Otros"), gasto("0.20", "2026-10-02", "Otros"));

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 10), gastos);

        assertThat(r.getTotal()).isEqualByComparingTo("0.30");
        assertThat(r.getCategorias().get(0).getTotal()).isEqualByComparingTo("0.30");
    }

    @Test
    @DisplayName("002-CA03 redondeo de participación a 1 decimal (33,3 %)")
    void ca03RedondeoParticipacion() {
        List<GastoDTO> gastos = List.of(
            gasto("1", "2026-10-01", "Ocio"),
            gasto("1", "2026-10-02", "Salud"),
            gasto("1", "2026-10-03", "Transporte")
        );

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 10), gastos);

        assertThat(r.getTotal()).isEqualByComparingTo("3");
        assertThat(r.getCategorias()).hasSize(3);
        assertThat(r.getCategorias()).allSatisfy(fila -> assertThat(fila.getParticipacion()).isEqualByComparingTo("33.3"));
    }

    @Test
    @DisplayName("002-CA03 la participación redondea la mitad hacia arriba (12,25 → 12,3)")
    void ca03MitadHaciaArriba() {
        // 12,25 / 100 = 12,25 % → 12,3 (con redondeo bancario daría 12,2)
        List<GastoDTO> gastos = List.of(gasto("12.25", "2026-10-01", "Ocio"), gasto("87.75", "2026-10-02", "Supermercado"));

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 10), gastos);

        assertFila(r.getCategorias().get(0), "Supermercado", "87.75", 1, "87.8");
        assertFila(r.getCategorias().get(1), "Ocio", "12.25", 1, "12.3");
    }

    @Test
    @DisplayName("002-CA04 variación positiva contra el mes anterior")
    void ca04VariacionPositiva() {
        List<GastoDTO> gastos = List.of(
            gasto("1000", "2026-09-10", "Supermercado"),
            gasto("700", "2026-10-10", "Supermercado"),
            gasto("500", "2026-10-11", "Transporte")
        );

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 10), gastos);

        assertThat(r.getTotal()).isEqualByComparingTo("1200");
        assertThat(r.getDiferencia()).isEqualByComparingTo("200");
        assertThat(r.getVariacionPorcentual()).isEqualByComparingTo("20.0");
    }

    @Test
    @DisplayName("002-CA05 mes anterior en 0: diferencia informada, variación porcentual no informada")
    void ca05MesAnteriorEnCero() {
        List<GastoDTO> gastos = List.of(gasto("500", "2026-10-15", "Ocio"));

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 10), gastos);

        assertThat(r.getTotal()).isEqualByComparingTo("500");
        assertThat(r.getDiferencia()).isEqualByComparingTo("500");
        assertThat(r.getVariacionPorcentual()).isNull();
    }

    @Test
    @DisplayName("002-CA06 cambio de año: enero se compara contra diciembre del año anterior")
    void ca06CambioDeAnio() {
        List<GastoDTO> gastos = List.of(
            gasto("300", "2025-11-30", "Ocio"), // dos meses antes: no cuenta para la variación
            gasto("100", "2025-12-31", "Ocio"),
            gasto("150", "2026-01-01", "Ocio")
        );

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 1), gastos);

        assertThat(r.getTotal()).isEqualByComparingTo("150");
        assertThat(r.getDiferencia()).isEqualByComparingTo("50");
        assertThat(r.getVariacionPorcentual()).isEqualByComparingTo("50.0");
    }

    @Test
    @DisplayName("002-CA07 límites del mes: cuenta el 28/02 y no el 01/03")
    void ca07LimitesDelMes() {
        List<GastoDTO> gastos = List.of(gasto("100", "2026-02-28", "Salud"), gasto("50", "2026-03-01", "Salud"));

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 2), gastos);

        assertThat(r.getTotal()).isEqualByComparingTo("100");
        assertThat(r.getCantidad()).isEqualTo(1);
        assertThat(r.getCategorias()).hasSize(1);
        assertFila(r.getCategorias().get(0), "Salud", "100", 1, "100.0");
    }

    @Test
    @DisplayName("002-CA07 límites del mes: el día 1 cuenta y el último día del mes anterior no")
    void ca07PrimerDiaDelMes() {
        List<GastoDTO> gastos = List.of(gasto("80", "2026-01-31", "Salud"), gasto("40", "2026-02-01", "Salud"));

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 2), gastos);

        assertThat(r.getTotal()).isEqualByComparingTo("40");
        assertThat(r.getCantidad()).isEqualTo(1);
    }

    @Test
    @DisplayName("002-CA09 empate de categorías: se ordena por nombre (Ocio antes que Salud)")
    void ca09EmpateDeCategorias() {
        List<GastoDTO> gastos = List.of(gasto("300", "2026-10-01", "Salud"), gasto("300", "2026-10-02", "Ocio"));

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 10), gastos);

        assertThat(r.getCategorias()).extracting(CategoriaResumenDTO::getNombre).containsExactly("Ocio", "Salud");
    }

    @Test
    @DisplayName("002-CA09 empate: orden alfabético del español sin distinguir mayúsculas ni acentos")
    void ca09EmpateOrdenEspanol() {
        // Con orden por código Unicode quedaría "Peluquería", "almacén", "Óptica" (mayúsculas antes, Ó al final).
        List<GastoDTO> gastos = List.of(
            gasto("100", "2026-10-01", "Peluquería"),
            gasto("100", "2026-10-02", "Óptica"),
            gasto("100", "2026-10-03", "almacén")
        );

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 10), gastos);

        assertThat(r.getCategorias()).extracting(CategoriaResumenDTO::getNombre).containsExactly("almacén", "Óptica", "Peluquería");
    }

    @Test
    @DisplayName("002-borde mes sin gastos con mes anterior con gastos: total 0 y variación -100 %")
    void bordeMesSinGastos() {
        List<GastoDTO> gastos = List.of(gasto("400", "2026-09-10", "Ocio"));

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 10), gastos);

        assertThat(r.getTotal()).isEqualByComparingTo("0");
        assertThat(r.getCantidad()).isZero();
        assertThat(r.getCategorias()).isEmpty();
        assertThat(r.getDiferencia()).isEqualByComparingTo("-400");
        assertThat(r.getVariacionPorcentual()).isEqualByComparingTo("-100.0");
    }

    @Test
    @DisplayName("002-borde mes y mes anterior sin gastos: diferencia 0 y variación no informada")
    void bordeAmbosMesesSinGastos() {
        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 10), List.of());

        assertThat(r.getTotal()).isEqualByComparingTo("0");
        assertThat(r.getCantidad()).isZero();
        assertThat(r.getCategorias()).isEmpty();
        assertThat(r.getDiferencia()).isEqualByComparingTo("0");
        assertThat(r.getVariacionPorcentual()).isNull();
    }

    @Test
    @DisplayName("002-borde variación negativa redondeada a 1 decimal")
    void bordeVariacionNegativa() {
        // (200 - 300) / 300 × 100 = -33,333... → -33,3
        List<GastoDTO> gastos = List.of(gasto("300", "2026-09-10", "Ocio"), gasto("200", "2026-10-10", "Ocio"));

        ResumenMensualDTO r = calculadora.calcular(YearMonth.of(2026, 10), gastos);

        assertThat(r.getDiferencia()).isEqualByComparingTo("-100");
        assertThat(r.getVariacionPorcentual()).isEqualByComparingTo("-33.3");
    }
}
