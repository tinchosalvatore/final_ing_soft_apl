# Plan — Spec 000 (generación base)

- Autor: agente coder (sesión `coder`, 2026-11-20)
- Estado: Borrador (decisiones §6 revisadas con el documentador; falta la aprobación del humano)

<!-- Lo escribe el implementador ANTES de codear. No se codea hasta que el humano lo apruebe. -->

## 1. Resumen de la solución

La app ya está generada con `generator-jhipster@9.4.0` desde `tracker.jh` (commit `chore: generación base desde tracker.jh`). Este plan cubre la configuración base que va encima: 12 factors III, VII y IX, y seguridad S5 y S6. No hay lógica de negocio.

Para **S5** el mecanismo propuesto es **un changelog nuevo de Liquibase con `context="prod"`** que desactiva `user` y reemplaza el hash de la contraseña de `admin` por uno que llega en una variable de entorno. Es declarativo, queda versionado (factor XII) y no agrega código Java.

## 2. Archivos que se tocan

| Archivo | Territorio | Qué cambia |
|---|---|---|
| `src/main/resources/config/liquibase/changelog/20261120000000_prod_usuarios_por_defecto.xml` (nuevo) | Compartido | Changeset `context="prod and !secret-samples"`: `UPDATE jhi_user SET activated=false WHERE login='user'` y `UPDATE jhi_user SET password_hash='${adminPasswordHash}' WHERE login='admin'` |
| `src/main/resources/config/liquibase/master.xml` | Compartido | `include` del changelog nuevo **arriba** del needle `jhipster-needle-liquibase-add-incremental-changelog` (sin tocar el needle) |
| `src/main/resources/config/application-prod.yml` | Compartido | `spring.liquibase.parameters.adminPasswordHash: ${TRACKER_ADMIN_PASSWORD_HASH}` (sin default: si falta, la app no arranca en prod). `spring.datasource.url: ${SPRING_DATASOURCE_URL}` sin default (saca el `localhost` del yml de prod). `server.port: ${SERVER_PORT:8080}`. `spring.lifecycle.timeout-per-shutdown-phase: 20s` (`server.shutdown: graceful` ya viene generado) |
| `src/main/resources/config/application-secret-samples.yml` | Compartido | `spring.liquibase.contexts: prod, secret-samples` y `spring.liquibase.parameters.adminPasswordHash` con un valor de muestra. Así el perfil de muestras (CI/E2E y el compose local `app.yml`) **no** aplica S5 y los E2E pueden seguir logueando con `user/user` y `admin/admin` |
| `.env.example` (nuevo) | Compartido | Todas las variables de `12-factors.md` §2 + `TRACKER_ADMIN_PASSWORD_HASH`, con placeholders y el comando para generar cada valor. Sin valores reales |
| `docs/catedra/12-factors.md` §2 | (doc) | Sumar `TRACKER_ADMIN_PASSWORD_HASH` a la tabla, como exige la guía. **Lo coordino con el documentador** |
| `AGENTS.md` §3 | (doc) | Revisado: los scripts generados coinciden con la tabla. Solo cambia si algo de este plan lo requiere |
| `docs/specs/000-base/tasks.md` (nuevo) | Desarrollador | Seguimiento |

Los tests de integración (`src/test/...`) corren con el perfil `testdev`/contexto `test`, así que el changeset de prod no los afecta.

## 3. Dónde vive cada regla de negocio

No aplica: el paso 000 no tiene reglas de negocio.

| Requisito | Dónde |
|---|---|
| S5: `user` desactivado en prod | Changelog nuevo, `context="prod and !secret-samples"` |
| S5: contraseña de `admin` desde variable de entorno | Mismo changelog + `spring.liquibase.parameters.adminPasswordHash` ← `TRACKER_ADMIN_PASSWORD_HASH` |
| S6: secreto JWT por variable de entorno | Ya resuelto por JHipster 9.4: `application-prod.yml` **no trae** secreto; se lee de `JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64_SECRET` (relaxed binding de Spring). Ver §5 |
| III: configuración por entorno | `application-prod.yml` con placeholders + `.env.example` |
| VII: puerto | `server.port: ${SERVER_PORT:8080}` |
| IX: apagado ordenado | `server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase: 20s` en prod |

## 4. Tecnologías y dependencias nuevas

Ninguna.

