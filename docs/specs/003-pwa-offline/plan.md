# Plan — Spec 003 (PWA y carga sin conexión)

- Autor: agente pwa (sesión `pwa`, 2026-11-20)
- Estado: Implementado (2026-11-20). El documentador pidió seguir sin esperar su OK (modo ahorro); el humano aprobó los bloques 1 (manifest) y 2 (precache) de `pwa.config.ts` y después pidió terminar el resto sin preguntar

<!-- Lo escribe el implementador ANTES de codear. No se codea hasta que se apruebe. -->

## 1. Resumen de la solución

El service worker lo **genera Workbox** (`vite-plugin-pwa` en modo `generateSW`). Toda la configuración (manifest, precache, `runtimeCaching`) vive en `app/pwa/pwa.config.ts`, que se arma **de a un bloque con el humano**; `vite.config.ts` solo hace `VitePWA(pwaConfig)`. No hay ningún `sw.js`/`sw.ts` propio.

La carga sin conexión la maneja **la app**, no el SW: si el `POST /api/gastos` falla por red, el gasto se guarda como pendiente en IndexedDB (`tracker-offline` v1, store `pendientes`, índice `login`, esquema en `app/pwa/offline-db.config.ts`, librería `idb`). Un módulo de sincronización envía los pendientes en orden al volver la red o al iniciar con sesión, con una instancia de axios **propia** para que un 401 no pase por el interceptor de JHipster (spec §5.1, punto 12).

El cierre de sesión pasa por un componente propio que avisa si hay pendientes, borra `api-v1` y los pendientes, y recién después ejecuta el logout de JHipster.

## 2. Archivos que se tocan

| Archivo | Territorio | Qué cambia |
|---|---|---|
| `app/pwa/pwa.config.ts` (nuevo) | Desarrollador | Manifest, precache (`globPatterns`, `navigateFallback`, `navigateFallbackDenylist: [/^\/api\//, /^\/management\//, ...]`, `cleanupOutdatedCaches`), `runtimeCaching`, `registerType: 'prompt'` sin UI (punto 16: el SW nuevo se activa al volver a abrir), `injectRegister: 'script'` |
| `app/pwa/offline-db.config.ts` (nuevo) | Desarrollador | Nombre, versión, store `pendientes` (`keyPath` autoincremental) e índice `login` |
| `app/shared/offline/pendientes.repositorio.ts` (nuevo) | Desarrollador | CRUD de pendientes con `idb`, siempre filtrando por `login` |
| `app/shared/offline/sincronizacion.ts` (nuevo) | Desarrollador | Envío en orden; mapeo de respuestas (ver §3); exclusión mutua para no enviar dos veces |
| `app/shared/offline/conexion.ts` (nuevo, incluye el hook `useEnLinea`) | Desarrollador | Estado "sin conexión" (ver §5, decisión D2) |
| `app/shared/offline/limpieza.ts` (nuevo) | Desarrollador | `caches.delete('api-v1')` + borrar pendientes |
| `app/shared/offline/cerrar-sesion.tsx` (nuevo) | Desarrollador | Aviso singular/plural con "Cerrar sesión igual" / "Cancelar"; al confirmar limpia y renderiza el `Logout` generado |
| `app/shared/offline/indicador-pendientes.tsx`, `use-pendientes.ts`, `eventos.ts`, `sincronizacion-offline.tsx`, `textos.ts` (nuevos) | Desarrollador | Textos de la spec §5.1 en un solo lugar (como ADR-0008) |
| `content/images/pwa/icono.svg`, `icono-maskable.svg` + `icono-192.png`, `icono-512.png`, `icono-maskable-512.png` (nuevos) | Desarrollador | Ícono propio; los PNG se generan una vez con ImageMagick y se commitean. `vite.config.ts` los copia a la **raíz** del build (`/icono-*.png`), que Spring sirve sin sesión (`/content/**` pide sesión) |
| `app/modules/gastos/mis-gastos.tsx`, `gasto-form.tsx`, `gastos.api.ts`, `index.tsx` | Desarrollador (spec 001) | Listado: pendientes arriba con su marca, rechazados con "Reintentar"/"Descartar", Editar/Borrar deshabilitados sin conexión, aviso de datos guardados / "no disponible". Formulario: si el POST falla por red → pendiente; modo "Corregir gasto pendiente" (ruta `/mis-gastos/pendiente/:clave/corregir`). Las validaciones siguen siendo `validarGasto` (punto 7, sin duplicar reglas) |
| `app/modules/resumen/resumen.tsx` | Desarrollador (spec 002) | Aviso de datos guardados / "no disponible" |
| `app/routes.tsx` | Compartido (ADR-0008) | La ruta `logout` usa `CerrarSesion`; ruta de corrección de pendiente |
| `app/shared/layout/header/header.tsx` | Compartido (ADR-0008) | Indicador "N pendientes" |
| `app/app.tsx` | Generador | Arranque de la sincronización (`online` + sesión iniciada). Va a la tabla de ADR-0008 (`n` y reaplicar) |
| `vite.config.ts` | Compartido | `VitePWA({ ...pwaConfig, disable: development })` (D5) y la copia de los íconos. Sin tocar el needle |
| `index.html` | Compartido | Saca `<link rel="manifest" href="/manifest.webapp">` (lo inyecta el plugin), título "Tracker de Gastos", `theme-color` |
| `manifest.webapp` | Generador | Se borra (lo reemplaza `/manifest.webmanifest`). Tabla de ADR-0008 |
| `config/SecurityConfiguration.java` | Generador | `/*.webmanifest` en `permitAll` (hoy devolvería 401). Tabla de ADR-0008 |
| `config/application.yml` | Compartido | `server.mime-mappings.webmanifest: application/manifest+json` (Tomcat lo servía como `application/octet-stream`) |
| `package.json` | Compartido | Dependencias (§4); `webapp:build:dev` con `--mode development` (D5) |
| Tests Vitest propios (`*.spec.ts` en `app/shared/offline/` y `app/pwa/`) | Desarrollador | Repositorio con `fake-indexeddb`, sincronización con axios mockeado, config de Workbox (estrategias y nombres) |
| `docs/specs/003-pwa-offline/tasks.md`, `docs/adr/ADR-0008...md` | Docs | Tareas y tabla de archivos generados modificados |

