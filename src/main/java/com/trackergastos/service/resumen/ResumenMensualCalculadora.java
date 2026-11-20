package com.trackergastos.service.resumen;

import com.trackergastos.service.dto.GastoDTO;
import java.time.YearMonth;
import java.util.List;

/**
 * Cálculo puro del resumen mensual (spec 002, puntos 3.2 a 3.4).
 * STUB de la fase roja: lo implementa el agente coder.
 */
public class ResumenMensualCalculadora {

    /**
     * @param mes   el mes a resumir.
     * @param gastosDelUsuario gastos de un único usuario; pueden incluir gastos de cualquier mes.
     *              Solo cuentan los del {@code mes} (total y detalle) y los del mes anterior (variación).
     */
    public ResumenMensualDTO calcular(YearMonth mes, List<GastoDTO> gastosDelUsuario) {
        throw new UnsupportedOperationException("002: pendiente de implementar (fase roja)");
    }
}
