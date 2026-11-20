# Trazabilidad: dónde se aplicó cada concepto

Mapa de **concepto de la materia → dónde está en el repo → cómo se demuestra**. Lo mantiene el documentador. Los implementadores agregan la ruta real del archivo cuando la conocen (columna "Dónde").

## Requisitos del enunciado

| Concepto | Spec / guía / ADR | Dónde (código o config) | Evidencia |
|---|---|---|---|
| Aplicación JHipster | `tracker.jh`, ADR-0001 | `.yo-rc.json`, `.jhipster/` | Regenerar desde `tracker.jh` reproduce el modelo |
| PWA | Spec 003, `catedra/pwa.md`, ADR-0005 | `app/pwa/pwa.config.ts`, `app/pwa/offline-db.config.ts`, `app/shared/offline/` | E2E 003-CA01 a CA11 |
| Tests de unidad | `catedra/testing.md` | `src/test/java/**/service/*Test.java`, `*.spec.ts` | Spec 002 completa con TDD |
| Tests E2E (Playwright) | ADR-0003, `catedra/testing.md` | `src/test/javascript/playwright/e2e/negocio/` | Traces de codegen y reporte en el CI |
| Logs (ELK) | Spec 004, `catedra/logs-elk.md`, ADR-0004 | `src/main/docker/elk.yml`, `src/main/docker/elk/` | Discover, dashboard "Salud", alerta de logins fallidos |
| Integración continua | Spec 005, `catedra/ci.md`, ADR-0006 | `.github/workflows/pipeline.yml` | PRs en verde y en rojo |

## 12 factors
Detalle en `catedra/12-factors.md`.

| Factor | Dónde |
|---|---|
| I Código base | Un repo, ramas `spec/*` |
| II Dependencias | `pom.xml`, `package-lock.json`, versiones fijas, Trivy |
| III Configuración | `application-prod.yml` con variables de entorno, `.env.example` |
| IV Servicios de respaldo | `SPRING_DATASOURCE_URL`, `JHIPSTER_LOGGING_LOGSTASH_HOST` |
| V Build/release/run | CI + Jib + `app.yml` |
| VI Sin estado | JWT (ADR-0001) |
| VII Puertos | `SERVER_PORT` |
| IX Desechabilidad | `server.shutdown: graceful` |
| X Paridad | ADR-0002 (Postgres en dev) |
| XI Logs | Spec 004, ADR-0004 (con concesión) |
| XII Administración | Changelogs de Liquibase (incluidas las categorías iniciales) |
| 14 Telemetría | Spec 004 |
| 15 AuthN/AuthZ | Spec 001, ADR-0007 |
| No aplicados | VIII, 13: justificado en `catedra/12-factors.md` §3 |

## Seguridad
Detalle en `catedra/seguridad.md`.

| Defensa | OWASP | Dónde | Test |
|---|---|---|---|
| S1 Propiedad verificada en el backend | A01 | `GastoService` | 001-CA07, CA08 |
| S2 Filtro forzado al dueño | A01 | `GastoQueryService` | 001-CA06 |
| S3 Dueño asignado por el servidor | A01 / Manipulación | `GastoService` | 001-CA04 |
| S4 Validación en el backend y límites de negocio | A06 | DTOs + services | 001-CA02, CA03; 002-CA10 |
| S5 Sin contraseñas por defecto en prod | A02 | Liquibase + config prod | `verify.md` de 000 |
| S6 Secreto JWT por entorno | A04 | `application-prod.yml` | Gitleaks |
| S7 Errores sin filtrar información interna | A10 | `ExceptionTranslator` | 004-CA05 |
| S8 Eventos de seguridad y alerta | A09 / A07 | Spec 004 | 004-CA01, CA03, CA08 |
| S9 Limpieza local al cerrar sesión | Datos sensibles | Módulo offline (`app/shared/offline/`) | 003-CA09, CA10 |
| S10 Solo ADMIN escribe categorías | A01 | `CategoriaResource` | 001-CA11 |
| Controles en el CI | A03, A05, A08 | `pipeline.yml` | Spec 005 |

## Prácticas de agentes (cátedra)
Detalle en `catedra/agentes-sdd.md`.

| Práctica | Dónde |
|---|---|
| Fuente única de verdad del modelo | `tracker.jh` |
| `AGENTS.md` (producto, territorios, comandos, reglas, mapa, criterios) | `AGENTS.md` |
| Territorios y needles de JHipster | `AGENTS.md` §2 |
| SDD: spec → plan → tasks → implement → verify | `docs/specs/NNN-*/` |
| Formato de spec (6 secciones, criterios en tabla) | `docs/specs/*/spec.md` |
| ADRs | `docs/adr/` |
| Tests escritos por un agente distinto | Rol tester, `catedra/testing.md` |
| Agente revisor sin participación previa | `verify.md` |
| Autonomía según qué tan reversible es la acción | `catedra/agentes-sdd.md` §4 |
| Context engineering (recuperación bajo demanda, sesiones nuevas, planes en archivos) | `AGENTS.md` §5, `tasks.md` |
| ArchUnit para proteger la arquitectura | `TechnicalStructureTest` |
