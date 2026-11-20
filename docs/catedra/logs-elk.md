# Guía: logs y ELK

Responsable: implementador de la spec 004. Objetivo: **lo más simple que cumpla la spec**. Si algo no está en esta guía ni en la spec, no se agrega.

## 1. Qué se aplica

| Concepto | Se aplica como |
|---|---|
| Log estructurado en JSON | Encoder de `logstash-logback-encoder` (ya viene en JHipster) |
| Niveles | `INFO` para hitos normales, `WARN` para lo sospechoso que no requiere acción inmediata, `ERROR` para fallas. `DEBUG` solo en dev |
| Campos de un log de nube (cuándo, dónde, quién, qué, resultado, traza) | `@timestamp`, `app_name`, `usuario`, `evento`, `message` y campos del evento, `traceId` |
| Ciclo de vida | Generar (app) → transportar (TCP 5000) → procesar (Logstash) → almacenar (Elasticsearch) → analizar (Kibana) |
| Telemetría: **logs** | Los eventos de la spec 004 |
| Telemetría: **métricas** | Derivadas de los logs `PETICION_HTTP` (peticiones por minuto, % de 5xx, latencia promedio y p95) en Kibana Lens |
| Telemetría: **trazas** | `traceId` y `spanId` en cada log (Micrometer Tracing). Todos los logs de una petición se encuentran por `traceId` en Discover |
| No loguear datos sensibles | Regla de la spec 004 y test 004-CA02 |
| Alertas por severidad | Una regla de Kibana por `LOGIN_FALLIDO` (equivale a la severidad "On-call") |

## 2. Cómo lo tiene que hacer el agente

### Infraestructura (`src/main/docker/elk.yml` + `src/main/docker/elk/`)
- Tres servicios: `elasticsearch`, `logstash` y `kibana`, con **la misma versión exacta** para los tres (la última estable al momento de implementar; nada de `latest`).
- Elasticsearch en modo `single-node`, con memoria acotada (`ES_JAVA_OPTS=-Xms512m -Xmx512m`) y un volumen con nombre para los datos.
- Seguridad de Elastic **desactivada** (`xpack.security.enabled=false`): es un entorno local o de demo (spec 004, fuera de alcance). Kibana necesita `XPACK_ENCRYPTEDSAVEDOBJECTS_ENCRYPTIONKEY` (32 caracteres o más) para que funcionen las reglas de alerta; el valor sale de `.env`. Si la versión elegida exige la seguridad activada para las alertas, el agente lo anota como decisión en el `plan.md` antes de seguir.
- Pipeline de Logstash `src/main/docker/elk/logstash.conf`:
  - `input { tcp { port => 5000 codec => json_lines } }`
  - `output { elasticsearch { hosts => ["http://elasticsearch:9200"] index => "tracker-logs-%{+YYYY.MM.dd}" } }`
  - Sin filtros: los logs ya llegan estructurados.
- Puertos expuestos: 5000 (Logstash), 5601 (Kibana). 9200 solo si hace falta para depurar.

### Aplicación
- Activar el appender por variables de entorno (`JHIPSTER_LOGGING_LOGSTASH_ENABLED=true`, `..._HOST`, `..._PORT`), en dev cuando ELK está levantado y en prod. La consola sigue andando (ADR-0004).
- **Campo `evento`**: cada log de la tabla de la spec 004 se escribe con `StructuredArguments.kv("evento", "LOGIN_FALLIDO")` y los datos extra también con `kv(...)`, para que lleguen como campos y no dentro del texto.
- **Campo `usuario`**: un filtro HTTP pone el login del usuario autenticado en el MDC al inicio de la petición y lo saca al final.
- **Traza**: verificar que `traceId` y `spanId` aparezcan en el MDC (Micrometer Tracing, que viene con Spring Boot Actuator). Si JHipster no trae el bridge, agregar `micrometer-tracing-bridge-brave` (versión gestionada por Spring Boot) y justificarlo en el `plan.md`.
- **`PETICION_HTTP`**: un `OncePerRequestFilter` para `/api/**` que, al terminar, loguea método, **patrón de ruta** (`/api/gastos/{id}`, no el id concreto, para poder agrupar), estado y `duracion_ms`.
- **`LOGIN_FALLIDO`**: escuchar el evento de Spring Security de credenciales incorrectas (`AuthenticationFailureBadCredentialsEvent`) o registrarlo en el endpoint de autenticación. Incluir login e IP. Mismo log exista o no el usuario.
- **`GASTO_AJENO`**: en el punto donde `GastoService` no encuentra el gasto para el usuario actual pero el id existe. Se loguea, pero la respuesta sigue siendo 404 (spec 001).
- **Lo que no se loguea nunca**: el DTO de un gasto completo, el body de las peticiones, headers `Authorization`, contraseñas. No usar `toString()` de DTOs en los logs.

### Kibana (todo exportado al repo)
- Data view `tracker-logs-*` con `@timestamp` como campo de tiempo.
- Una búsqueda guardada en Discover: "Eventos de seguridad" (`evento: (LOGIN_FALLIDO or GASTO_AJENO or ACCESO_DENEGADO)`).
- Dashboard **"Salud – Tracker de Gastos"** con los 5 paneles de la spec 004 hechos con **Lens**.
- Regla de alerta tipo **Elasticsearch query**: `evento : "LOGIN_FALLIDO"`, cuenta ≥ 5 en los últimos 5 minutos, chequeo cada 1 minuto. Acción: conector **Server log** (gratuito), o solo el registro en el panel de alertas.
- **Exportar** el data view, la búsqueda y el dashboard (Stack Management → Saved Objects → Export) a `src/main/docker/elk/kibana/objetos.ndjson`, y documentar en `src/main/docker/elk/README.md` cómo importarlos y cómo crear la regla (la regla se puede crear por API con un `curl` que también queda en ese README). Así el entorno se puede reconstruir.

## 3. Cómo se verifica
| Criterio | Cómo |
|---|---|
| 004-CA01 a CA05 | Tests de integración que capturan los logs (`OutputCaptureExtension` de Spring Boot o un appender de prueba de Logback) y validan los campos y la **ausencia** de datos sensibles |
| 004-CA06 a CA09 | Verificación manual guiada, con capturas en `verify.md`: levantar ELK, generar tráfico, abrir Discover, el dashboard y el panel de alertas. Para CA08 y CA09, un script `src/main/docker/elk/probar-alerta.sh` que hace N logins fallidos con `curl` |
| 004-CA10 | Apagar ELK y comprobar que la app responde y sigue escribiendo en la consola |

## 4. Qué queda afuera y por qué
| Concepto | Motivo |
|---|---|
| Filebeat / factor XI "puro" | Una pieza más; concesión documentada en ADR-0004 |
| OpenTelemetry Collector | Excede el alcance; las trazas se resuelven con `traceId` en los logs |
| Kafka/Redis como buffer | Un solo servicio y entorno local |
| Políticas de retención / ILM | Decisión del humano: fuera de alcance |
| TLS y usuarios en ELK | Entorno local o de demo |
| Métricas de infraestructura (CPU, RAM), Metricbeat, APM | Fuera de alcance; las métricas salen de los logs de peticiones |
| Grafana | ELK cubre el dashboard |

## 5. Referencias
Spec 004, ADR-0004, `seguridad.md` (S7, S8), `12-factors.md` (XI, 14).
