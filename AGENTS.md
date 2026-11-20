# AGENTS.md — Tracker de Gastos

Guía global para cualquier agente que trabaje en este repo. Leela entera antes de empezar. Leé el resto de la documentación **solo cuando la tarea lo necesite** (la sección 5 dice dónde está cada cosa).

---

## 1. Producto y stack

**Qué es:** una app web (instalable como PWA) para que cada persona registre sus gastos, los categorice y vea un resumen mensual. Es el trabajo final de Ingeniería de Software Aplicada; además del producto, tiene que demostrar PWA, tests de unidad y E2E, logs con ELK, CI, 12 factors y seguridad.

**Stack** (generado con JHipster a partir de `tracker.jh`; ver [ADR-0001](docs/adr/ADR-0001-monolito-jhipster-react-jwt.md)):

| Capa | Tecnología |
|---|---|
| Generador | JHipster 9.4 (`generator-jhipster`) |
| Backend | Java 21, Spring Boot, Maven (`./mvnw`) |
| Arquitectura | Monolito en capas: `web.rest` (Resource) → `service` → `repository` → `domain` |
| Persistencia | PostgreSQL en **todos** los entornos ([ADR-0002](docs/adr/ADR-0002-postgresql-en-desarrollo.md)), migraciones con Liquibase |
| Autenticación | JWT (stateless) |
| Frontend | React + TypeScript, Vite |
| PWA | Workbox ([ADR-0005](docs/adr/ADR-0005-pwa-workbox-indexeddb.md)) |
| Tests | JUnit 5 + Testcontainers + ArchUnit (back), Vitest (front), Playwright (E2E, [ADR-0003](docs/adr/ADR-0003-playwright-e2e.md)) |
| Logs | Logback → Logstash → Elasticsearch → Kibana ([ADR-0004](docs/adr/ADR-0004-logs-appender-logstash.md)) |
| CI | GitHub Actions ([ADR-0006](docs/adr/ADR-0006-pipeline-ci.md)) |

Idioma: el código (clases, métodos, variables) sigue las convenciones de JHipster en inglés, salvo los nombres de entidades del dominio, que están en español (`Gasto`, `Categoria`). Los documentos y los mensajes de commit van en español.

---

## 2. Territorios del repo

JHipster regenera código. Antes de editar un archivo, fijate en qué territorio está.

| Territorio | Qué incluye | Regla |
|---|---|---|
| **Generador** | `domain/*` (entidades), `service/dto/*`, `service/mapper/*`, `repository/*` generados, componentes CRUD del front en `src/main/webapp/app/entities/*`, `.jhipster/*.json`, `.yo-rc.json` | **No se editan a mano.** Para cambiarlos: editar `tracker.jh` y regenerar (sección 4). Si algo de acá tiene que cambiar sí o sí, se deja asentado en un ADR y se marca el archivo como ignorado en la regeneración (`i`). |
| **Compartido** | `src/main/resources/config/*.yml`, `config/liquibase/**` (changelogs y `master.xml`), `pom.xml`, `package.json`, `vite.config.ts`, `src/main/webapp/index.html`, `src/main/docker/*` | Se pueden editar, pero **respetando los needles** (comentarios `jhipster-needle-*`): nunca borrarlos ni escribir debajo de ellos. Las migraciones existentes no se modifican: un cambio de esquema es un changelog nuevo. |
| **Desarrollador** | Lógica de negocio en `service/*Service.java` (y clases nuevas en `service/`), endpoints nuevos en `web/rest/`, la PWA (`src/main/webapp/app/pwa/` y `app/shared/offline/`), los tests nuevos, `src/main/docker/elk.yml` y `src/main/docker/elk/`, `.github/workflows/` | Es donde va el código propio. Al regenerar, cualquier conflicto sobre estos archivos se responde con `n` (omitir) o `i` (ignorar siempre). |

---

## 3. Comandos

> Requisitos locales: Java 21 o 25, Node `^22.18` o `>=24.11` (lo exige JHipster 9.4; recomendado 24.21), Docker.

