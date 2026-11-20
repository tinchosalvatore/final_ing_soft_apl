# ADR-0006: Pipeline de CI basado en el de la cátedra, no en `jhipster ci-cd`

- Estado: Aceptado
- Fecha: 2026-11-20

## Contexto
- `jhipster ci-cd` genera un workflow que solo compila y testea.
- La cátedra compartió un pipeline de referencia (GitHub Actions, disparado en `pull_request`) con tests, SCA, SAST (Semgrep) y detección de secretos (Gitleaks), con acciones fijadas a un SHA y permisos mínimos. Ese pipeline es para un proyecto Node.
- Este proyecto es Java (Maven) + TypeScript (npm) y suma tests de integración con Testcontainers y E2E con Playwright.

## Decisión
Se adapta el pipeline de la cátedra (`.github/workflows/pipeline.yml`) manteniendo sus principios:
- Se dispara en `pull_request` (y en `push` a `main`, para que `main` también tenga su estado).
- Permisos mínimos y **toda acción de terceros fijada a un SHA de commit**.
- **Primero lo barato y rápido** (fallar rápido): Gitleaks → Semgrep → Trivy (SCA) → tests de backend → tests de frontend → E2E.
- SCA: se reemplaza `npm audit` por **Trivy en modo `fs`**, que analiza a la vez `pom.xml` y `package-lock.json`. Falla con vulnerabilidades HIGH o CRITICAL que tengan parche disponible.
- SAST: Semgrep con las reglas `p/java`, `p/typescript` y `p/react`.
- E2E: se levanta la app con Postgres y se corre Playwright. El reporte y los traces se guardan como artefacto.

## Consecuencias
- El umbral de merge queda escrito en la spec 005: todos los pasos en verde.
- El pipeline tarda más que el de referencia (Maven + Testcontainers + E2E), pero corre en un solo job, como el de la cátedra.
- Trivy, sin `ignore-unfixed`, haría fallar el pipeline por vulnerabilidades que todavía no tienen parche. Con `ignore-unfixed` solo falla por las que se pueden resolver.

## Alternativas
- **`jhipster ci-cd`:** no trae ninguna etapa de seguridad. Descartado (queda como referencia para los comandos de build).
- **Varios jobs en paralelo:** más rápido, pero más complejo de leer y de defender. Descartado por ahora.
- **OWASP Dependency-Check para Maven:** lento (descarga la base del NVD). Descartado frente a Trivy.
