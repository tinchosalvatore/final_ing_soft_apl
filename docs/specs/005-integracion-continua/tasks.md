# Tareas — Spec 005

<!-- Lo escribe el implementador a partir del plan aprobado. Es la memoria de qué se hizo y qué no:
     se actualiza al cerrar cada tarea, así una sesión nueva puede retomar sin releer todo. -->

| # | Tarea | Criterios que cubre | Estado | Commit |
|---|---|---|---|---|
| 1 | Plan y ajustes de spec/ADR del documentador | — | hecha | `dbb7c14`, `6f15669` |
| 2 | `pom.xml`: overrides de Tomcat 11.0.25 y Jackson 3.1.7 / 2.21.7 (CVE en comentarios). `./mvnw -ntp verify` verde | CA01 (que el SCA arreglado no deje `main` en rojo) | hecha | `ca5b899` |
| 3 | `pipeline.yml`: checkout v7.0.1, gitleaks v3.0.0 (Node 24), Semgrep 1.186.0, Trivy v0.70.0 explícito, Maven resuelto antes del SCA + `--offline-scan`, paso `SCA - cobertura` | §3.2, §3.6, §4 | hecha | `46f0206` |
| 4 | Push de la rama y PR a `main` con el pipeline en verde | CA01 | hecha (PR #4, mergeado) | `b9c2caa` |
| 5 | PRs descartables `ci-prueba/*` (#7 a #11), todos en rojo en el paso esperado | CA02 a CA06 | hecha; PRs cerrados y ramas borradas | |
| 6 | `verify.md` con links, explicación de Trivy y pasos de la protección de `main` | todos | hecha (rama `verify/005`) | |
| 7 | El humano activa la protección de `main` (después del merge); probar push directo y merge de un PR en rojo | CA07, CA08 | hecha (el intento de merge de CA08 lo corrió el documentador, con OK del humano) | |

## Evidencia local (2026-11-20)

**Trivy v0.70.0 (Docker), antes de los overrides, con `~/.m2` resuelto:** `pom.xml` 146 dependencias; 4 CRITICAL (`tomcat-embed-core` 11.0.24) y 10 HIGH (`jackson-core`/`jackson-databind` 2.21.5 y 3.1.5), todas con fix.

**Después de los overrides:** `pom.xml` 146 dependencias, 0 sin versión, 0 vulnerabilidades; `package-lock.json` 106 dependencias (solo producción), 0 vulnerabilidades.

**`./mvnw -ntp verify` con los overrides:** BUILD SUCCESS. Unitarios: 58 tests, 0 fallos. Integración: 170 tests, 0 fallos.

**Guarda `SCA - cobertura`, probada en las dos direcciones.** Se extrajo el script del yml y se corrió contra la salida de Trivy:

| Caso | Cómo se simuló | Resultado |
|---|---|---|
| Con Maven resuelto (como en CI) | `~/.m2` **vacío** + solo `./mvnw dependency:go-offline -Dmaven.repo.local=…` (confirma que `go-offline` alcanza) | `pom.xml: 146`, `package-lock.json: 106`, sin HIGH/CRITICAL sin fix → **exit 0** |
| Sin Maven resuelto (el bug que había) | `~/.m2` vacío, sin `go-offline` | `pom.xml: 28` y `::error::Trivy no pudo resolver la versión de estas dependencias de Maven` + la lista → **exit 1** |

**`.gitleaksignore`:** las 5 líneas (`archivo:regla:línea`) siguen apuntando al secreto JWT de muestra de JHipster. El cambio de `pom.xml` no las mueve.

## Notas para la próxima sesión
- Worktree: `../final_ing_soft_apl-ci`, rama `spec/005-integracion-continua`.
- Las dudas van al documentador (por delegación del humano). Push y PRs, siempre con confirmación del humano.
- Los PRs `ci-prueba/*` se crean **desde esta rama**, así corren con el pipeline nuevo, y se cierran sin mergear. Para CA04, probar antes con Semgrep 1.186.0 local qué regla de `p/java` detecta la concatenación.
- Protección de `main`: comando en `plan.md` §6.1. Avisar al humano antes de que la active.
