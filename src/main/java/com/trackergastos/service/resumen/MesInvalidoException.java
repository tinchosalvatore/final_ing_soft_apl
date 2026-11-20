package com.trackergastos.service.resumen;

import com.trackergastos.service.ReglaDeNegocioException;
import java.io.Serial;

/**
 * Mes pedido con formato inválido, anterior a 2000-01 o posterior al mes actual (spec 002, punto 3.6).
 * Es una {@link ReglaDeNegocioException}: el {@code ExceptionTranslator} la devuelve como 400 {@code error.mesinvalido}
 * (spec 002 §5.1).
 */
public class MesInvalidoException extends ReglaDeNegocioException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MesInvalidoException(String message) {
        super("resumen", "mesinvalido", message);
    }
}