| Para | Comando |
|---|---|
| Generar o regenerar desde el modelo | `npx generator-jhipster@9.4.0 jdl tracker.jh` |
| Levantar el backend (dev, levanta Postgres por docker compose) | `./mvnw` |
| Levantar el frontend con recarga en caliente | `npm start` |
| Tests de backend (unitarios + integración + ArchUnit) | `./mvnw -ntp verify` |
| Tests de frontend (Vitest) | `npm test` |
| Tests E2E (con la app corriendo) | `npm run e2e:headless` (`npm run e2e` los corre con el navegador visible) |
| Tests E2E levantando la app | `npm run e2e:dev` |
| Lo mismo que corre el CI | `npm run ci:backend:test`, `npm run ci:frontend:test`, `npm run ci:e2e:package && npm run ci:e2e:prepare && npm run ci:e2e:run` |
| Grabar un E2E nuevo | `npx playwright codegen http://localhost:8080` |
| Ver la grabación o el trace de un E2E | `npx playwright show-trace <archivo>.zip` |
| Levantar ELK | `docker compose -f src/main/docker/elk.yml up -d` |
| Build de producción | `./mvnw -ntp -Pprod clean verify` |

Si un comando de esta tabla deja de ser correcto (porque JHipster lo generó distinto), actualizá la tabla en el mismo commit.

---

## 4. Reglas

1. **Seguí el flujo SDD** ([guía](docs/catedra/agentes-sdd.md)): no se escribe código sin una `spec.md` aprobada y un `plan.md` aprobado por el humano.
2. **Hacé solo lo que te pide tu rol** (sección 6). El agente que escribe los tests no escribe la lógica, y al revés.
3. **El modelo de datos se cambia solo desde `tracker.jh`**: commit limpio → editar `tracker.jh` → regenerar → resolver conflictos → revisar el diff.
4. **Nunca pongas secretos en el repo ni en el contexto**: contraseñas, claves JWT, tokens. Todo valor que cambia entre entornos se lee de una variable de entorno y se documenta en `.env.example` ([guía 12 factors](docs/catedra/12-factors.md)).
5. **El control de acceso se verifica en el backend.** Ningún endpoint de `Gasto` devuelve ni modifica un gasto que no sea del usuario autenticado ([spec 001](docs/specs/001-gastos/spec.md)).
6. **Los logs no llevan datos sensibles**: ni contraseñas, ni tokens, ni montos ni descripciones de gastos ([guía logs](docs/catedra/logs-elk.md)).
7. **Si para implementar o testear tenés que decidir algo que la spec no dice, frená y preguntá.** No inventes reglas de negocio. Anotá la duda en el `plan.md` como "Decisión pendiente".
8. **Una rama por spec** (`spec/NNN-nombre`), con commits chicos y un PR a `main`. El CI tiene que quedar en verde.
9. **Si el agente que implementa toma una decisión estructural, escribe un ADR** en `docs/adr/` con la plantilla de [docs/adr/README.md](docs/adr/README.md).
10. **No agregues dependencias "por las dudas".** Cada dependencia nueva va con versión fija y se justifica en el `plan.md`.

---

## 5. Mapa: dónde está cada cosa

| Necesito saber... | Leé |
|---|---|
| El modelo de datos (entidades, campos, relaciones) | `tracker.jh` |
| Qué tiene que hacer una funcionalidad | `docs/specs/NNN-*/spec.md` |
| Cómo se planeó y en qué quedó una funcionalidad | `docs/specs/NNN-*/plan.md`, `tasks.md`, `verify.md` |
| Por qué algo es como es | `docs/adr/` (índice en `docs/adr/README.md`) |
| La arquitectura (nodos, subsistemas) | `docs/arquitectura.md` |
| Cómo aplicar un concepto de la materia | `docs/catedra/<tema>.md`: `agentes-sdd`, `12-factors`, `seguridad`, `pwa`, `testing`, `logs-elk`, `ci` |
| Dónde quedó aplicado cada concepto | `docs/trazabilidad.md` |
| Plantillas de spec, plan, tasks y verify | `docs/specs/_plantillas/` |

