# Verificación — Paso 000 (generación base)

- Verificador: agente seguridad (sesión nueva, sin participación previa en el paso 000)
- Rama / PR: [PR #1](https://github.com/tinchosalvatore/final_ing_soft_apl/pull/1), mergeado en `main` como `d29d803`
- Fecha: 2026-11-20
- Resultado: **Cumple con observaciones**

<!-- El paso 000 no tiene spec ni criterios de aceptación. Se verifica contra docs/catedra/agentes-sdd.md §3 y docs/specs/000-base/plan.md. -->

**Resumen.** S5, S6 y los factores III, VII y IX están implementados como dice el plan. Los reproduje contra un Postgres propio y no encontré ningún secreto real en el repo. Hay una observación **media** (O1): el plan afirma que cambiar `TRACKER_ADMIN_PASSWORD_HASH` después del primer arranque "no tiene efecto", pero en realidad **prod no arranca**. Las demás observaciones son bajas o le corresponden a otras specs.

## 1. Puntos verificados

El paso 000 no tiene criterios `000-CAxx`. Esta tabla reemplaza la de criterios: cada punto que pidió el documentador, con la prueba hecha.

| # | Punto | Cómo se verificó | ¿La evidencia lo demuestra? | Resultado |
|---|---|---|---|---|
| V1 | Solo hay código propio en `86053eb..d29d803` | `git diff --stat`: 11 archivos, +234/−7. Todos están en el plan §2 o son docs coordinados con el documentador | sí | ✅ |
| V2 | S5: en prod real, `user` queda desactivado | Corrida C (§4): `jhi_user.activated = f` para `user`, y el changeset figura en `databasechangelog` con `contexts = (prod and !secret-samples)` | sí | ✅ |
| V3 | S5: `admin` usa el hash de `TRACKER_ADMIN_PASSWORD_HASH` | Corrida C: `admin/admin` → 401 y `admin/<nueva>` → 200. El hash guardado empieza con `$2y$10$` (el de `htpasswd`; el del CSV es `$2a$`) | sí | ✅ |
| V4 | S5: sin la variable, prod no arranca | La corrida 3 de `tasks.md` muestra el mensaje de la precondición `HALT`. No la repetí: el changeset y el jar son los mismos | sí | ✅ |
| V5 | S5: con `secret-samples` (CI/E2E) no se aplica | Corrida F: con `e2e,secret-samples,prod` el changeset no corre y `admin/admin` y `user/user` → 200. El CI en verde con los E2E también lo confirma | sí | ✅ |
| V6 | S6: no hay secretos reales en `application-prod.yml` | Revisión del archivo: no tiene `base64-secret` ni contraseña de base, y la URL de la base es un placeholder | sí | ✅ |
| V7 | S6: sin el secreto JWT, prod no arranca | Corrida A2: `Could not resolve placeholder 'jhipster.security.authentication.jwt.base64-secret'`. `SecurityJwtConfiguration:25` lo lee con `@Value` sin default | sí | ✅ |
| V8 | S6: `secret-samples` se usa solo en CI/E2E y en el compose de demo | `ci:e2e:server:start` (`package.json:27`) y `src/main/docker/app.yml:8`. **También** lo activa el grupo de perfiles `dev` (`application.yml:105-107`); ver O5. La imagen de Jib no fija perfiles (`pom.xml:621-624`), así que con `-Pprod` arranca solo con `prod` | parcial (falta mencionar `dev` en la doc) | ✅ con O5 |
| V9 | Factor III: configuración por entorno | `application-prod.yml` lee la URL, el hash y el secreto del entorno. `.env.example` tiene solo placeholders (`<host>`, `<contraseña>`, `<hash-bcrypt>`, `<secreto-base64>`), y los `JHIPSTER_LOGGING_*` son defaults que no son secretos. `.env` nunca se commiteó (`git log --all -- .env` vacío) | sí | ✅ con O6 |
| V10 | Factor VII: `SERVER_PORT` | Todas mis corridas usaron `SERVER_PORT=18080` y la app respondió en ese puerto. La corrida 4 de `tasks.md` hizo lo mismo con 9090 | sí | ✅ |
| V11 | Factor IX: apagado ordenado | `server.shutdown: graceful` y `spring.lifecycle.timeout-per-shutdown-phase: 20s` en prod. El `entrypoint.sh` de Jib usa `exec java`, así que el SIGTERM llega a la JVM. Con `kill -TERM` el proceso terminó solo, en menos de 6 s | **parcial**: falta probar una petición en curso durante el apagado (ver O4) | ⚠️ |
| V12 | `.gitleaksignore`: excepciones puntuales y justificadas | 5 entradas `archivo:regla:línea`, sin desactivar ninguna regla. Comprobé que cada línea apunta al valor marcado: 4 son el mismo secreto de muestra de JHipster (`MDAyMDcz…`) y la quinta es otro secreto, solo de test (`AuthenticationIntegrationTest.java:19`). Ninguno es real: prod real no arranca sin su propio secreto (V7) | sí | ✅ |
| V13 | Needles y territorios | `master.xml`: el `include` va entre dos needles, sin borrar ni editar ninguno. No se tocó ningún archivo del territorio Generador | sí | ✅ |
| V14 | Logs sin datos sensibles (S5) | `grep` del hash en los logs de las corridas C y D: 0 apariciones. El error de checksum (O1) muestra solo los md5 de Liquibase, no el hash | sí | ✅ |

### ¿Las pruebas de `tasks.md` demuestran S5?

**Sí, para el primer arranque.** Mis corridas C, F y G dieron lo mismo que las corridas 1, 2 y 3 de `tasks.md`. Tienen dos limitaciones:

1. Ninguna corrida reinicia la app sobre una base ya migrada con **otro** hash. Ese es justo el caso que el plan da por resuelto ("no tiene efecto") y que falla (O1).
2. `tasks.md` solo demuestra el fail-fast del hash. Los de `SPRING_DATASOURCE_URL` y del secreto JWT los probé acá (corridas B y A2).

No hay un test automático del changeset de prod. Es un riesgo aceptado en el plan §7.

## 2. Casos borde no contemplados

Son propuestas para las specs y el plan. **No corregí nada**: lo que haya que cambiar lo deciden el documentador y el coder.

### O1 (media): cambiar el hash después del primer arranque impide que prod arranque

- **Qué dice la documentación:** el plan §7, el comentario del changeset, `.env.example` y `tasks.md` afirman: *"Cambiar la variable después del primer arranque no tiene efecto: la contraseña se rota desde la UI."*
- **Qué pasa:** Liquibase calcula el checksum del changeset **con el parámetro ya sustituido**. Con otro valor de `TRACKER_ADMIN_PASSWORD_HASH`, el checksum cambia y la validación corta el arranque:
  ```
  ValidationFailedException: Validation Failed: 1 changesets check sum
    ...20261120000000_prod_usuarios_por_defecto.xml::20261120000000-1::coder
    was: 9:f849cfc67855d967e2dce7e0fe6adeef but is now: 9:9aa6576824bd6f9f1f7934a48af14c67
  ```
  (corrida D; la corrida E, con el mismo hash, arranca bien).
- **Impacto:** no es una vulnerabilidad: falla cerrado y no expone nada. Pero es un riesgo operativo. Si alguien "rota" la contraseña de admin cambiando la variable, como invita a pensar la documentación, prod no levanta. Además, la variable tiene que conservar para siempre el hash **original**, aunque la contraseña ya se haya cambiado desde la UI.
- **Propuesta (decide el coder con el documentador):**
  - (a) Corregir la documentación: la variable no se cambia nunca después del primer arranque.
  - (b) Sacar el valor del checksum, por ejemplo con `<validCheckSum>ANY</validCheckSum>` en ese changeset. Así cambiar la variable de verdad no tendría efecto.
  - (c) Otro mecanismo.

  Cualquiera de las tres necesita la aprobación del humano y una prueba de reinicio con otro hash en `tasks.md`.

- **Derivación (2026-11-20):** riesgo R2 en `seguridad.md` §4.1. Lo decide el humano. El documentador recomienda `runOnChange="true"`, que convierte a la variable en la fuente de verdad y lo implementa el coder en un PR chico. Si se adopta, hay que verificar que reiniciar con otro hash arranque y aplique el hash nuevo, y tener en cuenta que el hash de la variable pisa cualquier contraseña de `admin` cambiada desde la UI.

### O2 (baja): un registro anónimo borra al `user` desactivado y toma su login

- **Qué pasa:** `UserService.registerUser` (código generado por JHipster) busca el login y, si la cuenta existe pero **no está activada**, la **borra** (`removeNonActivatedUser`) y crea una nueva. En prod real, `user` queda desactivado por S5, así que cualquiera puede hacer un `POST /api/register` con `login: "user"`:
  ```
  POST /api/register {"login":"user","email":"otro@example.com",...}  → 201
  jhi_user: id 2 (user, user@localhost) desaparece; aparece id 1001 (user, otro@example.com, activated=f)
  ```
  (corrida C).
- **Impacto hoy:** bajo. La cuenta nueva queda sin activar, y la activación necesita un mail, que en prod no está configurado (`spring.mail.host: localhost`). Si algún día se configura el mail, el login `user` queda a disposición de cualquiera.
- **Cómo se generaliza:** pasa con **cualquier** cuenta que un admin desactive, no solo con `user`. Un anónimo puede borrarla y quedarse con su login. Si esa cuenta tiene gastos, el resultado depende de la FK de `gasto`, y eso no lo cubre ninguna spec.
- **Propuesta:** que el documentador decida si va como caso borde de la spec 001 (cuentas y dueño de los gastos) o como un punto nuevo de `seguridad.md` (por ejemplo, desactivar no es lo mismo que "registro pendiente"). Para el 000 alcanza con dejarlo asentado.

- **Derivación (2026-11-20):** riesgo R1 en `seguridad.md` §4.1. Lo decide el humano. El documentador propone desactivar el auto-registro en prod (los usuarios los crea el ADMIN). Si se decide, entra en la spec 001.

### O3 (ya conocido, 004-CA11): el login de un usuario desactivado responde 500, **con cualquier contraseña**

Ya está asignado a 004-CA11. Hay un detalle que la spec no recoge: el 500 sale **también con una contraseña incorrecta** (`user/wrongpass` → 500). Para comparar: un usuario inexistente → 401, y `admin` con una contraseña mala → 401. JHipster comprueba si la cuenta está activada antes de comparar la contraseña, así que el 500 revela que la cuenta existe sin saber la clave. **Propuesta:** que 004-CA11 diga "con cualquier contraseña" en lugar de "con contraseña correcta".

- **Derivación (2026-11-20):** aceptado. 004-CA11 ahora dice "con cualquier contraseña".

### O4 (baja): el apagado ordenado no tiene prueba de comportamiento

La configuración es correcta (V11), pero ni `tasks.md` ni esta verificación prueban que una petición en curso termine durante el apagado. Además, el mensaje de Spring *"Commencing graceful shutdown"* no aparece en el log de la corrida C, así que tampoco hay evidencia indirecta. **Propuesta:** si la cátedra pide evidencia del factor IX, que la sume el agente que tenga un endpoint lento a mano (por ejemplo, observabilidad o testing). No bloquea el paso 000.

- **Derivación (2026-11-20):** queda como observación; no se va a automatizar.

### O5 (baja, documentación): `secret-samples` también se activa con `dev`

`application.yml` tiene el grupo `dev: [secret-samples, api-docs]`. Es razonable para desarrollo local, pero `12-factors.md` §2 y `seguridad.md` S6 dicen "solo CI/E2E y el compose de demo". **Propuesta:** que el documentador sume "y el perfil `dev`" en las dos guías.

- **Derivación (2026-11-20):** corregido por el documentador en `12-factors.md` §2 y en `seguridad.md` S6.

### O6 (baja): detalles del factor III

- `spring.mail.host: localhost` y `port: 25` están fijos en `application-prod.yml`. Hoy no se manda mail en prod, pero si se configura tiene que pasar a variable de entorno.
- Sin `SPRING_DATASOURCE_URL`, prod falla (bien), pero con un mensaje poco claro: `'url' must start with "jdbc"`, porque el placeholder llega literal a Hikari. **Propuesta opcional:** mencionarlo en `.env.example`.

- **Derivación (2026-11-20):** `SPRING_MAIL_HOST` por variable de entorno va en el PR chico de O1 (coder). El mensaje poco claro de `SPRING_DATASOURCE_URL` se acepta.

### O7 (para la spec 004): el email del usuario sale en los logs

Al registrarse, `MailService` (generado) loguea `WARN ... Email could not be sent to user 'otro@example.com'`. La regla 6 de `AGENTS.md` no nombra los emails, pero son datos personales. **Propuesta:** que el documentador decida si la spec 004 los suma a la lista de datos que no van a los logs.

- **Derivación (2026-11-20):** el email se sumó a "dato sensible" en la spec 004 (punto 3, con referencia a `MailService`). Lo arregla el agente observabilidad.

## 3. Revisión del diff (`86053eb..d29d803`)

| Archivo | Territorio | Observación |
|---|---|---|
| `.env.example` (nuevo) | Compartido | Solo placeholders y comandos para generar valores. Bien explicado lo del `$` del hash en compose. El texto sobre la rotación es incorrecto (O1) |
| `.gitleaksignore` (nuevo) | (del agente ci) | Ver V12. Las entradas por número de línea son frágiles: si una línea se mueve, Gitleaks vuelve a marcarla. Falla cerrado, así que es aceptable, y el archivo lo advierte |
| `application-prod.yml` | Compartido | Sin secretos. Usa `${VAR:${samples.x}}` para el orden de perfiles del CI; las claves `samples.*` solo existen en `application-secret-samples.yml` |
| `application-secret-samples.yml` | Compartido | Agrega `samples.*`. El hash de muestra es el mismo de `user.csv` (`admin`), es público y no se aplica |
| `20261120000000_prod_usuarios_por_defecto.xml` (nuevo) | Compartido | Contexto `prod and !secret-samples`, precondición `HALT` con regex BCrypt. Inyectar `${adminPasswordHash}` en el `sqlCheck` es aceptable: lo controla el operador, y un valor con `'` también falla cerrado. El comentario sobre la rotación es incorrecto (O1) |
| `master.xml` | Compartido | `include` entre needles, sin borrar ni editar ninguno |
| `docs/catedra/12-factors.md`, `seguridad.md`, `specs/004/spec.md` | Docs | Coherentes con la implementación, salvo O3 y O5 |
| `docs/specs/000-base/plan.md`, `tasks.md` | Desarrollador | Ver O1 y O4 |

- **Secretos reales en el diff:** ninguno.
- **Logs con datos sensibles agregados por el diff:** ninguno. O7 viene del código generado.
- **Código del generador editado a mano:** ninguno.
- **Dependencias nuevas:** ninguna.

## 4. Evidencia

**CI:**
- Run [44774586527](https://github.com/tinchosalvatore/final_ing_soft_apl/actions/runs/44774586527) (`20fae19`): verde. Gitleaks, Semgrep, Trivy, backend, frontend y E2E.
- Run 44776039938 (`8b6520a`, cabeza del PR; solo cambia `tasks.md`): verde.
- Run 44777351993 (`d29d803`, push a `main`): verde.

**Corridas propias:**
- Jar armado en el worktree `verify/000-base` con `./mvnw -ntp -Pprod -DskipTests -Dskip.npm -Dskip.installnodenpm package` (solo backend: alcanza para la API).
- Postgres `postgres:18.6` aislado en el puerto 55432, recreado entre escenarios.
- Contraseñas de prueba descartables, con sus hashes generados por `htpasswd -bnBC 10`.
- Secreto JWT generado con `openssl rand -base64 64`.

| Corrida | Perfiles / variables | Arranca | Resultado |
|---|---|---|---|
| A2 | `prod` + base + hash, **sin secreto JWT** | no | `Could not resolve placeholder 'jhipster.security.authentication.jwt.base64-secret'` |
| B | `prod` + secreto + hash, **sin `SPRING_DATASOURCE_URL`** | no | `'url' must start with "jdbc"` (O6) |
| C | `prod` real, todas las variables, hash H1, base vacía | sí | changeset en `databasechangelog` (`md5 9:f849…`). `user.activated=f`. `admin/admin` 401, `admin/<H1>` 200, `user/user` 500, `user/wrongpass` 500, `noexiste` 401. Registro anónimo de `user` → 201 y la cuenta original desaparece (O2). Hash en el log: 0 |
| D | igual que C, sobre la misma base, con **hash H2** | **no** | `ValidationFailedException ... check sum was 9:f849… but is now 9:9aa6…` (O1) |
| E | igual que C, sobre la misma base, con el **mismo hash H1** | sí | — |
| F | `e2e,secret-samples,prod` (orden del CI), base nueva | sí | changeset 0 filas; `admin/admin` 200, `user/user` 200 |
| G | `prod` real sobre la base de F (demo → prod) | sí | El changeset corre en ese momento: `user.activated=f`, `admin/admin` 401, `admin/<H1>` 200 |

Los contenedores y el jar de la verificación se borraron al terminar. No se modificó ningún archivo fuera de este `verify.md`.
