# Tareas — Spec 003 (PWA y carga sin conexión)

## Fase roja (agente testing, 2026-11-20)

| # | Tarea | Criterios | Estado |
|---|---|---|---|
| T1 | E2E escritos a mano con el contrato §5.1 (PR #16) en `e2e/negocio/003-pwa-offline.pw.ts` | 003-CA01 a CA11 | escritos; compilan y pasan lint |
| T2 | **Correrlos en rojo contra el jar de prod** y anotar la salida | 003-CA01 a CA11 | **pendiente** |
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
