package com.trackergastos.service;

import java.io.Serial;

/**
 * Un campo no cumple una regla de negocio que se valida en el service (spec 001, regla 2; defensa S4).
 * El {@code ExceptionTranslator} la devuelve como 400 con {@code fieldErrors}, igual que un error de Bean Validation,
 * para que el cliente la muestre junto al campo (spec 001 §5.1 y regla 15).
 */
public class CampoInvalidoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String entidad;
    private final String campo;

    public CampoInvalidoException(String entidad, String campo, String mensaje) {
        super(mensaje);
        this.entidad = entidad;
        this.campo = campo;
    }

    public String getEntidad() {
        return entidad;
    }

    public String getCampo() {
        return campo;
    }
}
