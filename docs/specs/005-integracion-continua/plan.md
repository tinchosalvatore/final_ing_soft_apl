# Plan — Spec 005

- Autor: agente ci (sesión del 2026-11-20)
- Estado: Aprobado (2026-11-20, por el documentador, por delegación del humano)

<!-- Lo escribe el implementador ANTES de codear. No se codea hasta que el humano lo apruebe. -->

## 1. Resumen de la solución
Se parte del `pipeline.yml` actual (un solo job, `verificar`) y se mantienen el orden y los principios del ADR-0006. Los cambios son tres: se actualizan las acciones que corren con Node 20, se fijan las versiones de Semgrep y de Trivy, y **se arregla el paso de SCA, que hoy no analiza las dependencias de Maven** (ver §5.1). Después se prueban los rojos (CA02 a CA06) con PRs descartables `ci-prueba/*` y se dejan en `verify.md` los pasos (y el comando `gh api`) para que el humano active la protección de `main` (CA07 y CA08).

## 2. Archivos que se tocan
| Archivo | Territorio | Qué cambia |
|---|---|---|
| `.github/workflows/pipeline.yml` | dev | SHAs nuevos (checkout, gitleaks); versión fija de Semgrep y de Trivy; el setup de Java/Node y la resolución de dependencias Maven pasan a estar **antes** del paso de Trivy; se sacan los `TODO(agente CI)` |
| `.gitleaksignore` | dev | Solo si se mueve alguna línea. Hoy los 5 fingerprints coinciden con `main` |
| `.trivyignore` | dev | Solo si aparece un falso positivo (con CVE y motivo). Hoy no hace falta |
| `docs/specs/005-integracion-continua/{plan,tasks,verify}.md` | dev | Plan, tareas y evidencia |
| `pom.xml` | compartido | **Autorizado por el documentador, solo para esto:** override de `<tomcat.version>` y de la versión del BOM de Jackson a las versiones corregidas, cada uno con un comentario con los CVE y "quitar cuando Spring Boot/JHipster traiga una versión ≥ X". Se valida con `./mvnw -ntp verify` completo |

## 3. Dónde vive cada regla de la spec
| Regla de la spec | Dónde |
|---|---|
| §3.1 Corre en cada PR a `main`, en sus actualizaciones y al integrar | `on: pull_request` (opened/synchronize/reopened, por defecto) + `on: push` a `main` |
| §3.2.1 Secretos, primero | Paso `Secretos (Gitleaks)`, `fetch-depth: 0` para revisar todo el historial del PR |
| §3.2.2 SAST | Paso `SAST (Semgrep)`, `p/java p/typescript p/react`, `--error` |
| §3.2.3 SCA, solo HIGH/CRITICAL con fix | Paso `SCA (Trivy)`, `severity: HIGH,CRITICAL`, `ignore-unfixed: true`, `exit-code: 1` |
| §3.2.4 a 3.2.6 Tests back, front, E2E | Scripts `ci:*` de JHipster, en ese orden |
| §3.3 Si falla, se detiene e indica cuál | Pasos secuenciales en un job: el primero que falla corta los siguientes y queda marcado en rojo en la UI |
| §3.4 Reporte E2E descargable | `upload-artifact` de `target/playwright/` con `if: !cancelled()`. Ya funciona: en el PR #1 subió el reporte HTML (286 KB). `outputDir` = `target/playwright/results` (traces y capturas de los que fallan), reporte = `target/playwright/report` (solo con `CI=true`, que GitHub define) |
| §3.5 Umbral de merge | Protección de `main` que configura el humano (pasos en `verify.md`) |
| §3.6 Permisos mínimos, versiones inmutables | `permissions:` a nivel workflow; todas las `uses:` con SHA + versión en comentario; Semgrep y Trivy con versión exacta |
| §4 Falso positivo | `// nosemgrep: <regla>`, `.trivyignore`, `.gitleaksignore`, siempre con comentario |
| §4 Vulnerabilidad sin fix → no bloquea pero se ve | `ignore-unfixed: true` en el paso que corta; el paso de cobertura (§5.1) las lista en el log sin fallar |
| §4 El SCA no logra analizar una parte → falla | Paso `SCA - cobertura` (§5.1): falla si `pom.xml` o `package-lock.json` no aparecen, o si alguna dependencia de Maven queda sin versión |
| §4 Vulnerabilidad en dependencia de desarrollo → no bloquea | Comportamiento por defecto de Trivy: no analiza `devDependencies` de npm ni dependencias Maven de scope `test` |
| §4 E2E inestable → hasta 2 reintentos | Ya está: `retries: 2` en `playwright.config.ts` (generado, no se toca) |

