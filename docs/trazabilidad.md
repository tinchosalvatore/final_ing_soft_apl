# Trazabilidad: dónde se aplicó cada concepto

Mapa de **concepto de la materia → dónde está en el repo → cómo se demuestra**. Lo mantiene el documentador. Las rutas son las reales, salvo las marcadas *(en curso)*.

## Estado de las specs

| Spec | PR | Verify (agente seguridad) |
|---|---|---|
| 000 Generación base | #1 | [Cumple con observaciones](specs/000-base/verify.md) |
| 001 Gastos y categorías | #5 + #15 (fix) | [Cumple con observaciones](specs/001-gastos/verify.md) |
| 002 Resumen mensual | #6 | [Cumple con observaciones](specs/002-resumen-mensual/verify.md) |
| 003 PWA offline | *(en curso)* | pendiente |
| 004 Observabilidad | #18 | pendiente |
| 005 Integración continua | #4 + #12 (verify) | [Cumple](specs/005-integracion-continua/verify.md) |

## Requisitos del enunciado

| Concepto | Spec / guía / ADR | Dónde (código o config) | Evidencia |
|---|---|---|---|
| Aplicación JHipster | `tracker.jh`, ADR-0001, ADR-0008 | `.yo-rc.json`, `.jhipster/`, `.yo-resolve` | Regenerar desde `tracker.jh` reproduce el modelo; `.yo-resolve` protege los archivos generados que reescribimos |
| PWA | Spec 003, `catedra/pwa.md`, ADR-0005 | `src/main/webapp/app/pwa/pwa.config.ts`, `app/pwa/offline-db.config.ts` *(en curso)* | E2E 003-CA01 a CA11 |
| Tests de unidad | `catedra/testing.md` | `service/resumen/ResumenMensualCalculadoraTest.java`, `MesResumenTest.java`, `app/modules/*/*.spec.ts` | Spec 002 con TDD: PR #6 en rojo antes del código |
| Tests de integración | `catedra/testing.md` | `web/rest/GastoControlAccesoIT.java`, `GastoValidacionIT.java`, `GastoPatchIT.java`, `CategoriaReglasIT.java`, `RegistroDeshabilitadoIT.java`, `UsuarioConGastosIT.java`, `ResumenResourceIT.java`, `observabilidad/ObservabilidadIT.java` | Testcontainers con Postgres real, en el CI |
| Tests de arquitectura | ADR-0001 | `TechnicalStructureTest.java` (ArchUnit) | En cada build |
| Tests E2E (Playwright) | ADR-0003, `catedra/testing.md` | `src/test/javascript/playwright/e2e/negocio/001-gastos.pw.ts`, `002-resumen-mensual.pw.ts` | Reporte y traces en el artefacto del CI. La regrabación con codegen está pendiente |
| Logs (ELK) | Spec 004, `catedra/logs-elk.md`, ADR-0004 | `src/main/docker/elk.yml`, `src/main/docker/elk/` (`logstash.conf`, `kibana/objetos.ndjson`, `probar-alerta.sh`, `README.md`), `web/filter/PeticionHttpFilter.java`, `config/LoggingConfiguration.java` | Discover por `traceId`, dashboard "Salud – Tracker de Gastos", regla "Logins fallidos (5 en 5 min)" |
| Integración continua | Spec 005, `catedra/ci.md`, ADR-0006 | `.github/workflows/pipeline.yml`, `.gitleaksignore`, protección de `main` | PRs #7 a #11: un rojo por cada control; #4 en verde |

## 12 factors
Detalle en `catedra/12-factors.md`.

| Factor | Dónde |
|---|---|
| I Código base | Un repo, una rama por spec, PRs a `main` protegida |
| II Dependencias | `pom.xml` (con overrides de Tomcat y Jackson por CVE), `package-lock.json`, Trivy en el CI |
| III Configuración | `application-prod.yml` con `${VAR}`, `.env.example` (`TRACKER_ADMIN_PASSWORD_HASH`, `SPRING_MAIL_HOST`, `KIBANA_ENCRYPTION_KEY`, etc.) |
| IV Servicios de respaldo | `SPRING_DATASOURCE_URL`, `JHIPSTER_LOGGING_LOGSTASH_HOST` |
| V Build/release/run | CI + Jib + `src/main/docker/app.yml` |
| VI Sin estado | JWT (ADR-0001) |
| VII Puertos | `server.port: ${SERVER_PORT:8080}` |
| IX Desechabilidad | `server.shutdown: graceful` + `timeout-per-shutdown-phase` |
| X Paridad | ADR-0002: Postgres en dev, Testcontainers en los tests |
| XI Logs | Spec 004, ADR-0004 (concesión documentada: el appender envía a Logstash) |
| XII Administración | Liquibase: `20261120000000_prod_usuarios_por_defecto.xml` (con `runOnChange`), `20261120000100_categorias_iniciales.xml` |
| 14 Telemetría | Spec 004: logs + métricas derivadas de `PETICION_HTTP` + `traceId` |
| 15 AuthN/AuthZ | Spec 001, ADR-0007 |
| No aplicados | VIII, 13: justificado en `catedra/12-factors.md` §3 |

