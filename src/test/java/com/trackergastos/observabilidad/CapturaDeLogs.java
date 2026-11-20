package com.trackergastos.observabilidad;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.logstash.logback.encoder.LogstashEncoder;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Captura los logs de la aplicación tal como llegarían a Logstash: cada evento se serializa en el momento con el mismo
 * {@link LogstashEncoder} que usa el appender de JHipster (MDC, argumentos {@code kv(...)} y stack incluidos).
 */
final class CapturaDeLogs extends AppenderBase<ILoggingEvent> {

    private static final String PAQUETE = "com.trackergastos";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final List<String> lineas = new CopyOnWriteArrayList<>();

    private LogstashEncoder encoder;

    private Level nivelAnterior;

    void iniciar() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        encoder = new LogstashEncoder();
        encoder.setContext(context);
        encoder.setCustomFields("{\"app_name\":\"trackerGastos\"}");
        encoder.start();
        setContext(context);
        setName("CAPTURA_DE_LOGS");
        start();
        context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(this);
        // El CI corre con -Dlogging.level.com.trackergastos=OFF: los eventos de la spec son INFO o más
        Logger app = context.getLogger(PAQUETE);
        nivelAnterior = app.getLevel();
        app.setLevel(Level.INFO);
    }

    void detener() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(PAQUETE).setLevel(nivelAnterior);
        context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(this);
        stop();
        encoder.stop();
    }

    void limpiar() {
        lineas.clear();
    }

    @Override
    protected void append(ILoggingEvent evento) {
        lineas.add(new String(encoder.encode(evento), StandardCharsets.UTF_8));
    }

    /** Todas las líneas JSON capturadas. */
    List<String> lineas() {
        return List.copyOf(lineas);
    }

    /** Los registros con ese código de evento. */
    List<JsonNode> eventos(String codigo) {
        return lineas
            .stream()
            .map(JSON::readTree)
            .filter(json -> codigo.equals(json.path("evento").asString(null)))
            .toList();
    }

    /**
     * Las líneas sin los campos que son ruido aleatorio o técnico (hora, traza, ids, duración), para buscar datos
     * sensibles sin falsos positivos (un {@code traceId} hexadecimal puede contener "1234").
     */
    List<String> lineasSinCamposTecnicos() {
        return lineas
            .stream()
            .map(linea -> {
                var json = (tools.jackson.databind.node.ObjectNode) JSON.readTree(linea);
                json.remove(List.of("@timestamp", "traceId", "spanId", "gastoId", "categoriaId", "duracion_ms", "thread_name"));
                return json.toString();
            })
            .toList();
    }
}