## 4. Tecnologías y dependencias nuevas
No se agregan herramientas: se actualizan y se fijan las que ya están. SHAs verificados con `git ls-remote --tags` el 2026-11-20.

| Herramienta | Antes | Después | Por qué |
|---|---|---|---|
| `actions/checkout` | `11d5960…` (Node 20) | v7.0.1 `3d3c42e5aac5ba805825da76410c181273ba90b1` | Node 20 deprecado. v7 no cambia inputs (bloquea checkout de forks en `pull_request_target`, que no usamos) |
| `gitleaks/gitleaks-action` | v2.3.9 `ff98106…` | v3.0.0 `e0c47f4f8be36e29cdc102c57e68cb5cbf0e8d1e` | Node 20 → 24. Release notes: "No changes to inputs, outputs, or behavior". El repo es personal, no hace falta `GITLEAKS_LICENSE` |
| Semgrep | `semgrep==1.*` | `semgrep==1.186.0` | Factor II: versión exacta (resuelve el TODO) |
| Trivy (binario) | implícito (default de la action v0.36.0 = v0.70.0) | `version: v0.70.0` explícito | Que la versión se lea en el yml y no dependa del default |
| `trivy-action`, `setup-java`, `setup-node`, `upload-artifact` | v0.36.0, v6.3.0, v7.1.0, v7.0.2 | sin cambios | Ya son las últimas y no generan el aviso de Node 20 |

## 5. Decisiones tomadas por el agente

### 5.1 Hallazgo: Trivy no está analizando las dependencias de Maven
Corrí Trivy v0.70.0 local (Docker) contra `main`:
- **Tal como corre hoy** (igual que en CI): 0 vulnerabilidades. Pero el log del CI del PR #1 dice `WARN [pom] Dependency version cannot be determined. Child dependencies will not be found.` Trivy intenta bajar el `spring-boot-starter-parent` de Maven Central para saber las versiones, no lo consigue (localmente vi `429 Too Many Requests`) y sigue de largo **sin fallar**. Resultado: de `pom.xml` solo ve 28 dependencias directas, sin versión, y no puede compararlas con ninguna base.
- **Con el repositorio Maven local montado** (`~/.m2`, `--offline-scan`): ve 146 paquetes, todos con versión, y encuentra **4 CRITICAL** (`tomcat-embed-core` 11.0.24, fix en 11.0.25) y **10 HIGH** (`jackson-core`/`jackson-databind` 2.21.5 y 3.1.5, fix en 2.21.7 y 3.1.7), todas con versión corregida.

O sea: el paso de SCA está en verde porque no mira, no porque no haya nada. **Decisión:** que el pipeline resuelva las dependencias de Maven **antes** de Trivy (`./mvnw -ntp dependency:go-offline`, con la caché de `setup-java`) y que Trivy corra con `--offline-scan` para leerlas de `~/.m2` sin depender de Maven Central. Esto mueve `setup-java`, `setup-node` y `npm ci` antes del paso 3. **No cambia el orden de las verificaciones** (secretos → SAST → SCA → tests), solo mueve la preparación del entorno, que no verifica nada. El costo es ~30 s a 1 min más antes de llegar a Trivy.

**Guarda de cobertura (caso borde nuevo de la spec: si el SCA no logra analizar, falla).** `--offline-scan` evita el 429, pero si falta un pom en `~/.m2` Trivy vuelve a seguir de largo con un `WARN`. Por eso, inmediatamente después de `SCA (Trivy)` va un paso más, **`SCA - cobertura`**, que reusa el binario y la base que ya bajó la action (`--skip-db-update`) y genera el JSON con `--list-all-pkgs`. Con `jq`:
- **falla** si no hay resultado para `pom.xml` o para `package-lock.json`, o si alguna dependencia de `pom.xml` no tiene versión;
- **imprime sin fallar** las HIGH/CRITICAL que no tienen versión corregida (spec §4: "no bloquea, pero aparece en el log").

