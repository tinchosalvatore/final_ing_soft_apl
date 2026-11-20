package com.trackergastos.service.resumen;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de un mes de un usuario (spec 002, punto 3.2).
 * Contrato de la fase roja: los nombres de los campos los usan los tests.
 */
public class ResumenMensualDTO implements Serializable {

    private YearMonth mes;
    /** Total del mes, exacto. */
    private BigDecimal total;
    /** Cantidad de gastos del mes. */
    private long cantidad;
    /** Una fila por categoría con gastos en el mes, ordenadas por total desc y nombre asc. */
    private List<CategoriaResumenDTO> categorias = new ArrayList<>();
    /** Total del mes menos total del mes anterior (puede ser negativa). */
    private BigDecimal diferencia;
    /** Variación porcentual (escala 0-100, 1 decimal). {@code null} = no informada (mes anterior en 0). */
    private BigDecimal variacionPorcentual;

    public YearMonth getMes() {
        return mes;
    }

    public void setMes(YearMonth mes) {
        this.mes = mes;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public long getCantidad() {
        return cantidad;
    }

    public void setCantidad(long cantidad) {
        this.cantidad = cantidad;
    }

    public List<CategoriaResumenDTO> getCategorias() {
        return categorias;
    }

    public void setCategorias(List<CategoriaResumenDTO> categorias) {
        this.categorias = categorias;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia;
    }

    public BigDecimal getVariacionPorcentual() {
        return variacionPorcentual;
    }

    public void setVariacionPorcentual(BigDecimal variacionPorcentual) {
        this.variacionPorcentual = variacionPorcentual;
    }
}
