# Plan — Spec 004

- Autor: agente observabilidad (2026-11-20)
- Estado: Borrador (pendiente de aprobación del documentador)

## 1. Resumen de la solución
Se activa el appender de Logstash que ya trae JHipster (por variables de entorno, ADR-0004) y cada evento de la spec se escribe con `StructuredArguments.kv(...)`, para que `evento` y sus datos lleguen como campos JSON. Un filtro dentro de la cadena de seguridad pone `usuario` en el MDC y escribe `PETICION_HTTP`. Micrometer Tracing (Brave, sin exportar spans) aporta `traceId`/`spanId`. ELK corre con un `docker compose` propio, sin filtros en Logstash, y lo de Kibana queda exportado al repo.

## 2. Archivos que se tocan
| Archivo | Territorio | Qué cambia |
|---|---|---|
| `pom.xml` | compartido | 2 dependencias (sección 4) |
| `config/application-dev.yml`, `application-prod.yml` | compartido | `jhipster.logging.logstash.enabled/host/port` leídos de `JHIPSTER_LOGGING_LOGSTASH_*` (default `false`) |
| `.env.example` | compartido | ya tiene las variables; se suma `KIBANA_ENCRYPTION_KEY` |
| `web/filter/PeticionHttpFilter.java` (nuevo) | dev | `usuario` en el MDC + log `PETICION_HTTP` (método, patrón de ruta, estado, `duracion_ms`) |
| `config/SecurityConfiguration.java` | compartido | registra el filtro con `addFilterAfter(..., BearerTokenAuthenticationFilter.class)`; envuelve el `AccessDeniedHandler` para loguear `ACCESO_DENEGADO` en las rutas `/api/admin/**` |
| `web/rest/AuthenticateController.java` | dev | `LOGIN_OK` / `LOGIN_FALLIDO` (login, ip). Toda `AuthenticationException` (incluido el usuario desactivado, que hoy llega envuelto en `InternalAuthenticationServiceException`) se responde como un `BadCredentialsException` con mensaje fijo → 401 con el mismo cuerpo (CA11) |
| `web/rest/errors/ExceptionTranslator.java` | compartido | `ERROR_INESPERADO` (ERROR, `tipoError`, stack solo en el log) para todo 5xx; `ACCESO_DENEGADO` para el `AccessDeniedException` de `@PreAuthorize`; `detail` sin `getMessage()` de `ErrorResponseException` (arregla el `ProblemDetailWithCause[...]` del 400 `idinvalid`) y genérico si el mensaje tiene paquetes, "Exception" o SQL, **en todos los perfiles** |
| `service/GastoService.java` | dev | `GASTO_CREADO/EDITADO/BORRADO` (INFO, `gastoId`); `GASTO_AJENO` (WARN, `gastoId`, `operacion`) en `buscarPropio` cuando no es del usuario pero el id existe. La respuesta sigue siendo 404 |
| `service/CategoriaService.java` | dev | `CATEGORIA_CREADA/EDITADA/BORRADA` (INFO, `categoriaId`); los `debug` dejan de imprimir el DTO |
| `aop/logging/LoggingAspect.java` | compartido | deja de loguear argumentos y resultados (solo el nombre del método) |
| `service/MailService.java`, `service/UserService.java`, `web/rest/UserResource.java`, `security/DomainUserDetailsService.java` | dev / compartido | ningún log con email ni `User`/`UserDTO` completo: solo login o id |
| `src/main/docker/elk.yml`, `src/main/docker/elk/logstash.conf` (nuevos) | dev | ES + Logstash + Kibana, versión exacta **9.5.5** (último tag estable en Docker Hub hoy), single-node, 512 MB, sin seguridad |
| `src/main/docker/elk/kibana/objetos.ndjson`, `README.md`, `probar-alerta.sh` (nuevos) | dev | data view, búsqueda, dashboard exportados; pasos para levantar y mostrar en la defensa; regla creada por `curl`; script de N logins fallidos |
| `src/test/java/.../observabilidad/*IT.java` + `CapturaDeLogs` (nuevos) | dev | tests 004-CA01 a CA05 y CA11 |
| `docs/specs/004-observabilidad/tasks.md`, `verify.md` | — | tareas y capturas de CA06 a CA10 |

