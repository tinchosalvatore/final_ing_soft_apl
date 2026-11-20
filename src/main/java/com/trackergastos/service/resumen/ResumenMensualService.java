package com.trackergastos.service.resumen;

import com.trackergastos.service.GastoQueryService;
import com.trackergastos.service.GastoService;
import com.trackergastos.service.criteria.GastoCriteria;
import com.trackergastos.service.dto.GastoDTO;
import java.time.YearMonth;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.filter.LocalDateFilter;

/**
 * Resumen mensual del usuario autenticado (spec 002).
 * <p>
 * Los gastos se piden a {@link GastoQueryService}, que siempre agrega "dueño = usuario autenticado" (defensa S2 de la
 * spec 001). Por eso el resumen solo incluye gastos propios (002-CA08) sin una consulta nueva. El cálculo lo hace
 * {@link ResumenMensualCalculadora}, que es pura. Los logs no llevan montos (AGENTS.md, regla 6).
 */
@Service
@Transactional(readOnly = true)
public class ResumenMensualService {

    private static final Logger LOG = LoggerFactory.getLogger(ResumenMensualService.class);

    private final GastoQueryService gastoQueryService;

    private final ResumenMensualCalculadora calculadora = new ResumenMensualCalculadora();

    public ResumenMensualService(GastoQueryService gastoQueryService) {
        this.gastoQueryService = gastoQueryService;
    }

    /**
     * @param mes mes en formato {@code AAAA-MM}, o {@code null} para el mes actual.
     * @return el resumen calculado solo con los gastos del usuario autenticado.
     * @throws MesInvalidoException si el mes es inválido.
     */
    public ResumenMensualDTO obtenerResumen(String mes) {
        YearMonth pedido = MesResumen.resolver(mes, GastoService.hoy());
        LOG.debug("Resumen mensual: mes={}", pedido);

        // Del primer día del mes anterior (para la variación) al último día del mes pedido
        LocalDateFilter rango = new LocalDateFilter();
        rango.setGreaterThanOrEqual(pedido.minusMonths(1).atDay(1));
        rango.setLessThanOrEqual(pedido.atEndOfMonth());
        GastoCriteria criteria = new GastoCriteria();
        criteria.setFecha(rango);

        List<GastoDTO> gastos = gastoQueryService.findByCriteria(criteria, Pageable.unpaged()).getContent();
        return calculadora.calcular(pedido, gastos);
    }
}
