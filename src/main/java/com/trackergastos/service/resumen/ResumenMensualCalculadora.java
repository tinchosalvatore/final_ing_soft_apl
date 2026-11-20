package com.trackergastos.service.resumen;

import com.trackergastos.service.dto.GastoDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculo puro del resumen mensual (spec 002, puntos 3.2 a 3.4). Sin base de datos ni seguridad: recibe los gastos
 * de un único usuario y devuelve el resumen. Todo con {@link BigDecimal}, para que las sumas sean exactas (002-CA02).
 */
public class ResumenMensualCalculadora {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    /** Totales en pesos: 2 decimales (spec 002 §5.1). */
    private static final int ESCALA_PESOS = 2;

    /** Participación y variación: 1 decimal, la mitad hacia arriba (spec 002, punto 3.3). */
    private static final int ESCALA_PORCENTAJE = 1;

    /**
     * @param mes   el mes a resumir.
     * @param gastosDelUsuario gastos de un único usuario; pueden incluir gastos de cualquier mes.
     *              Solo cuentan los del {@code mes} (total y detalle) y los del mes anterior (variación).
     * @return el resumen del mes.
     */
    public ResumenMensualDTO calcular(YearMonth mes, List<GastoDTO> gastosDelUsuario) {
        YearMonth anterior = mes.minusMonths(1); // enero → diciembre del año anterior (002-CA06)
        List<GastoDTO> delMes = gastosDelUsuario
            .stream()
            .filter(g -> YearMonth.from(g.getFecha()).equals(mes))
            .toList();
        BigDecimal total = sumar(delMes);
        BigDecimal totalAnterior = sumar(
            gastosDelUsuario
                .stream()
                .filter(g -> YearMonth.from(g.getFecha()).equals(anterior))
                .toList()
        );

        ResumenMensualDTO resumen = new ResumenMensualDTO();
        resumen.setMes(mes);
        resumen.setTotal(total);
        resumen.setCantidad(delMes.size());
        resumen.setCategorias(detallePorCategoria(delMes, total));
        BigDecimal diferencia = total.subtract(totalAnterior);
        resumen.setDiferencia(diferencia);
        // Sin gastos el mes anterior no hay base para un porcentaje: no se informa (002-CA05)
        resumen.setVariacionPorcentual(totalAnterior.signum() == 0 ? null : porcentaje(diferencia, totalAnterior));
        return resumen;
    }

    private static List<CategoriaResumenDTO> detallePorCategoria(List<GastoDTO> delMes, BigDecimal total) {
        Map<String, List<GastoDTO>> porCategoria = new LinkedHashMap<>();
        for (GastoDTO gasto : delMes) {
            porCategoria.computeIfAbsent(gasto.getCategoria().getNombre(), nombre -> new ArrayList<>()).add(gasto);
        }
        List<CategoriaResumenDTO> filas = new ArrayList<>();
        porCategoria.forEach((nombre, gastos) -> {
            CategoriaResumenDTO fila = new CategoriaResumenDTO();
            fila.setNombre(nombre);
            fila.setTotal(sumar(gastos));
            fila.setCantidad(gastos.size());
            fila.setParticipacion(porcentaje(fila.getTotal(), total));
            filas.add(fila);
        });
        // Punto 4: total de mayor a menor; a igual total, por nombre (002-CA09)
        filas.sort(Comparator.comparing(CategoriaResumenDTO::getTotal).reversed().thenComparing(CategoriaResumenDTO::getNombre));
        return filas;
    }

    private static BigDecimal sumar(List<GastoDTO> gastos) {
        BigDecimal suma = gastos.stream().map(GastoDTO::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        // Se lleva a 2 decimales sin redondear nunca: la regla 2 de la spec 001 ya limita los montos a 2 decimales.
        return suma.scale() < ESCALA_PESOS ? suma.setScale(ESCALA_PESOS) : suma;
    }

    /**
     * {@code parte × 100 / base} redondeado a 1 decimal con la mitad hacia arriba. {@code divide} con escala redondea
     * el cociente exacto (12,25 → 12,3), sin pasar por {@code double} ni redondear dos veces.
     */
    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal base) {
        return parte.multiply(CIEN).divide(base, ESCALA_PORCENTAJE, RoundingMode.HALF_UP);
    }
}
