# Tareas — Spec 004

| # | Tarea | Criterios que cubre | Estado | Commit |
|---|---|---|---|---|
| 1 | Dependencias: `logstash-logback-encoder` 9.0, `spring-boot-micrometer-tracing-brave` + `micrometer-tracing-bridge-brave` (BOM de Boot) | todos | hecha | ver PR |
| 2 | `PeticionHttpFilter` (usuario en el MDC + `PETICION_HTTP`) en la cadena de seguridad; `AccesoDenegadoHandler` | CA04, S8 | hecha | |
| 3 | `AuthenticateController`: `LOGIN_OK`/`LOGIN_FALLIDO` (login con forma de email enmascarado), 401 único para incorrecto/inexistente/desactivado | CA01, CA11 | hecha | |
| 4 | `ExceptionTranslator`: `ERROR_INESPERADO`, `ACCESO_DENEGADO`, detalle sin nombres internos en todos los perfiles (incluye el 400 `idinvalid`) | CA05 | hecha | |
| 5 | Eventos de `GastoService` (`GASTO_*`, `GASTO_AJENO`) y `CategoriaService` (`CATEGORIA_*`) | CA02, CA03 | hecha | |
| 6 | Sin datos sensibles: `LoggingAspect` (sin args/resultados/mensajes), `MailService`, `UserService`, `UserResource`, `DomainUserDetailsService` | CA02, regla 3 | hecha | |
| 7 | Tests: `ObservabilidadIT` (13) + `LoggingAspectTest` actualizado | CA01–CA05, CA11 | hecha | |
| 8 | `elk.yml`, `logstash.conf`, `.env.example` (`KIBANA_ENCRYPTION_KEY`) | CA06, CA10 | hecha | |
| 9 | `kibana/objetos.ndjson`, regla por API, `README.md`, `probar-alerta.sh` | CA06–CA09 | hecha | |
| 10 | Verificación manual con capturas de CA06 a CA10 en `verify.md` | CA06–CA10 | pendiente (humano / seguridad) | |

## Notas para la próxima sesión
- **CI con logs apagados:** `backend:unit:test` corre con `-Dlogging.level.com.trackergastos=OFF`. `CapturaDeLogs` sube el nivel a INFO mientras captura; sin eso los tests de logs no ven nada.
- **Tracing en Boot 4:** hacen falta las dos dependencias (`spring-boot-micrometer-tracing-brave` trae la autoconfiguración; el bridge trae Brave). En los tests `traceId` aparece sin configuración extra.
- **Kibana 9.5.5 sin seguridad:** las reglas de alerta funcionan (`/api/alerting/_health` → `is_sufficiently_secure: true`); no hizo falta activar la seguridad de Elastic.
- **Prueba local hecha por el agente** (app en dev con `JHIPSTER_LOGGING_LOGSTASH_ENABLED=true`): los registros llegan a `tracker-logs-*` con `evento`, `usuario`, `traceId`, `spanId`, `app_name`; el dashboard muestra los 5 paneles con datos; la regla con 4 fallidos no dispara y con el 5.º sí (detalle en el PR).
- `elk.yml` exige `KIBANA_ENCRYPTION_KEY` en todos los comandos de compose: usar siempre `--env-file .env`.
