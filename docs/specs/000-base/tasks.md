# Tareas — Spec 000 (generación base)

<!-- Lo escribe el implementador a partir del plan aprobado. Es la memoria de qué se hizo y qué no:
     se actualiza al cerrar cada tarea, así una sesión nueva puede retomar sin releer todo. -->

| # | Tarea | Criterios que cubre | Estado | Commit |
|---|---|---|---|---|
| 1 | Commitear docs pendientes del documentador en `main` y crear `spec/000-base` | — | hecha | `f2f4d28` |
| 2 | Generar con `generator-jhipster@9.4.0 jdl tracker.jh` y fusionar `.gitignore` | agentes-sdd §3 pasos 1-3 | hecha | `86053eb` |
| 3 | `./mvnw -ntp verify` (58 unit + 170 IT) y `npm test` (165) en verde | paso 4 | hecha | — |
| 4 | Revisar tabla de comandos de `AGENTS.md` §3 contra `package.json` | paso 5 | hecha (coincide, sin cambios) | — |
| 5 | `plan.md` con el mecanismo de S5 | paso 6 | hecha (aprobado por el humano 2026-11-20) | `69d2632` |
| 6 | Factores III, VII, IX en `application-prod.yml` + `.env.example` | paso 6 | hecha | ver PR |
| 7 | S5: changelog `context="prod and !secret-samples"` (con precondición BCrypt) + claves `samples.*` en `application-secret-samples.yml` | S5 | hecha | ver PR |
| 8 | Verificar con dos corridas contra Postgres: con contexts `prod, secret-samples` el changeset NO corre; con solo `prod` SÍ corre (user desactivado, admin con el hash del entorno). Evidencia acá abajo | S5 | hecha | — |
| 9 | Push y PR a `main` (con confirmación del humano) | paso 7 | pendiente | |

## Evidencia de la tarea 8 (2026-11-20)

Jar construido con `./mvnw -ntp -Pprod -DskipTests package`, corrido contra un Postgres limpio (`src/main/docker/postgresql.yml`, borrado entre corridas con `down -v`). Contraseña de prueba: `Prueba-S5-2026`, con su hash generado por `htpasswd -bnBC 10`.

| Corrida | Perfiles / variables | Arranca | Changeset S5 en `databasechangelog` | `user` activado | `admin/admin` | `user/user` | `admin/<nueva>` |
|---|---|---|---|---|---|---|---|
| 1 | `e2e,secret-samples,prod` (orden del CI), sin variables | sí | 0 (no corrió) | true | 200 | 200 | 401 |
| 2 | `prod` + `SPRING_DATASOURCE_*`, JWT, `TRACKER_ADMIN_PASSWORD_HASH` | sí | 1 (corrió) | **false** | **401** | 500 (ver nota) | **200** |
| 3 | `prod` sin `TRACKER_ADMIN_PASSWORD_HASH` | **no**: `TRACKER_ADMIN_PASSWORD_HASH falta o no es un hash BCrypt (ver .env.example)` | — | — | — | — | — |
| 4 | `prod` + `SERVER_PORT=9090` | sí, responde `/management/health/readiness` en 9090 | — | — | — | — | — |

Además, `./mvnw -ntp verify` y `npm test` pasan después de los cambios.

## Notas para la próxima sesión

- La generación se hizo en una carpeta vacía y se copió sin pisar archivos (el `--force` está bloqueado por permisos).
- JHipster 9.4 pone el secreto JWT de ejemplo en el perfil `secret-samples`, no en `application-prod.yml`. Lo activan `src/main/docker/app.yml` y `ci:e2e:server:start`.
- Los E2E generados loguean con `user/user` (`src/test/javascript/playwright/support/commands.ts`), por eso S5 se excluye con `secret-samples`.
- **Perfiles y orden:** el CI activa `e2e,secret-samples,prod`, y con ese orden `prod` pisa lo que defina `secret-samples`. Por eso `application-prod.yml` no comparte claves con `secret-samples`: lee `samples.*` como fallback (`${VAR:${samples.x}}`), y esas claves solo existen en el perfil de muestras.
- **Fail-fast de S5:** Spring vincula `spring.liquibase.parameters` ignorando los placeholders sin resolver. Sin la precondición del changeset, la corrida 3 arrancaba y escribía el texto del placeholder como hash de `admin`.
- **Login de un usuario desactivado devuelve 500, no 401:** es comportamiento de JHipster. `ExceptionTranslator` solo mapea `BadCredentialsException` a 401, y `UserNotActivatedException` cae en 500. No se tocó, porque no es del paso 000; lo avisé al documentador por si corresponde a la spec 004 (LOGIN_FALLIDO) o a seguridad (S7).
- **Factor IX:** `timeout-per-shutdown-phase: 20s` quedó configurado, pero no se probó apagando con una petición en curso.