Es parte de la verificación 3 (SCA), no un paso nuevo fuera de la spec. Antes de abrir el PR lo pruebo en local en las dos situaciones: con `~/.m2` (pasa) y sin `~/.m2` (falla).

Consecuencia: el pipeline encuentra vulnerabilidades reales que hoy no se ven. Para que `main` no quede en rojo, este mismo PR sube Tomcat y Jackson (§6.2, resuelta).

### 5.2 Las 45 vulnerabilidades de `npm install` (pendiente 3 del PR #1)
La hipótesis del pedido era que Trivy no las cortaba por `ignore-unfixed`. **No es así.** Con `npm audit --package-lock-only`:
- Todas las dependencias: 45 (1 low, 36 moderate, 4 high, 4 critical).
- Solo producción (`--omit=dev`): **0**.

Las 45 están en `devDependencies` (`vite`, `concurrently`, `happy-dom`, `generator-jhipster`, `simple-git`, `shell-quote`…), y **Trivy, por defecto, no analiza las dependencias de desarrollo de npm** (hace falta `--include-dev-deps`). Por eso pasa. Se deja explicado en `verify.md` para la defensa. Si se quieren incluir, ver §6.3.

### 5.3 Reporte E2E (pendiente 1 del PR #1)
No hacía falta corregirlo: en el run del PR #1 (`44776039938`) el paso subió el artefacto `reporte-e2e` (1 archivo, el HTML). Sube un solo archivo porque todos los E2E pasaron y `target/playwright/results` solo guarda traces y capturas de los que fallan. Se deja el `path: target/playwright/` (cubre reporte y resultados). La "grabación" de un E2E que falla es el **trace** (`retain-on-failure`, se abre con `npx playwright show-trace`), que incluye capturas paso a paso. El video está apagado en `playwright.config.ts` (es del generador y no lo toco); si se quiere video, se pasa `PLAYWRIGHT_VIDEO=true` en el `env` del paso (decisión pendiente §6.4).

### 5.4 `.gitleaksignore` (pendiente 4)
Verificado: las 5 líneas (`archivo:regla:línea`) coinciden hoy con `main`. Se revisa de nuevo antes de abrir el PR.

### 5.5 PRs de prueba en rojo
Uno por criterio, desde ramas `ci-prueba/caNN-*` creadas **desde esta rama** (para que corran con el pipeline nuevo), con base `main`, y se cierran sin mergear:

| Criterio | Cambio del PR descartable | Rojo esperado en |
|---|---|---|
| CA02 | Romper un assert de un test que ya está en `main` (por ejemplo un test de `service/` generado), porque los de la 002 todavía no están mergeados | Tests de backend; E2E no corre |
| CA03 | Archivo con una clave privada RSA generada para la ocasión y descartada | Gitleaks (primer paso) |
| CA04 | Un método en un archivo nuevo que arma una consulta SQL concatenando un parámetro (`"... where x = '" + param + "'"`) | Semgrep. Antes de abrir el PR lo pruebo local con Semgrep 1.186.0 para confirmar qué regla de `p/java` lo detecta |
| CA05 | Fijar en `pom.xml` una dependencia con CVE crítica conocida y fix (por ejemplo `log4j-core` 2.14.1, Log4Shell) | Trivy |
| CA06 | Cambiar un texto de la UI que usa un E2E generado | E2E, con el artefacto descargable con el trace |

Como CA03 a CA05 cortan antes de los tests, son PRs rápidos. Los links quedan en `verify.md`.

> Ojo con el orden: CA02, CA04 y CA06 tienen que pasar Trivy para llegar a su paso. Si cuando los corra el pipeline sigue rojo por Tomcat/Jackson (§6.2), los hago desde una rama que ya tenga las versiones corregidas.

### 5.6 Revisión de seguridad del propio workflow
- Ningún `run:` usa valores controlados por quien abre el PR (título, rama, cuerpo). Se mantiene.
- Se usa `pull_request` (no `pull_request_target`): los PRs de forks no reciben secretos ni token con escritura.
- `pull-requests: write` queda solo porque Gitleaks comenta en el PR. Es el único permiso de escritura.

## 6. Decisiones pendientes (para el humano)