## 3. Dónde vive cada regla de negocio

| Regla de la spec | Dónde |
|---|---|
| 1, 003-CA01: instalable | `pwa.config.ts` → `manifest` |
| 2, CA02: abre sin conexión | `pwa.config.ts` → precache + `navigateFallback: 'index.html'` |
| 4, 6, CA03: datos guardados, siempre del servidor con red | `pwa.config.ts` → `NetworkFirst` en `api-v1` para `GET /api/gastos*`, `/api/categorias*`, `/api/resumen*` (200, `maxEntries: 50`); aviso con `use-conexion` |
| 5: nunca visto | Sin entrada en caché → el fetch falla → la pantalla muestra "Esta información no está disponible sin conexión" |
| 7, CA08: validación offline | `validarGasto` (spec 001), antes de guardar el pendiente |
| 8, CA04: pendiente e indicador | `gasto-form.tsx` (error de red sin respuesta → `pendientes.repositorio.agregar`), `indicador-pendientes.tsx` |
| 9, 10, CA05: sincronizar en orden | `sincronizacion.ts`: orden por clave autoincremental; 2xx → se borra; uno rechazado no frena a los siguientes |
| 11, CA06: rechazo | 400 → `estado: 'rechazado'`, motivo = `fieldErrors[0].message`, o `detail`/`title` del *problem* |
| 12, CA07: sesión vencida | 401 → se corta la vuelta y todo queda pendiente; instancia de axios propia (no dispara `clearAuthentication`). Error de red o 5xx → queda pendiente y se reintenta después |
| 13: sin editar/borrar offline | `mis-gastos.tsx` con `use-conexion` |
| 14, CA09: logout | `cerrar-sesion.tsx` + `limpieza.ts` |
| 15, CA10: aislamiento | Pendientes filtrados por `login`; `api-v1` se borra al cerrar sesión **y en cada login nuevo** (D4) |
| 16: actualizaciones | `registerType: 'prompt'` sin UI: Workbox no hace `skipWaiting`, el SW nuevo se activa al cerrar y volver a abrir |
| CA11: 360 px | Revisión de "Mis gastos", formulario y resumen; ajustes de CSS si hace falta |

## 4. Tecnologías y dependencias nuevas

