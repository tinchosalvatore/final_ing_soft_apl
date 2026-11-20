# Tareas — Spec 003 (PWA y carga sin conexión)

## Fase roja (agente testing, 2026-11-20)

| # | Tarea | Criterios | Estado |
|---|---|---|---|
| T1 | E2E escritos a mano con el contrato §5.1 (PR #16) en `e2e/negocio/003-pwa-offline.pw.ts` | 003-CA01 a CA11 | escritos; compilan y pasan lint |
| T2 | **Correrlos en rojo contra el jar de prod** y anotar la salida | 003-CA01 a CA11 | hecho por el agente pwa (ver abajo) |
| T3 | Regrabar con codegen (graba el humano, pule el agente testing) | todos | pendiente |

### Cómo correrlos (el service worker solo existe en el build de prod)
```
npm run ci:e2e:package && npm run ci:e2e:prepare
npm run ci:e2e:server:start          # en otra terminal: jar e2e con perfiles e2e,secret-samples,prod
npx playwright test --project=chromium e2e/negocio/003-pwa-offline.pw.ts
```
No correrlos en paralelo con otra app ni con la grabación con codegen (regla 13).

### Supuestos (confirmados por el documentador, spec 003 §5.1)
- CA06: "Reintentar" abre el formulario "Corregir gasto pendiente"; del motivo solo se afirma `Rechazado: <texto>`.
- CA07: la sesión vencida se simula reemplazando `jhi-authenticationToken` por un token inválido; un 401 al sincronizar no borra los pendientes.
- CA09: singular "Tenés 1 gasto sin sincronizar. Si cerrás sesión se va a perder."
- CA10: `beto` inicia sesión con red y después se corta.
- CA11 puede pasar ya en rojo (depende del layout de la 001/002, no de la PWA): sería regresión.

Helpers: `e2e/negocio/helpers/offline.ts` (IndexedDB `tracker-offline`/`pendientes` sin crearla, caché `api-v1`, API del admin fuera del contexto offline).

### Evidencia de la fase roja (agente pwa, 2026-11-20)
Corrida contra `target/e2e.jar` (perfiles `e2e,secret-samples,prod`) de `spec/003-pwa-offline` con `main` y `test/003-pwa-offline` integrados, **antes** de implementar la PWA:
```
npx playwright test --project=chromium e2e/negocio/003-pwa-offline.pw.ts --output=target/playwright/pwa-rojo
  14 failed
  1 passed (20.9m)
```
| Test | Falla en | Motivo |
|---|---|---|
| 003-CA01 | `expect(link[rel="manifest"]).toHaveAttribute('href', '/manifest.webmanifest')` | No hay manifest de la PWA (el de JHipster es `/manifest.webapp`) |
| 003-CA02 a CA10 (13 tests) | `esperarServiceWorker` → `page.evaluate: Execution context was destroyed` | No hay service worker: `navigator.serviceWorker.ready` nunca resuelve y el `evaluate` queda colgado hasta que la página navega |
| 003-CA11 | — (pasa) | Esperado: depende del layout de la 001/002, no de la PWA. Queda como test de regresión |

Todos fallan por la ausencia de la PWA (motivo de la spec), no por errores de los tests: no hizo falta corregir ninguno.