## 3. Dónde vive cada regla
| Regla de la spec | Clase / método |
|---|---|
| Campos comunes (`app_name`, `traceId`, `usuario`, `evento`) | `LoggingConfiguration` (generado, ya pone `app_name`), Micrometer Tracing, `PeticionHttpFilter` |
| `LOGIN_OK`, `LOGIN_FALLIDO`, mismo trato a inexistente/desactivado | `AuthenticateController.authorize` |
| `ACCESO_DENEGADO` | `ExceptionTranslator` (método) + handler de `SecurityConfiguration` (URL) |
| `GASTO_*`, `GASTO_AJENO` | `GastoService` |
| `CATEGORIA_*` | `CategoriaService` |
| `PETICION_HTTP` | `PeticionHttpFilter` |
| `ERROR_INESPERADO`, respuestas sin nombres internos | `ExceptionTranslator` |
| Sin datos sensibles | todas las anteriores + `LoggingAspect`, `MailService`, `UserService` |

## 4. Dependencias nuevas
| Dependencia | Versión | Por qué |
|---|---|---|
| `net.logstash.logback:logstash-logback-encoder` | `9.0` (la de `jhipster-parent` 9.4) | `jhipster-framework` la declara `optional`: sin ella el appender de Logstash y `kv(...)` no existen |
| `org.springframework.boot:spring-boot-micrometer-tracing-brave` | gestionada por Spring Boot 4.1.1 | no hay bridge de tracing en el árbol: sin él no hay `traceId` en el MDC (la guía lo prevé). No se exportan spans (sin Zipkin) |

## 5. Decisiones tomadas por el agente
- `PETICION_HTTP` se registra dentro de la cadena de seguridad (después del filtro de JWT) para tener `usuario`. Consecuencia: una petición con un token **inválido** (rechazada por el propio filtro JWT) no genera `PETICION_HTTP`; sin token sí (401).
- Ruta sin patrón (no llegó a un controller, ej. 401) → se registra la URI.
- `operacion` de `GASTO_AJENO`: `ver`, `editar`, `borrar`. De `ACCESO_DENEGADO`: `"<MÉTODO> <patrón>"`, ej. `POST /api/categorias`.
- Los tests capturan con un `ListAppender` de Logback y serializan cada evento con el `LogstashEncoder`: se valida el JSON real que llega a Logstash.
- El arreglo del usuario desactivado va en el controller y no en `DomainUserDetailsService`, para no romper su test generado.

## 6. Decisiones pendientes
1. **Login con forma de email en `LOGIN_FALLIDO`.** La spec pide "login intentado", pero se puede entrar con el email, y el email es dato sensible (regla 3). Propuesta: si el login intentado contiene `@`, se registra enmascarado (`a***@***`). ¿Va así?
2. **Versión de ELK:** 9.5.5. Si las reglas de alerta exigen seguridad activada en esa versión, lo anoto acá antes de seguir (la guía lo prevé).

## 7. Riesgos
- Los tests de Spring Boot 4 pueden apagar el tracing por defecto: CA04 puede necesitar la anotación de `spring-boot-micrometer-tracing-test` (sería una dependencia más de test, que justifico acá si hace falta).
- `fix/001-patch` toca `GastoService` y `UserService`: integro `main` cuando se mergee, antes de tocarlos.
- Con el usuario desactivado no se verifica la contraseña, así que responde más rápido que con una cuenta activa (enumeración por tiempo). Queda fuera de alcance; lo dejo anotado para seguridad.
