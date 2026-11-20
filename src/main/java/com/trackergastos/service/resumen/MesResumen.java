package com.trackergastos.service.resumen;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Resuelve y valida el mes pedido para el resumen (spec 002, puntos 3.1 y 3.6; criterios 002-CA10 y CA11).
 */
public final class MesResumen {

    /** El mes más antiguo que se puede pedir (spec 002, punto 3.6). */
    static final YearMonth PRIMER_MES = YearMonth.of(2000, 1);

    private static final Pattern FORMATO = Pattern.compile("^\\d{4}-\\d{2}$");

    private MesResumen() {}

    /**
     * @param mes  mes pedido en formato {@code AAAA-MM}, o {@code null} si no se indicó.
     * @param hoy  la fecha de hoy en America/Argentina/Buenos_Aires.
     * @return el mes a resumir; si {@code mes} es {@code null}, el mes actual.
     * @throws MesInvalidoException si el formato es inválido, es anterior a 2000-01 o posterior al mes actual.
     */
    public static YearMonth resolver(String mes, LocalDate hoy) {
        YearMonth actual = YearMonth.from(hoy);
        if (mes == null) {
            return actual;
        }
        if (!FORMATO.matcher(mes).matches()) {
            throw new MesInvalidoException("El mes tiene que tener el formato AAAA-MM");
        }
        YearMonth pedido;
        try {
            pedido = YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            throw new MesInvalidoException("El mes no existe");
        }
        if (pedido.isBefore(PRIMER_MES) || pedido.isAfter(actual)) {
            throw new MesInvalidoException("El mes tiene que estar entre 2000-01 y el mes actual");
        }
        return pedido;
    }
}
