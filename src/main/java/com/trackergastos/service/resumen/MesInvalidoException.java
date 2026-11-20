package com.trackergastos.service.resumen;

/** Mes pedido con formato inválido, anterior a 2000-01 o posterior al mes actual (spec 002, punto 3.6). */
public class MesInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MesInvalidoException(String message) {
        super(message);
    }
}