`docs/defense/` y `docs/enunciado.md` son notas personales del humano: **no las leas ni las modifiques**.

### Specs

| Spec | Tema | Rama |
|---|---|---|
| [001](docs/specs/001-gastos/spec.md) | Gastos y categorías (dominio y control de acceso) | `spec/001-gastos` |
| [002](docs/specs/002-resumen-mensual/spec.md) | Resumen mensual (lógica de negocio) | `spec/002-resumen-mensual` |
| [003](docs/specs/003-pwa-offline/spec.md) | PWA y carga de gastos sin conexión | `spec/003-pwa-offline` |
| [004](docs/specs/004-observabilidad/spec.md) | Observabilidad (logs, dashboard, alerta) | `spec/004-observabilidad` |
| [005](docs/specs/005-integracion-continua/spec.md) | Integración continua | `spec/005-integracion-continua` |

Orden de implementación (detalle en `docs/catedra/agentes-sdd.md` §2): 000 (generación base, sin spec) → 001 → 002 → 005 → 003 → 004. El CI va antes de la PWA y los logs para que todo lo que sigue entre por PR con el pipeline andando. Hasta que exista la app (paso 000), el pipeline falla en el paso de Java: es esperado.

---

## 6. Roles y criterios de desarrollo

Hay **un agente por tema**. Cada uno es una sesión con nombre propio (`/rename <agente>`), dueña de su tema: lo implementa y después el humano la usa para consultar y estudiar ese tema. Lo que define a un agente es **su rol y sus archivos**, no el chat: si una sesión se llena de contexto, se abre una nueva con el mismo nombre que retoma desde `tasks.md` y `verify.md`.

| Agente (sesión) | Specs | Hace | Guía | No toca |
|---|---|---|---|---|
| **documentador** | todas | Specs, ADRs, guías, `tracker.jh`, este archivo. Prepara el pedido inicial de cada agente | `agentes-sdd.md` | código |
| **coder** | 000, 001, 002 | Genera la app con JHipster y escribe la lógica de negocio: `plan.md`, `tasks.md`, código | `agentes-sdd.md`, `12-factors.md` | los tests del agente testing (si uno parece estar mal, avisa; no lo modifica) |
| **testing** | 001, 002 (+ E2E de todas) | Escribe los tests de los criterios **antes** de que el coder implemente (fase roja). Graba los E2E con codegen | `testing.md` | código de producción |
| **ci** | 005 | Termina `.github/workflows/pipeline.yml` y la protección de `main` | `ci.md` | código de la app |
| **pwa** | 003 | Service worker con Workbox, manifest, módulo offline | `pwa.md` | lógica de backend |
| **observabilidad** | 004 | Logs estructurados, ELK, dashboard y alerta | `logs-elk.md` | reglas de negocio |
| **seguridad** | todas (revisión) | Es el **verificador**: no programa nada, así que revisa sin sesgo. Escribe el `verify.md` de cada spec y audita S1 a S10 y 12 factors en cada PR | `seguridad.md`, `12-factors.md` | código y tests |

Los agentes pwa y observabilidad escriben sus propios tests de integración y unitarios. Los E2E de sus criterios los graba el agente testing, para mantener separado quien testea de quien implementa.

**Criterios de desarrollo:**
- Las reglas de negocio van en la capa `service`, nunca en el `Resource` ni en el front. El front puede validar para mejorar la experiencia, pero el backend valida siempre.
- Las clases nuevas siguen el patrón Controller(Resource) → Service → Repository. ArchUnit lo controla.
- Los tests nombran el criterio que cubren, por ejemplo `// Criterio 001-CA03` o `test('CA03 - ...')`, para que el verificador pueda trazarlos.
- Los E2E se graban con `codegen` y se dejan en `src/test/javascript/playwright/e2e/` con extensión `.pw.ts`, usando locators por rol o texto ([guía testing](docs/catedra/testing.md)).
- La tarea está terminada cuando: compila, los tests propios y los del tester pasan, el CI está en verde y `tasks.md` está actualizado.
