package com.trackergastos.security;

import static net.logstash.logback.argument.StructuredArguments.kv;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Spec 004: registra {@code ACCESO_DENEGADO} cuando la autorización por URL rechaza una petición (ej. un USER en
 * {@code /api/admin/**}) y responde como el handler de JHipster. El rechazo de {@code @PreAuthorize} lo registra el
 * {@code ExceptionTranslator}.
 */
public class AccesoDenegadoHandler implements AccessDeniedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AccesoDenegadoHandler.class);

    private final AccessDeniedHandler delegado = new BearerTokenAccessDeniedHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
        throws IOException, ServletException {
        LOG.warn("Acceso denegado", kv("evento", "ACCESO_DENEGADO"), kv("operacion", request.getMethod() + " " + request.getRequestURI()));
        delegado.handle(request, response, accessDeniedException);
    }
}