### 6.1 Protección de `main` — **Resuelta (2026-11-20): el repo es público**
El humano hizo público el repo, y ahora la API responde `Branch not protected` en lugar del 403. La regla **la aplica el humano** (`agentes-sdd.md` §4: la configuración de GitHub es solo del humano). En `verify.md` dejo los pasos de la UI y este comando, listo para correr:

```bash
gh api -X PUT repos/tinchosalvatore/final_ing_soft_apl/branches/main/protection --input - <<'JSON'
{
  "required_status_checks": { "strict": false, "contexts": ["verificar"] },
  "enforce_admins": true,
  "required_pull_request_reviews": { "required_approving_review_count": 0 },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON
```
- `contexts: ["verificar"]`: el check obligatorio es el job `verificar`.
- `enforce_admins: true`: "Do not allow bypassing". Sin esto, el dueño del repo puede hacer push directo y CA07 no se cumple.
- `required_approving_review_count: 0`: se exige PR, pero no aprobaciones (desarrollador solo; la revisión queda como comentario, spec §3.5).
- `strict: false`: no obliga a actualizar la rama con `main` antes de mergear. Con varios agentes en paralelo, `true` obligaría a re-correr el pipeline (~8 min) cada vez que otro mergea.

**Cuándo activarla:** cuando se mergee este PR (para que el check `verificar` sea el del pipeline nuevo). Antes de activarla, aviso al humano: desde ese momento ningún PR con el pipeline en rojo o sin terminar se puede mergear, incluidos los de testing y seguridad. Después pruebo CA07 (push directo rechazado) y CA08 (PR en rojo con el botón de merge bloqueado) y dejo la evidencia en `verify.md`.

### 6.2 Tomcat y Jackson en `pom.xml` — **Resuelta por el documentador (2026-11-20)**
Van en este PR: override por propiedad a las versiones corregidas, con comentario con los CVE y la condición para quitarlo. `./mvnw -ntp verify` completo tiene que quedar verde. El documentador avisa a testing y coder que integren `main` después del merge.

### 6.3 Dependencias de desarrollo — **Resuelta por el documentador (2026-11-20)**
El SCA cubre solo dependencias de producción. Quedó escrito en la spec 005 (definición de "dependencia de producción" y caso borde) y en el ADR-0006. No se agrega `--include-dev-deps`.

### 6.4 Video en los E2E — **Resuelta (2026-11-20): opción a**
Solo trace y capturas de los tests que fallan. El trace es la "grabación reproducible" (ADR-0003); el video no suma.

## 7. Riesgos
- **Las ramas de los demás agentes** se van a poner rojas en SCA hasta que integren `main` (traen el `pom.xml` viejo solo si lo tocan; si no, alcanza con hacer merge de `main`). Lo coordina el documentador.
- **Overrides de versión en `pom.xml`**: se desfasan del BOM de Spring Boot. Por eso llevan la condición de remoción en el comentario.
- **Las reglas de Semgrep (`p/java`, …) se bajan del registro en cada corrida** y no están versionadas. Un PR puede ponerse rojo por una regla nueva sin haber cambiado nada. Se acepta (fijar reglas implicaría copiarlas al repo); si pasa, se resuelve con `nosemgrep` comentado.
- **La base de vulnerabilidades de Trivy se actualiza todos los días.** Igual que arriba: un PR puede ponerse rojo por un CVE nuevo. Es lo que se busca.
- `dependency:go-offline` puede no bajar algún pom de plugins o de dependencias de test que Trivy no necesita; si Trivy vuelve a avisar `version cannot be determined`, se ajusta la resolución. En `verify.md` dejo cómo comprobar que Trivy ve las 146 dependencias.
- **`strict: false` en la protección de `main`** (aprobado por el documentador). Con `strict: true`, GitHub obliga a actualizar la rama con `main` y a correr de nuevo el pipeline (~7-8 min) cada vez que otro agente mergea, y con varios agentes en paralelo eso es una cola constante. El costo de `false`: un PR puede quedar verde contra una versión vieja de `main` y romper algo al integrarse con otro cambio (dos PRs verdes por separado pero rotos juntos). Lo mitiga el pipeline de `push` a `main`, que valida el resultado integrado: si se rompe, `main` queda en rojo a la vista y se corrige con un PR.
