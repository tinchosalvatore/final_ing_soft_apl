# Verificación — Spec 005

- Verificador: **agente ci** (evidencia de los criterios). La verificación formal (S1 a S10, 12 factors) la hace el agente seguridad.
- Rama / PR: `spec/005-integracion-continua`, [PR #4](https://github.com/tinchosalvatore/final_ing_soft_apl/pull/4) (mergeado)
- Resultado: **Cumple con observaciones** (CA08: falta el intento de merge manual, ver §1)

<!-- No alcanza con que compile. Se contrasta contra la spec: ¿cada criterio tiene un test que lo representa de verdad? -->

## 1. Criterios de aceptación

Repo: `https://github.com/tinchosalvatore/final_ing_soft_apl`. Los PRs #7 a #11 son **pruebas descartables** que se abrieron para ver el rojo y se cierran sin mergear. Cada uno tiene un solo commit sobre `main`.

| ID | Cómo se probó | ¿Representa el criterio? | Resultado |
|---|---|---|---|
| 005-CA01 | [PR #4](https://github.com/tinchosalvatore/final_ing_soft_apl/pull/4), [run 44781643316](https://github.com/tinchosalvatore/final_ing_soft_apl/actions/runs/44781643316): los 6 pasos en verde (Gitleaks, Semgrep, Trivy + cobertura, tests de backend, tests de frontend, E2E). Después del merge, el run de `push` a `main` ([44782562576](https://github.com/tinchosalvatore/final_ing_soft_apl/actions/runs/44782562576)) también quedó verde | sí | ✅ |
| 005-CA02 | [PR #7](https://github.com/tinchosalvatore/final_ing_soft_apl/pull/7), [run 44782644672](https://github.com/tinchosalvatore/final_ing_soft_apl/actions/runs/44782644672): un assert roto en `UserMapperTest.testUserToUserDTO` (`expected: "login-que-no-es"`). Rojo en **Tests de backend**; *Tests de frontend* y todos los pasos *E2E* quedan `skipped` | parcial: la spec dice "un test de la spec 002", pero esos tests todavía no estaban en `main`. Se usó un test que ya existía (lo pidió el documentador). El mecanismo es el mismo | ✅ |
| 005-CA03 | [PR #8](https://github.com/tinchosalvatore/final_ing_soft_apl/pull/8), [run 44782656705](https://github.com/tinchosalvatore/final_ing_soft_apl/actions/runs/44782656705): `src/main/resources/clave-prueba-ca03.pem`. Gitleaks: `RuleID: private-key`, `leaks found: 1`. Rojo en **Secretos (Gitleaks)**, el primer paso; los demás `skipped` | sí | ✅ |
| 005-CA04 | [PR #9](https://github.com/tinchosalvatore/final_ing_soft_apl/pull/9), [run 44782670033](https://github.com/tinchosalvatore/final_ing_soft_apl/actions/runs/44782670033): `BusquedaInseguraService` con `st.executeQuery("... = '" + descripcion + "'")`. Semgrep: `java.lang.security.audit.formatted-sql-string`, *Blocking*. Rojo en **SAST (Semgrep)** | sí | ✅ |
| 005-CA05 | [PR #10](https://github.com/tinchosalvatore/final_ing_soft_apl/pull/10), [run 44782682201](https://github.com/tinchosalvatore/final_ing_soft_apl/actions/runs/44782682201): `log4j-core` 2.14.1 en `pom.xml`. Trivy: `CVE-2021-44228 CRITICAL fixed 2.14.1 → 2.15.0…`, `Total: 3 (HIGH: 1, CRITICAL: 2)`. Rojo en **SCA (Trivy)** | sí | ✅ |
| 005-CA06 | [PR #11](https://github.com/tinchosalvatore/final_ing_soft_apl/pull/11), [run 44782696807](https://github.com/tinchosalvatore/final_ing_soft_apl/actions/runs/44782696807): en `i18n/es/gasto.json` se renombra la clave `home.title`, así que la UI muestra la clave cruda en vez de "Gastos". Rojo en **E2E - Playwright**: `gasto.pw.ts › should have translated page title` falla 3 veces (intento + `retry #1` + `retry #2`). Artefacto `reporte-e2e` (7,4 MB) descargable desde el run, con `report/index.html` y, en cada intento, `results/…/trace.zip` + `test-failed-1.png` | sí. Los E2E generados por JHipster usan `data-cy` y no textos; este es el único que verifica un texto visible. Los E2E por rol/texto que grabe el agente testing lo van a cubrir mejor | ✅ |
| 005-CA07 | Con la protección activa: commit vacío sobre `origin/main` y `git push origin HEAD:main`. GitHub responde: `GH006: Protected branch update failed for refs/heads/main. - Changes must be made through a pull request. - Required status check "verificar" is expected.` y `! [remote rejected] HEAD -> main (protected branch hook declined)`. Con `enforce_admins: true` vale también para el dueño del repo | sí | ✅ |
| 005-CA08 | Con la protección activa, `gh pr view N --json mergeStateStatus` da **`BLOCKED`** para los 5 PRs en rojo (#7 a #11). **Falta el intento de merge**: el agente no tiene permiso para correr `gh pr merge` sobre un PR en rojo. Lo hace el humano sobre el #11 (si se llegara a mergear, es un cambio inofensivo): `gh pr merge 11 --merge` tiene que fallar, o en la UI el botón tiene que figurar deshabilitado con "Merging is blocked". Pegar acá la salida o la captura | parcial (falta el intento) | ⏳ |

## 2. Casos borde no contemplados

- **El SCA pasaba en verde sin analizar el backend.** Trivy no resolvía las versiones que `pom.xml` hereda de Spring Boot (429 de Maven Central) y seguía de largo con un `WARN`. Ya se agregó a la spec 005 §4 ("si no logra analizar, falla") y se implementó con el paso `SCA - cobertura` (ver §4.1).
- **Las 45 vulnerabilidades de `npm install`** son todas de `devDependencies` (ver §4.2). Ya se agregó a la spec 005 §2 la definición de "dependencia de producción".
- Sin otros hallazgos.

## 3. Revisión del diff (PR #4)

- **Archivos fuera del plan:** ninguno. `pom.xml` (territorio compartido) estaba previsto en `plan.md` §2, con autorización del documentador: solo `<tomcat.version>`, `<jackson-bom.version>` y `<jackson-2-bom.version>`, cada uno con los CVE y la condición para quitarlo. No se tocaron needles.
- **Territorio del generador:** no se tocó.
- **Secretos:** ninguno. `.gitleaksignore` sin cambios (las 5 líneas siguen apuntando al secreto JWT de muestra de JHipster).
- **Workflow:**
  - ningún `run:` usa valores controlados por quien abre el PR;
  - se dispara con `pull_request`, no con `pull_request_target`;
  - el único permiso de escritura es `pull-requests: write` (para Gitleaks);
  - todas las `uses:` están fijadas a un SHA con la versión en un comentario;
  - Semgrep (`1.186.0`) y Trivy (`v0.70.0`) tienen versión exacta.
- **Logs:** el pipeline no imprime datos de la app.

## 4. Evidencia

### 4.1 SCA: por qué antes no analizaba Maven y cómo se comprobó que ahora sí

| Situación | `pom.xml` | Resultado |
|---|---|---|
| Pipeline viejo (run del PR #1) | 28 dependencias **sin versión** (`WARN [pom] Dependency version cannot be determined`) | verde, pero sin analizar el backend |
| Local, Maven resuelto, antes de los overrides | 146 dependencias | 4 CRITICAL (`tomcat-embed-core` 11.0.24) y 10 HIGH (`jackson-*` 2.21.5 / 3.1.5), todas con fix |
| Local, con los overrides de `pom.xml` | 146 | 0 vulnerabilidades |
| Guarda `SCA - cobertura` con `~/.m2` vacío + solo `dependency:go-offline` | 146 | exit 0 |
| Guarda `SCA - cobertura` con `~/.m2` vacío, sin `go-offline` | 28 sin versión | `::error::Trivy no pudo resolver la versión…` → **exit 1** |
| CI del PR #4 | `pom.xml: 146 dependencias analizadas`, `package-lock.json: 106 dependencias analizadas` | verde |

`./mvnw -ntp verify` con los overrides: BUILD SUCCESS (58 tests unitarios y 170 de integración, 0 fallos).

**Cómo comprobar en cualquier run que Trivy analizó todo:** en el log del paso `SCA - cobertura` tienen que aparecer las dos líneas `… dependencias analizadas`, con más de 100 en cada una. Si Trivy vuelve a no resolver Maven, ese paso falla.

### 4.2 Por qué Trivy pasa aunque `npm install` reporte 45 vulnerabilidades (pregunta de defensa)

```text
npm audit --package-lock-only            → 45 (1 low, 36 moderate, 4 high, 4 critical)
npm audit --package-lock-only --omit=dev → 0
```

Las 45 están en `devDependencies` (`vite`, `concurrently`, `happy-dom`, `generator-jhipster` y transitivas como `simple-git` y `shell-quote`). Son herramientas de build y test que **no viajan en la app** (el jar lleva el bundle del front ya compilado). Trivy, en modo `fs`, por defecto **no analiza las devDependencies de npm ni el scope `test` de Maven**, y eso es justo lo que pide la spec 005 §2 ("dependencia de producción"). No tiene que ver con `ignore-unfixed`: Trivy local, sin `ignore-unfixed` y con todas las severidades, da 0 vulnerabilidades en `package-lock.json` (106 paquetes de producción).

Qué hace `ignore-unfixed: true`: una HIGH/CRITICAL **sin** versión corregida no corta el pipeline (no hay cómo arreglarla), pero el paso `SCA - cobertura` la lista en el log (spec 005 §4).

### 4.3 Protección de `main`

La activó el humano (a través del documentador) el 2026-11-20, con el comando de `plan.md` §6.1. Estado según `GET /repos/.../branches/main/protection`:

```json
{"checks":["verificar"],"strict":false,"enforce_admins":true,"reviews":0,"force":false,"del":false}
```

Equivalente en la UI: Settings → Branches → regla para `main`:
- Require a pull request before merging, con 0 aprobaciones requeridas (desarrollador solo: la revisión humana queda como comentario en el PR, spec §3.5);
- Require status checks to pass, con el check `verificar`;
- Do not allow bypassing the above settings;
- sin force push ni borrado.

`strict: false` (no exige que la rama esté al día con `main`): ver el trade-off en `plan.md` §7.

### 4.4 Sobre el PR de CA03 en un repo público
La clave de `clave-prueba-ca03.pem` es una clave RSA **de prueba**, generada con `openssl genrsa` para este criterio y nunca usada en ningún sistema. Como el repo es público, el diff del PR #8 queda visible en GitHub aunque el PR se cierre y la rama se borre. No es un riesgo: no protege nada.
