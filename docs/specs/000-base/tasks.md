# Tareas — Spec 000 (generación base)

<!-- Lo escribe el implementador a partir del plan aprobado. Es la memoria de qué se hizo y qué no:
     se actualiza al cerrar cada tarea, así una sesión nueva puede retomar sin releer todo. -->

| # | Tarea | Criterios que cubre | Estado | Commit |
|---|---|---|---|---|
| 1 | Commitear docs pendientes del documentador en `main` y crear `spec/000-base` | — | hecha | `f2f4d28` |
| 2 | Generar con `generator-jhipster@9.4.0 jdl tracker.jh` y fusionar `.gitignore` | agentes-sdd §3 pasos 1-3 | hecha | `86053eb` |
| 3 | `./mvnw -ntp verify` (58 unit + 170 IT) y `npm test` (165) en verde | paso 4 | hecha | — |
| 4 | Revisar tabla de comandos de `AGENTS.md` §3 contra `package.json` | paso 5 | hecha (coincide, sin cambios) | — |
| 5 | `plan.md` con el mecanismo de S5 | paso 6 | en curso (esperando aprobación) | |
| 6 | Factores III, VII, IX en `application-prod.yml` + `.env.example` | paso 6 | pendiente | |
| 7 | S5: changelog `context="prod and !secret-samples"` + ajuste de `application-secret-samples.yml` | S5 | pendiente | |
| 8 | Verificar a mano el jar con perfil `prod` contra Postgres (user desactivado, admin con el hash del entorno) | S5 | pendiente | |
| 9 | Push y PR a `main` (con confirmación del humano) | paso 7 | pendiente | |

## Notas para la próxima sesión

- La generación se hizo en una carpeta vacía y se copió sin pisar archivos (el `--force` está bloqueado por permisos).
- JHipster 9.4 pone el secreto JWT de ejemplo en el perfil `secret-samples`, no en `application-prod.yml`. Lo activan `src/main/docker/app.yml` y `ci:e2e:server:start`.
- Los E2E generados loguean con `user/user` (`src/test/javascript/playwright/support/commands.ts`), por eso S5 se excluye con `secret-samples`.
