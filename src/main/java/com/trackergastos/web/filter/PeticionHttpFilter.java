package com.trackergastos.web.filter;

import static net.logstash.logback.argument.StructuredArguments.kv;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Spec 004: pone el login del usuario autenticado en el MDC ({@code usuario}) durante la petición y, al terminar,
 * registra {@code PETICION_HTTP} con método, patrón de ruta, estado y duración. Solo para {@code /api/**}.
 * <p>
 * Va dentro de la cadena de seguridad, después del filtro de JWT (ver {@code SecurityConfiguration}), para conocer al
 * usuario y ver el estado final de los 401 y 403. No se registra como bean para no correr dos veces.
 */
public class PeticionHttpFilter extends OncePerRequestFilter {

    public static final String MDC_USUARIO = "usuario";

    private static final Logger LOG = LoggerFactory.getLogger(PeticionHttpFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        long inicio = System.nanoTime();
        String usuario = usuarioAutenticado();
        if (usuario != null) {
            MDC.put(MDC_USUARIO, usuario);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            long duracionMs = (System.nanoTime() - inicio) / 1_000_000;
            LOG.info(
                "Petición HTTP",
                kv("evento", "PETICION_HTTP"),
                kv("metodo", request.getMethod()),
                kv("ruta", ruta(request)),
                kv("estado", response.getStatus()),
                kv("duracion_ms", duracionMs)
            );
            MDC.remove(MDC_USUARIO);
        }
    }

    /** El patrón (ej. {@code /api/gastos/{id}}) para poder agrupar; la URI si la petición no llegó a un controller. */
    private static String ruta(HttpServletRequest request) {
        Object patron = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return patron != null ? patron.toString() : request.getRequestURI();
    }

    private static String usuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }
}