## Seguridad
Detalle en `catedra/seguridad.md`.

| Defensa | OWASP | Dónde | Test |
|---|---|---|---|
| S1 Propiedad verificada en el backend | A01 | `service/GastoService.java` | `GastoControlAccesoIT` (001-CA07, CA08) |
| S2 Filtro y conteo forzados al dueño | A01 | `service/GastoQueryService.java`, `GastoSpecifications.java` | `GastoControlAccesoIT` (001-CA06, `/count`) |
| S3 Dueño asignado por el servidor | A01 / Manipulación | `service/GastoService.java` | 001-CA04 |
| S4 Validación en el backend y límites de negocio | A06 | `GastoService.validar`, `service/resumen/MesResumen.java` | `GastoValidacionIT`, `GastoPatchIT`, `MesResumenTest` |
| S5 Sin contraseñas por defecto en prod | A02 | `20261120000000_prod_usuarios_por_defecto.xml` | `verify.md` del 000 y del fix |
| S6 Secreto JWT por entorno | A04 | `application-prod.yml`; perfil `secret-samples` solo para pruebas | Gitleaks |
| S7 Errores sin filtrar información interna | A10 / A07 | `web/rest/errors/ExceptionTranslator.java` | `ObservabilidadIT` (004-CA05, CA11), `ExceptionTranslatorIT` |
| S8 Eventos de seguridad y alerta | A09 / A07 | `AuthenticateController.java` (`LOGIN_FALLIDO`), `GastoService` (`GASTO_AJENO`), regla de Kibana | `ObservabilidadIT`, `probar-alerta.sh` |
| S9 Limpieza local al cerrar sesión | Datos sensibles | Módulo offline *(en curso)* | 003-CA09, CA10 |
| S10 Solo ADMIN escribe categorías | A01 | `web/rest/CategoriaResource.java` | `CategoriaReglasIT` (001-CA11) |
| R1 Sin registro público en prod | A07 | `RegistroDeshabilitadoException`, `application.registro.habilitado` | `RegistroDeshabilitadoIT` (001-CA16) |
| No borrar usuarios con gastos | Integridad | `UserService` | `UsuarioConGastosIT` (001-CA17) |
| Controles en el CI | A03, A05, A08 | `pipeline.yml` (Gitleaks, Semgrep, Trivy con cobertura) | Spec 005, PRs #7 a #11 |

## Prácticas de agentes (cátedra)
Detalle en `catedra/agentes-sdd.md`.

| Práctica | Dónde |
|---|---|
| Fuente única de verdad del modelo | `tracker.jh` |
| `AGENTS.md` (producto, territorios, comandos, reglas, mapa, criterios) | `AGENTS.md`, con las reglas 11 a 13 que salieron de errores reales |
| Territorios y needles de JHipster | `AGENTS.md` §2, ADR-0008, `.yo-resolve` |
| SDD: spec → tests → plan → tasks → implement → verify | `docs/specs/NNN-*/` |
| Contrato observable antes de la fase roja | Sección 5.1 de cada spec, que salió de las preguntas del agente testing |
| ADRs | `docs/adr/` (0001 a 0008) |
| Tests escritos por un agente distinto (TDD) | PRs #5 y #6 en borrador y en rojo antes del código |
| Agente revisor sin participación previa | `verify.md` de cada spec (agente seguridad) |
| Autonomía según qué tan reversible es la acción | `catedra/agentes-sdd.md` §4; la protección de `main` la aplicó el humano |
| Context engineering (recuperación bajo demanda, sesiones nuevas, planes en archivos) | `AGENTS.md` §5, `tasks.md`; worktrees separados por agente |
