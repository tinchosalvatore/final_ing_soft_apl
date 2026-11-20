package com.trackergastos.service.resumen;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Resuelve y valida el mes pedido para el resumen (spec 002, puntos 3.1 y 3.6).
 * STUB de la fase roja: lo implementa el agente coder.
 */
public final class MesResumen {

    private MesResumen() {}

    /**
     * @param mes  mes pedido en formato {@code AAAA-MM}, o {@code null} si no se indicó.
     * @param hoy  la fecha de hoy en America/Argentina/Buenos_Aires.
     * @return el mes a resumir; si {@code mes} es {@code null}, el mes actual.
     * @throws MesInvalidoException si el formato es inválido, es anterior a 2000-01 o posterior al mes actual.
     */
    public static YearMonth resolver(String mes, LocalDate hoy) {
        throw new UnsupportedOperationException("002: pendiente de implementar (fase roja)");
    }
}
