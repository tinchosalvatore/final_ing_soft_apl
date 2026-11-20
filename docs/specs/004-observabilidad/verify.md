# Verificación — Spec 004 (observabilidad)

- Verificador: agente seguridad (no participó en la implementación de la 004)
- Rama / PR: [PR #18](https://github.com/tinchosalvatore/final_ing_soft_apl/pull/18), verificado sobre `00cfe64`, mergeado en `main` como `7eaf9fc`. Este verify entra por `verify/003-004`
- Fecha: 2026-11-20
- Resultado: **Cumple con observaciones**: **CA06 a CA10 quedan pendientes de evidencia** (O1)

<!-- No alcanza con que compile. Se contrasta contra la spec: ¿cada criterio tiene un test que lo representa de verdad? -->

**Cómo se verificó.**
- No levanté la app ni ELK (regla 13).
- Revisé el código y la configuración de ELK.
- Corrí `npm run ci:backend:test` en local.
- Usé el CI del PR (run 44801833637, en verde).

**Resumen.**
- CA01 a CA05 y CA11 están cubiertos por `ObservabilidadIT`, que valida el JSON real del encoder.
- **S7:** el `ExceptionTranslator` ya no depende del perfil, así que ningún 4xx ni 5xx expone clases internas en ningún perfil.
- `LoggingAspect`, `MailService`, `UserService`, `UserResource` y `DomainUserDetailsService` ya no loguean argumentos, emails, claves ni DTO.
- La evidencia de CA06 a CA10 es **solo la afirmación del agente que implementó**. No alcanza como verificación (O1).

## 1. Criterios de aceptación

| ID | Test o evidencia | ¿Representa el criterio? | Resultado |
|---|---|---|---|
| 004-CA01 | `ObservabilidadIT`: `004-CA01 login fallido…`, `…borde: un usuario inexistente se registra igual; un login con forma de email se enmascara`, `…LOGIN_OK al iniciar sesión` | sí: `LOGIN_FALLIDO`, WARN, `login` e `ip`, sin la contraseña. El inexistente sale con el mismo formato | ✅ |
| 004-CA02 | `ObservabilidadIT`: `004-CA02 crear un gasto…` | sí: `GASTO_CREADO` con `gastoId` y `usuario`, y ninguna línea con "1234" ni "farmacia". **Alcance:** captura `com.trackergastos` en INFO. El DEBUG de dev lo verifiqué revisando el código (§2) | ✅ |
| 004-CA03 | `ObservabilidadIT`: `004-CA03 ana pide un gasto de beto…` y `…borde: un id que no existe no es GASTO_AJENO` | sí: 404 y `GASTO_AJENO` WARN con `usuario = ana` y el id. El inexistente no genera el evento, y la respuesta al cliente es la misma (S1 de la 001) | ✅ |
| 004-CA04 | `ObservabilidadIT`: `004-CA04 … comparten el traceId` y `…PETICION_HTTP registra el patrón de la ruta, no el id`; borde `petición sin sesión tiene traza pero no usuario` | sí | ✅ |
| 004-CA05 | `ObservabilidadIT`: `004-CA05 error inesperado…` y `…también los 400: el idinvalid no expone ProblemDetailWithCause` | sí, con el hallazgo del verify de la 001 (O4) incluido | ✅ |
| 004-CA06 | Verificación manual del agente observabilidad (descripción del PR #18) | **sin evidencia propia** (O1) | ⏳ pendiente de evidencia |
| 004-CA07 | ídem; el dashboard está exportado en `elk/kibana/objetos.ndjson` | ídem. Revisé que el ndjson tenga el dashboard con el nombre del contrato y 5 paneles Lens | ⏳ pendiente de evidencia |
| 004-CA08 | ídem; la regla está en `elk/README.md` §4 (se crea por API) y `probar-alerta.sh` | La configuración es correcta: _Elasticsearch query_ `evento: LOGIN_FALLIDO`, `threshold [5]`, `>=`, ventana de 5 min y chequeo cada 1 min. Falta la evidencia de la ejecución | ⏳ pendiente de evidencia |
| 004-CA09 | ídem | ídem (4 < 5 con la misma regla) | ⏳ pendiente de evidencia |
| 004-CA10 | ídem | ídem. Con Logstash caído, el appender TCP de JHipster no bloquea: es asíncrono y reconecta | ⏳ pendiente de evidencia |
| 004-CA11 | `ObservabilidadIT`: `004-CA11 usuario desactivado…` | sí: el cuerpo del 401 es **idéntico** con credenciales incorrectas, con el desactivado con contraseña correcta y con el desactivado con contraseña incorrecta. Hay 3 `LOGIN_FALLIDO` y ningún `ERROR_INESPERADO` (cierra O3 del verify del 000) | ✅ |

## 2. S7 y datos sensibles

| Punto | Resultado |
|---|---|
| **S7 en todos los perfiles** | `ExceptionTranslator.getCustomizedErrorDetails` ya no tiene la rama `activeProfiles.contains(prod)`. Un 5xx siempre responde "Unexpected error". Un 4xx deja pasar el mensaje solo si `esInterno` es falso (sin nombre de paquete, "Exception", "ProblemDetail", "SQL", "select " ni "insert "). Si es interno, usa el `title` o "Invalid request". La `cause` anidada pasa por el mismo `customizeProblem`. ✅ |
| **`LoggingAspect` (dev)** | Ya no loguea argumentos, resultados ni mensajes de excepción: solo el nombre del método y el tipo de excepción. `LoggingAspectTest` lo afirma con los mensajes exactos. Cierra O5 del verify de la 001. ✅ |
| **`MailService`** | Sin destinatario, asunto ni contenido. Solo el tipo de error. Cierra O7 del verify del 000. ✅ |
| **`UserService` / `UserResource` / `DomainUserDetailsService`** | Sin claves de activación ni de reset, sin `User.toString()` ni `UserDTO.toString()` (que llevan el email), y sin el login en el DEBUG de autenticación. Solo el login, donde hace falta. ✅ |
| **Búsqueda de lo que quedó** | `grep` de llamadas a log con `getEmail`, `getPassword`, token, monto, descripción o un DTO en `src/main/java`: solo aparece `CategoriaResource` con `CategoriaDTO` (nombre de la categoría, que no es sensible). `SecurityJwtConfiguration` loguea `e.getMessage()` de un JWT inválido: son mensajes de Nimbus, sin el token. ✅ |
| **Otros loggers en dev** | `application-dev.yml` pone `ROOT: DEBUG` y `org.hibernate.SQL: DEBUG`. SQL se loguea con `?`, porque los valores van en `org.hibernate.orm.jdbc.bind`, a TRACE, que no está activo. `logback-spring.xml` deja `org.springframework.web` en WARN, así que tampoco aparece el "Read … to [GastoDTO{…}]" de Spring MVC. ✅ |
| **Eventos de negocio** | `GASTO_*`, `CATEGORIA_*`, `GASTO_AJENO`, `ACCESO_DENEGADO` y `PETICION_HTTP` llevan ids, el patrón de la ruta, el método, el estado y la duración. ✅ |

## 3. Casos borde no contemplados

### O1 (media, evidencia): CA06 a CA10 solo tienen la palabra del implementador

La descripción del PR #18 dice "verificado a mano", pero no adjunta nada: ni capturas, ni salida de consultas a ES, ni el estado de la regla. `src/main/docker/elk/README.md` dice que las capturas "las junta el humano". Un verificador no puede dar por cumplidos estos criterios con la afirmación de quien los implementó.

**Qué haría falta como mínimo:**
- **CA06:** una captura de Discover en `tracker-logs-*` con un evento y su `@timestamp`, o la salida de `_count`/`_search`.
- **CA07:** una captura del dashboard "Salud – Tracker de Gastos" con los 5 paneles con datos.
- **CA08 y CA09:** la salida de `probar-alerta.sh` con 4 y con 5 intentos, y el panel de alertas (o la API `_find` de alerts) con 0 y 1 alertas.
- **CA10:** la consola de la app con Logstash detenido y una petición respondida.

**Propuesta:** que el humano junte esas capturas siguiendo el README (ya estaba previsto) y que yo las agregue a este verify. Mientras tanto, CA06 a CA10 quedan "pendientes de evidencia". No bloquea lo demás.

### O2 (baja): `LOGIN_FALLIDO` registra el login tal como se escribió

La spec lo pide ("login intentado") y los emails se enmascaran. Pero si alguien escribe la contraseña en el campo de usuario, esa contraseña queda en ES en texto plano. Es un riesgo conocido de registrar los usernames fallidos. **Propuesta:** aceptarlo y dejarlo asentado en `logs-elk.md`, o truncar o enmascarar los logins que no existen. Decide el documentador.

### O3 (baja): el stack de `ERROR_INESPERADO` puede llevar valores

`ERROR_INESPERADO` loguea la excepción completa: es el detalle que pide la spec, y no llega al cliente. Algunos mensajes del driver de Postgres incluyen valores, por ejemplo `Key (email)=(x@y) already exists` de una violación de unicidad. Hoy esos casos se validan antes de llegar a la base, así que no lo pude provocar. **Propuesta:** dejarlo como riesgo conocido en `logs-elk.md`.

### O4 (baja, despliegue): ELK sin seguridad

`elk.yml` tiene `xpack.security.enabled=false`, con los puertos atados a `127.0.0.1`. Para el entorno local de la defensa está bien, y el yml lo advierte. Los índices tienen logins e IPs. Si alguna vez se despliega fuera de la PC, hace falta activar la seguridad. Ya está en las trampas conocidas de la 004.

## 4. Revisión del diff (`7eaf9fc^1..7eaf9fc`)

- **Dependencias nuevas:**
  - `logstash-logback-encoder` 9.0.
  - `spring-boot-micrometer-tracing-brave` y `micrometer-tracing-bridge-brave`, con la versión del BOM de Boot.
  - Están justificadas en el plan §4.
- **Secretos:** ninguno. `.env.example` suma `KIBANA_ENCRYPTION_KEY=<…>`, con un placeholder y el comando para generarla. `elk.yml` la exige con `:?`.
- **Archivos generados modificados** (`LoggingAspect`, `MailService`, `UserService`, `UserResource`, `DomainUserDetailsService`, `AuthenticateController`, `ExceptionTranslator`, `SecurityConfiguration`): todos por la regla 3 o por CA11 y S7. No se agregó lógica de negocio.
- **Archivos nuevos:** `AccesoDenegadoHandler` y `PeticionHttpFilter`, en territorio del desarrollador.

## 5. Evidencia

- **CI:** run 44801833637 (`00cfe64`, PR #18) en **verde**. El CI corre con los logs OFF, así que `ObservabilidadIT` captura con su propio appender (`CapturaDeLogs`, nivel INFO en `com.trackergastos`).
- **En local** (`verify/003-004` sobre `main`): `npm run ci:backend:test` **BUILD SUCCESS**, 78 unitarios y 241 de integración, con `ObservabilidadIT` 13/13 y `LoggingAspectTest`. El stack que aparece en el log de la corrida es el `ERROR_INESPERADO` que fuerza a propósito `ca05ErrorSinFiltracion`.
- **CA06 a CA10:** sin evidencia propia (O1).
