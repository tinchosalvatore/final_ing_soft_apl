# ADR-0003: Playwright como único framework E2E, grabado con codegen

- Estado: Aceptado
- Fecha: 2026-11-20

## Contexto
- El enunciado pide tests E2E con **Playwright**.
- JHipster 9.4 soporta Playwright de forma nativa (`testFrameworks [playwright]`): genera `playwright.config.ts`, helpers de soporte y un E2E de CRUD por entidad en `src/test/javascript/playwright/e2e/` con extensión `.pw.ts`.
- La cátedra pidió grabar los flujos con **codegen**, para que queden el código y una grabación reproducible.

## Decisión
- Playwright es el único framework E2E: no se genera Cypress.
- Los E2E de negocio (los criterios de aceptación de cada spec) se **graban con `npx playwright codegen`**, se guardan como `*.pw.ts` en `src/test/javascript/playwright/e2e/negocio/` y después se ajustan: locators por rol o texto y una aserción por cada criterio.
- La "grabación reproducible" es el **trace** de Playwright (`trace: 'on'` para los tests de negocio), que se ve con `npx playwright show-trace`. En CI se guarda el reporte HTML como artefacto.

## Consecuencias
- Los E2E de CRUD que genera JHipster quedan como regresión gratis.
- Los tests grabados se acoplan a la UI: si cambia un texto o un rol, se regraban.
- El nombre `.pw.ts` es obligatorio: si no, Vitest intenta correrlos como tests unitarios.

## Alternativas
- **Cypress (el default histórico de JHipster):** no es lo que pide el enunciado. Descartado.
- **Escribir los E2E a mano sin codegen:** no deja la grabación que pidió la cátedra. Descartado como método principal.
