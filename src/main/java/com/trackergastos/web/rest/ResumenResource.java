package com.trackergastos.web.rest;

import com.trackergastos.service.resumen.ResumenMensualDTO;
import com.trackergastos.service.resumen.ResumenMensualService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Resumen mensual (spec 002 §5.1). Las reglas están en {@link ResumenMensualService}; un mes inválido sale como 400
 * {@code error.mesinvalido} por el {@code ExceptionTranslator}. Sin sesión, 401 (seguridad generada para {@code /api/**}).
 */
@RestController
@RequestMapping("/api/resumen")
public class ResumenResource {

    private final ResumenMensualService resumenMensualService;

    public ResumenResource(ResumenMensualService resumenMensualService) {
        this.resumenMensualService = resumenMensualService;
    }

    /**
     * {@code GET /api/resumen?mes=AAAA-MM}: resumen del mes pedido, o del mes actual si no se indica.
     *
     * @param mes el mes en formato {@code AAAA-MM}, opcional.
     * @return el resumen del usuario autenticado.
     */
    @GetMapping("")
    public ResponseEntity<ResumenMensualDTO> obtenerResumen(@RequestParam(name = "mes", required = false) String mes) {
        return ResponseEntity.ok(resumenMensualService.obtenerResumen(mes));
    }
}