| Dependencia | Versión | Tipo | Por qué |
|---|---|---|---|
| `vite-plugin-pwa` | 2.0.0 | dev | Genera el SW con Workbox (`generateSW`) y el manifest. Soporta Vite 8 (peer `^8.0.0`). Trae `workbox-build` y `workbox-window` 7.4.x |
| `idb` | 8.0.4 | runtime | Envoltorio chico de IndexedDB con promesas (lo nombra ADR-0005) |
| `fake-indexeddb` | (fija al instalar) | dev | IndexedDB en Vitest (jsdom no lo tiene), para los tests propios |

## 5. Decisiones tomadas por el agente (para revisar)

- **D1. `GET /api/account` también va a `api-v1` con NetworkFirst.** Sin esto, al abrir sin red `getSession()` falla, la app cree que no hay sesión y las rutas privadas redirigen al login: CA03 no se puede cumplir. Es la misma estrategia y se borra con la misma limpieza. Se aparta de la guía (que dice NetworkOnly para el resto de `/api/**`); por eso lo pregunto. Alternativa: guardar el último `account` en `localStorage` (más código propio, otra cosa que limpiar).
- **D2. Detección de "sin conexión".** Con NetworkFirst la página recibe un 200 igual venga de la red o de la caché, y en `generateSW` marcarlo exige un plugin de Workbox con código (es escribir SW a mano). Propuesta: cuando una pantalla carga datos cacheables, si `navigator.onLine` es `false` → sin conexión; si es `true`, una sonda `HEAD /management/health` (`cache: 'no-store'`, no la intercepta el SW): cualquier respuesta HTTP = con conexión, error de red = sin conexión. Cubre el caso "WiFi sin internet" de la guía §2.4 con un pedido extra por pantalla.
- **D3. Logout sin tocar `logout.tsx` generado**: la ruta `logout` pasa a `CerrarSesion`, que envuelve al generado.
- **D4. `api-v1` se borra también en cada login** (no solo en el logout). Si la sesión de `ana` vence sin logout y entra `beto`, la caché de `ana` no se le muestra (punto 15).
- **D5. SW solo en prod**: hoy `webapp:build:dev` y `webapp:build:prod` son el mismo `vite build`. `webapp:build:dev` pasa a `vite build --mode development` y el plugin queda `disable` fuera de `production`. `npm start` no registra SW (default del plugin).
- **D6. Ícono propio** (SVG simple, generado con ImageMagick), no los de JHipster.

- **D7. Sincronización ligada al dueño del token**: antes de enviar se compara el `sub` del JWT con el `login` de los pendientes; si es de otro usuario no se envía nada (hallazgo de la revisión de seguridad automática). Un token ilegible se deja pasar: el backend responde 401 y el pendiente queda (punto 12).

## 6. Decisiones pendientes (para el documentador / humano)

Resueltas por el agente al implementar (el humano pidió terminar sin preguntar); el documentador puede revisarlas:
1. **D1 y D2** se implementaron como se proponen arriba. D1 está explicada en el comentario de la entrada de `/api/account` en `pwa.config.ts`.
2. **Logout con pendientes de otro usuario.** Se borran solo los pendientes del usuario que cierra sesión (los que cuenta el aviso) y la caché `api-v1` completa. Los de otro usuario cuya sesión venció siguen esperando su login (punto 12) y nunca se le muestran a nadie más (punto 15). CA09 se cumple igual. Si la spec quiere borrar "todos" literalmente, es cambiar `borrarPendientesDe(login)` por un `clear()` del store.
3. **Respuestas que no son 400 ni 401** al sincronizar (403, 404, 5xx): quedan pendientes y se reintentan después.

## 7. Riesgos

- `vite-plugin-pwa` con Rolldown (Vite 8) es reciente: si el build falla se fija otra versión 2.x y se anota.
- Cambios en `mis-gastos.tsx` y `gasto-form.tsx` (spec 001): conflicto posible si el coder los toca. Hoy `fix/001-patch` no toca el front.
- `NetworkFirst` cachea con clave = URL, sin el usuario: la privacidad depende de la limpieza en logout y login (D4) y del aislamiento por `login` en IndexedDB.
- Los E2E necesitan el jar de prod: más lentos; no se corren en paralelo con otro agente (regla 13).
