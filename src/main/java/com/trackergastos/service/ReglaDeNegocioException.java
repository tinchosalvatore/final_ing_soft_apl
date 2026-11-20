package com.trackergastos.service;

import java.io.Serial;

/**
 * Una operación viola una regla de negocio que no es de un solo campo (por ejemplo, borrar una categoría en uso).
 * El {@code ExceptionTranslator} la devuelve como 400 con {@code message = "error." + clave} (spec 001 §5.1).
 */
public class ReglaDeNegocioException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String entidad;
    private final String clave;

    public ReglaDeNegocioException(String entidad, String clave, String mensaje) {
        super(mensaje);
        this.entidad = entidad;
        this.clave = clave;
    }

    public String getEntidad() {
        return entidad;
    }

    public String getClave() {
        return clave;
    }
}
