package com.trackergastos.service.resumen;

import java.io.Serializable;
import java.math.BigDecimal;

/** Fila del detalle por categoría del resumen mensual (spec 002, punto 3.2). */
public class CategoriaResumenDTO implements Serializable {

    private String nombre;
    private BigDecimal total;
    private long cantidad;
    /** Participación en el total del mes (escala 0-100, 1 decimal, mitad hacia arriba). */
    private BigDecimal participacion;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public BigDecimal getParticipacion() {
        return participacion;
    }

    public void setParticipacion(BigDecimal participacion) {
        this.participacion = participacion;
    }
}
