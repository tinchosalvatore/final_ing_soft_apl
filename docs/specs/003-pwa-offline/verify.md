# Verificación — Spec 003 (PWA y carga sin conexión)

- Verificador: agente seguridad (no participó en la implementación ni en los tests de la 003)
- Rama / PR: [PR #19](https://github.com/tinchosalvatore/final_ing_soft_apl/pull/19), verificado sobre `9e91f2f`, mergeado en `main` como `ed9a9d4`. Este verify entra por `verify/003-004`
- Fecha: 2026-11-20
- Resultado: **Cumple con observaciones**

<!-- No alcanza con que compile. Se contrasta contra la spec: ¿cada criterio tiene un test que lo representa de verdad? -->

**Cómo se verificó.**
- No levanté la app ni corrí E2E: el agente testing está usando el puerto 8080 con el humano (regla 13).
- Revisé el código como atacante.
- Corrí `npm run ci:backend:test` y `npm run ci:frontend:test` en local.
- Para los E2E uso el CI del PR (run 44806920498, en verde).

**Resumen.**
- Los 11 criterios tienen un test que los representa.
- **S9:** la caché `api-v1` (que también guarda `/api/account`) se borra al cerrar sesión, al abrir la app sin token y cuando inicia sesión otro usuario.
- Los pendientes se guardan por login, y nunca se envían con el token de otro usuario.
- Un 401 al sincronizar no pasa por el interceptor que cierra la sesión, así que no se pierden datos.
- Hay dos observaciones bajas. O1: un riesgo residual de la decisión D1 (cachear `/api/account`). O2: `alIniciarSesion` sin test unitario. Hay una de proceso (O3): el implementador ajustó dos E2E del agente testing, de forma documentada y sin relajarlos.

## 1. Criterios de aceptación

E2E en `src/test/javascript/playwright/e2e/negocio/003-pwa-offline.pw.ts`, escritos en rojo por el agente testing (`a10483a`). Unitarios propios del agente pwa en `app/pwa/` y `app/shared/offline/`.

| ID | Test que lo cubre | ¿El test representa el criterio? | Resultado |
|---|---|---|---|
| 003-CA01 | E2E `003-CA01 instalable…`; unit `pwa.config.spec.ts` `003-CA01: genera el SW con Workbox…` | sí: `link rel=manifest`, `name`, `short_name`, `display`, `start_url`, íconos 192 y 512 y maskable, SW `activated` con scope `/` | ✅ |
| 003-CA02 | E2E `003-CA02 abre sin conexión…`; unit `003-CA02: la navegación offline cae en index.html, salvo la API` | sí: recarga sin red, sin `chrome-error` y con el menú visible | ✅ |
| 003-CA03 | E2E `003-CA03 consulta sin conexión…` y `…borde: una pantalla nunca vista…`; unit `003-CA03: NetworkFirst en api-v1…` | sí: con red no aparece el aviso (punto 6). Sin red, los mismos datos y el aviso. Una pantalla nunca vista muestra el texto del punto 5 | ✅ |
| 003-CA04 | E2E `003-CA04 carga sin conexión…` y `…borde: sin conexión no se puede editar ni borrar…`; unit `pendientes.repositorio` `003-CA04…` | sí: la marca, el indicador "1 pendiente" y IndexedDB con 1. Editar y Borrar deshabilitados con el texto del contrato | ✅ |
| 003-CA05 | E2E `003-CA05 sincronización…`; unit `sincronizacion` `003-CA05…` y `pendientes.repositorio` `003-CA05: lista en el orden…` | sí: los 2 llegan, desaparecen las marcas y el indicador, y quedan 0 en IndexedDB. El orden lo prueba el unitario | ✅ |
| 003-CA06 | E2E `003-CA06 rechazo…` (Reintentar) y `…se puede descartar`; unit `003-CA06: un 400 deja el pendiente "rechazado"… y sigue con los siguientes` | sí: el ADMIN borra la categoría por API y aparecen `Rechazado: <motivo>`, "Corregir gasto pendiente" con los datos y Descartar sin rastro en el servidor. El borde "el 2.º se rechaza y el 3.º igual se acepta" lo cubre el unitario | ✅ |
| 003-CA07 | E2E `003-CA07 sesión vencida…`; unit `003-CA07: un 401 corta la sincronización… (sin pasar por el interceptor de logout)` | sí: con un token inválido el pendiente no llega (la API responde 401) y sigue en IndexedDB. Después del login se sincroniza | ✅ |
| 003-CA08 | E2E `003-CA08 validación offline…` | sí: los mensajes de la 001 y 0 pendientes | ✅ |
| 003-CA09 | E2E `003-CA09 logout limpia datos…` y `…"Cancelar" no cierra la sesión ni borra nada`; unit `003-CA09…` | sí: hay un control previo (la caché tenía datos), aparece el aviso con N = 1 y N = 2, y al final la caché y los pendientes quedan en 0 | ✅ |
| 003-CA10 | E2E `003-CA10 aislamiento en el dispositivo…`; unit `limpieza.spec` `003-CA10: sin token al abrir la app, borra la caché`, `pendientes.repositorio` `003-CA10: nunca devuelve los pendientes de otro usuario`, `sincronizacion` `003-CA10: no envía los pendientes de un usuario con el token de otro` | sí para el escenario de la spec (`ana` cierra sesión). El caso "la sesión de `ana` venció sin cerrar sesión y entra `beto`" depende de `alIniciarSesion`, que no tiene test (O2) | ✅ con O2 |
| 003-CA11 | E2E `003-CA11 mobile…` (360×740) | sí: "Mis gastos", el formulario y el resumen sin scroll horizontal | ✅ |

## 2. Revisión como atacante (S9 y sincronización)

| Punto | Código | Resultado |
|---|---|---|
| ¿Qué se cachea? | `pwa.config.ts`: NetworkFirst para GET `/api/(gastos\|categorias\|resumen)` y `/api/account` en `api-v1`, solo 200 y como máximo 50 entradas. El resto de `/api/` es NetworkOnly (login, admin, cambio de contraseña) | ✅ No se cachea ningún 401, 404 ni 500, ni ninguna escritura |
| La clave de la caché es la URL (no varía por `Authorization`) | Con red, NetworkFirst siempre pisa la entrada con la respuesta del usuario actual. Sin red, la respuesta guardada sería de otro usuario si nadie la borró. Por eso importa la limpieza ↓ | ✅ con la limpieza |
| Limpieza al cerrar sesión | `/logout` → `CerrarSesion` → `limpiarAlCerrarSesion`: borra `api-v1`, los pendientes del login y `ULTIMO_LOGIN`. Funciona sin red (Cache Storage e IndexedDB son locales) | ✅ |
| Limpieza sin token | `app.tsx`: `borrarCacheSiNoHaySesion()` **antes** de `getSession()`. Así se evita que, sin token, la cuenta guardada reabra la sesión anterior | ✅ |
| Limpieza cuando entra otro usuario | `SincronizacionOffline` → `alIniciarSesion(login)`: si el login es distinto de `ULTIMO_LOGIN`, borra `api-v1` y vuelve a pedir `/api/account` | ✅ (sin test, O2) |
| Pendientes por usuario | IndexedDB `pendientes` con índice `login`. `usePendientes` y `listarPendientes` filtran por el login con sesión. Al cerrar sesión solo se borran los del usuario que la cierra. Los de otro, cuya sesión venció, esperan a su dueño (decisión aceptada) | ✅ |
| Token de otro usuario | `ejecutar(login)`: si el `sub` del token no coincide con `login`, no envía nada. Un token ilegible se deja pasar: no puede ser válido para otro usuario, el backend responde 401 y el pendiente queda | ✅ |
| 401 sin logout | `clienteSincronizacion` es una instancia de axios propia, sin el interceptor de JHipster. Además, el 401 del interceptor global (`clearAuthentication`) solo borra el token, no los pendientes | ✅ |
| Dependencias nuevas | `idb` 8.0.4, `vite-plugin-pwa` 2.0.0 y `fake-indexeddb` 6.2.8 (dev), con versión fija y justificadas en el plan. El CI corre Trivy y quedó verde | ✅ |
| Backend | Solo `permitAll` para `/*.webmanifest` y el mime `application/manifest+json`. No expone nada más | ✅ |

## 3. Casos borde no contemplados

### O1 (baja, riesgo residual de D1): con "Recordarme", sin red se ven los datos aunque el JWT esté vencido

Con D1, `/api/account` se cachea para poder abrir sin red. Si `ana` inició sesión con "Recordarme" (token en `localStorage`) y nunca cerró sesión, cualquiera que abra la app **sin red** en ese dispositivo entra con la cuenta y los datos guardados de `ana`, aunque el JWT ya venció. Sin red el backend no puede rechazarlo, y el front no mira el `exp` del token.

Es lo mismo que deja una sesión abierta en un dispositivo compartido. Con token de sesión (sin "Recordarme"), cerrar el navegador lo resuelve (`borrarCacheSiNoHaySesion`).

**Propuesta:** si el documentador lo considera necesario, que `borrarCacheSiNoHaySesion` también trate como "sin sesión" un token con `exp` vencido (leyendo el claim, como ya hace `loginDelToken` con el `sub`). Si no, dejarlo asentado como riesgo aceptado en ADR-0005.

### O2 (baja): `alIniciarSesion` no tiene test

La única defensa del caso "la sesión de `ana` venció sin cerrar sesión y entra `beto` con red" es `alIniciarSesion`: cambia el login, entonces borra `api-v1`. `limpieza.spec.ts` solo prueba `borrarCacheSiNoHaySesion`, y el E2E de CA10 pasa por el logout de `ana`, que ya limpia todo. **Propuesta:** que el agente pwa sume un unitario: con otro login borra la caché, con el mismo login no la borra.

### O3 (proceso): el implementador ajustó dos E2E del agente testing

`26eeecf` y `9e91f2f` (sesión del agente pwa) modifican `003-pwa-offline.pw.ts`, que escribió en rojo el agente testing (`a10483a`). Los cambios son esperas a carreras reales:
- CA01: `ready` resuelve en `activating`, así que se espera con `expect.poll` hasta `activated`.
- CA09: el logout recarga la página y `evaluate` falla mientras navega, así que se reintenta.

Ninguno relaja una aserción. Siguen exigiendo `activated`, caché en 0 y pendientes en 0, y el `-1` del reintento nunca da 0. Están documentados en `tasks.md` ("el agente testing estaba apagado").

Como verificador los acepto. Igual dejo la observación para la trazabilidad: la regla 2 y §6 de `AGENTS.md` dicen que el implementador no toca los E2E de sus criterios. **Propuesta:** que el agente testing los revise cuando regrabe con codegen.

## 4. Revisión del diff (`ed9a9d4^1..ed9a9d4`)

- **Territorios:** el SW lo genera Workbox (`generateSW`), no hay service worker escrito a mano, y la configuración está en `app/pwa/pwa.config.ts`. El módulo offline está en `app/shared/offline/` (territorio del desarrollador). Los compartidos (`vite.config.ts`, `index.html`, `routes.tsx`, `header.tsx`, `application.yml`, `SecurityConfiguration`) cambian lo mínimo y con comentario. `webapp:build:dev` pasa a `--mode development`, para que el build de dev no registre SW.
- **Secretos:** ninguno.
- **Logs:** el front no tiene `console.log`. En el backend no hay logs nuevos.

## 5. Evidencia

- **CI:** run 44806920498 (`9e91f2f`, PR #19) en **verde**. Pasan Gitleaks, Semgrep, Trivy, backend, frontend y la suite E2E contra el jar de prod. `tasks.md` registra 73 pasan y 1 se omite (`administration › /docs`), y además 3 corridas seguidas de la 003 sola, 15/15.
- **En local** (`verify/003-004` sobre `main` = `ed9a9d4`, después de `npm ci`):
  - `npm run ci:frontend:test`: build de prod y Vitest, 25 archivos, **204/204**.
  - `npm run ci:backend:test`: javadoc, nohttp y `verify`, **BUILD SUCCESS**, 78 unitarios y 241 de integración, 0 fallas.
- No corrí E2E ni levanté la app (regla 13).
