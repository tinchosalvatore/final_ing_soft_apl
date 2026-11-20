package com.trackergastos.service;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** El registro público está deshabilitado (spec 001, regla 16 y criterio 001-CA16): 401. */
@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Registro deshabilitado")
public class RegistroDeshabilitadoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RegistroDeshabilitadoException() {
        super("Registro deshabilitado");
    }
}
