# Guía: integración continua

Responsable: **agente CI** (spec 005). Punto de partida: `.github/workflows/pipeline.yml` (versión inicial ya escrita, que traduce el pipeline de la cátedra).

## 1. Qué se aplica

| Concepto de la cátedra | En el pipeline |
|---|---|
| Disparo por evento (`on: pull_request`) | `pull_request` a `main` + `push` a `main` |
| Permisos mínimos del `GITHUB_TOKEN` | `contents: read`, `pull-requests: write` (Gitleaks) |
| Acciones fijadas a un **SHA** (cadena de suministro) | Todas las `uses:` llevan SHA de commit, con la versión en un comentario |
| Detección de secretos | **Gitleaks** (paso 1) |
| SAST | **Semgrep** con `p/java`, `p/typescript`, `p/react` (paso 2) |
| SCA | **Trivy** `fs` sobre `pom.xml` y `package-lock.json` (paso 3; reemplaza a `npm audit`, ADR-0006) |
| Instalación reproducible | `npm ci`, `./mvnw` (scripts `ci:*` de JHipster) |
| Pirámide de pruebas en orden | Unitarios + integración + ArchUnit → frontend → E2E |
| Fallar rápido | Primero lo barato (secretos, SAST, SCA) y después lo caro (tests, E2E) |
| VM efímera | `runs-on: ubuntu-latest` |
| Revisión humana / rama protegida | Protección de `main` (sección 2) |
| Umbral de merge escrito | Spec 005, punto 5 |

## 2. Cómo lo tiene que hacer el agente
1. **Partir de `pipeline.yml`** y hacerlo andar en un PR real. Resolver los `TODO(agente CI)`:
   - Fijar la versión exacta de Semgrep.
   - Confirmar la carpeta del reporte de Playwright (`playwright.config.ts` generado: `outputDir` y `report`) y ajustar el `path` del artefacto.
2. **No cambiar el orden** de los pasos ni agregar pasos fuera de la spec 005 sin anotarlo en el `plan.md`.
3. **Toda acción nueva** se fija a un SHA (se obtiene con `git ls-remote --tags https://github.com/<owner>/<repo>`) y se deja la versión en un comentario.
4. **Nunca usar en un `run:`** valores controlados por quien abre el PR (título, rama, cuerpo, mensajes de commit). Si hace falta alguno, se pasa por `env:` y se cita.
5. **Falsos positivos**:
   - Semgrep: comentario `// nosemgrep: <id-regla>` en la línea, con la justificación al lado.
   - Trivy: archivo `.trivyignore` con el CVE y un comentario del motivo.
   - Gitleaks: `.gitleaksignore` con el fingerprint. Nunca desactivar la herramienta entera.
6. **Protección de `main`** (la configura el humano, el agente deja los pasos en `verify.md`): Settings → Branches → regla para `main`:
   - "Require a pull request before merging" (sin exigir aprobaciones, porque el desarrollador está solo).
   - "Require status checks to pass": check `verificar`.
   - "Do not allow bypassing the above settings".
7. **Gitleaks** en repos de una **organización** de GitHub requiere `GITLEAKS_LICENSE`. En un repo personal no. Si el repo está en una organización, avisar al humano.
8. **Probar los rojos** (spec 005, CA02 a CA06) con PRs descartables desde ramas `ci-prueba/*`, que se cierran sin integrar. Los secretos de prueba tienen que ser **falsos** (por ejemplo, una clave privada RSA generada para la ocasión y descartada).

## 3. Cómo se verifica
- Un PR real con el pipeline en verde (005-CA01).
- Los links a los PRs de prueba en rojo, uno por criterio, en `verify.md`.
- Captura de la regla de protección de `main`.

## 4. Qué queda afuera y por qué
| Concepto | Motivo |
|---|---|
| Despliegue continuo, estrategias de despliegue (rolling, blue/green) | No hay un entorno de producción real en el alcance |
| Construir y escanear la imagen de contenedor con Trivy | Suma tiempo al pipeline; se puede agregar al final si sobra tiempo (no cambia la spec) |
| DAST (OWASP ZAP) | Requiere la app desplegada en un entorno de prueba |
| IaC (Terraform) y su análisis | No hay infraestructura declarada en la nube |
| Kubernetes | Fuera de la materia |
| Métricas DORA | Se explican conceptualmente en la defensa; no se automatizan |
| Varios jobs en paralelo | Un solo job, como el de la cátedra, es más fácil de leer y defender (ADR-0006) |

## 5. Referencias
Spec 005, ADR-0006, `seguridad.md` §4 (A03, A05, A08).