## 5. Decisiones tomadas por el agente

1. **Generación sin pisar archivos propios.** El `--force` de JHipster quedó bloqueado; se generó en una carpeta vacía y se copió al repo sin sobrescribir nada (equivalente a responder `n` a todo). Solo hubo colisión en `.gitignore` (fusionado: se conservan `docs/enunciado.md`, `docs/defense/`, `.env`) y `tracker.jh` (idéntico).
2. **La variable trae un hash BCrypt, no la contraseña en claro.** Así la contraseña nunca está en texto plano ni en el entorno del contenedor. Se genera con `htpasswd -bnBC 10 "" 'la-contraseña' | tr -d ':\n'` (Spring acepta prefijos `$2a$`/`$2b$`/`$2y$`); el comando queda en `.env.example`.
3. **Hallazgo de JHipster 9.4 sobre S6:** el secreto JWT de ejemplo ya no está en `application-prod.yml`, sino en un perfil aparte `secret-samples` (`application-secret-samples.yml`), que `src/main/docker/app.yml` y el script `ci:e2e:server:start` activan junto con `prod`. Ese perfil es aceptable para CI/E2E, pero **no** para un despliegue real. Un despliegue real arranca **solo** con `prod` (sin `secret-samples`) y toma los secretos del entorno; si falta alguno, no arranca.
4. **S5 no se aplica con `secret-samples`.** Los E2E generados (`support/commands.ts`) loguean por defecto con `user/user` y `admin/admin`, y el CI los corre contra el jar con `prod,secret-samples`. Por eso el changeset se excluye cuando está activo el perfil de muestras: ese perfil ya es, por definición, "secretos de ejemplo".

## 6. Decisiones pendientes (para el humano)

Revisadas con el documentador (2026-11-20). Falta la aprobación del humano.

1. **Mecanismo de S5:** el changeset de Liquibase con `context="prod and !secret-samples"` y el hash BCrypt por variable de entorno. Se descartó un `ApplicationRunner` Java con `@Profile("prod")` que leyera la contraseña en claro y la hasheara en cada arranque: agrega código que corre en cada inicio y deja la contraseña en texto plano en el entorno.
2. **Rotación:** el hash se aplica una sola vez. Después, `admin` cambia la contraseña desde la UI (Cuenta → Contraseña).
3. **`secret-samples` sin S5:** no se tocan el pipeline ni `src/main/docker/app.yml`. Un despliegue real es solo el perfil `prod` más variables de entorno (`12-factors.md` §2). El agente seguridad lo audita en el `verify.md`.

## 7. Riesgos

- Si alguien levanta prod sin `TRACKER_ADMIN_PASSWORD_HASH`, `SPRING_DATASOURCE_URL` o el secreto JWT, la app **no arranca** (fail-fast). Es intencional y queda explicado en `.env.example`.
- Si alguien despliega de verdad con `secret-samples` activo, quedan `admin/admin`, `user/user` y el secreto JWT de ejemplo. Lo mitiga la decisión 3 (documentarlo) y lo audita el agente seguridad en el `verify.md`.
- Sin el perfil de muestras no hay una prueba automática de que el changeset de prod corre bien. Lo verifico a mano levantando el jar con `prod` contra el Postgres de `src/main/docker/postgresql.yml` y queda anotado en `tasks.md`.
- **Limitación conocida:** cambiar `TRACKER_ADMIN_PASSWORD_HASH` después del primer arranque no tiene efecto, porque el changeset ya corrió en esa base. La contraseña se rota desde la UI.
- **Gitleaks:** `application-secret-samples.yml` trae un `base64-secret` de alta entropía y el paso 1 del pipeline puede marcarlo. Si pasa, se agrega a `.gitleaksignore` el fingerprint de ese hallazgo, con el comentario "secreto de muestra de JHipster, solo perfil secret-samples" (`ci.md` §2.5). La regla no se desactiva.
- **`$` del hash BCrypt en el `.env` de docker compose:** compose lee cada `$` como una variable. En `.env.example` se indica escribir el valor entre comillas simples o con `$$`. Si no, la contraseña de `admin` queda mal sin ningún error.
- **Contextos de Liquibase:** se verifica con dos corridas que, con `contexts: prod, secret-samples`, el changeset **no** corre y que, con solo `prod`, **sí** corre. La evidencia queda en `tasks.md` (tarea 8).
