package com.trackergastos.service.resumen;

import org.springframework.stereotype.Service;

/**
 * Resumen mensual del usuario autenticado (spec 002).
 * STUB de la fase roja: lo implementa el agente coder (puede agregar dependencias al constructor).
 */
@Service
public class ResumenMensualService {

    /**
     * @param mes mes en formato {@code AAAA-MM}, o {@code null} para el mes actual.
     * @return el resumen calculado solo con los gastos del usuario autenticado.
     * @throws MesInvalidoException si el mes es inválido.
     */
    public ResumenMensualDTO obtenerResumen(String mes) {
        throw new UnsupportedOperationException("002: pendiente de implementar (fase roja)");
    }
}
