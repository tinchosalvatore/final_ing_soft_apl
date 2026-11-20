# Guía: PWA con Workbox

Responsable: agente **pwa** (spec 003). El agente **testing** graba los E2E offline.

## 1. Qué se aplica
| Concepto | Se aplica como |
|---|---|
| Manifest | Nombre "Tracker de Gastos", `short_name` "Gastos", `display: standalone`, `start_url: /`, colores de tema e íconos de 192 y 512 px (incluido uno `maskable`) |
| Service worker con **Workbox, parametrizado** (lo pidió la cátedra; **nada de service worker escrito a mano**) | `vite-plugin-pwa` en modo **`generateSW`**. Toda la configuración en **`src/main/webapp/app/pwa/pwa.config.ts`** (ADR-0005) |
| Precaching / app shell | `workbox.globPatterns` (HTML, JS, CSS, íconos) en `pwa.config.ts` |
| Estrategias de caché | `workbox.runtimeCaching` en `pwa.config.ts`, una entrada por tipo de recurso: **NetworkFirst** para `GET /api/gastos*`, `/api/categorias*` y `/api/resumen*` (`cacheName: 'api-v1'`, `cacheableResponse: { statuses: [200] }`, `expiration: { maxEntries: 50 }`). **NetworkOnly** para el resto de `/api/**` (incluido `/api/authenticate`). Lo que no sea `GET` no se cachea nunca |
| Navegación offline | `workbox.navigateFallback: 'index.html'` (SPA), excluyendo `/api/**` con `navigateFallbackDenylist` |
| Limpieza de versiones viejas | `workbox.cleanupOutdatedCaches: true` |
| IndexedDB | Gastos **pendientes**, gestionados por la app con la librería `idb`. Esquema parametrizado en **`src/main/webapp/app/pwa/offline-db.config.ts`** (nombre y versión de la base, store `pendientes` con clave autoincremental, índice por `login`) |
| Sincronización | La app escucha `online` y el arranque con sesión válida; envía los pendientes en orden con el token actual (ADR-0005) |
| Mobile first | Revisar las pantallas de las specs 001 a 003 a 360 px de ancho |
| Application panel de DevTools | Para depurar: estado del SW, Cache Storage, IndexedDB, simular offline |

## 2. Cómo lo tiene que hacer el agente
0. **Configuración en archivos propios, no "un import y listo".** El humano quiere ver y defender cada parámetro. Por eso:
   - `app/pwa/pwa.config.ts`: manifest + precache + `runtimeCaching`. Cada entrada de `runtimeCaching` lleva un comentario de una línea con **por qué esa estrategia** para ese recurso.
   - `app/pwa/offline-db.config.ts`: esquema de IndexedDB.
   - `vite.config.ts` solo hace `VitePWA(pwaConfig)`.
   - Armar `pwa.config.ts` **de a un bloque** (manifest → precache → una entrada de `runtimeCaching` por vez → base offline), explicando cada parámetro y esperando el OK del humano antes de pasar al siguiente.
   - Prohibido escribir un service worker a mano (`injectManifest`, `sw.js` o `sw.ts` propios) y usar `workbox-recipes`.
1. **Territorio**: `app/pwa/**`, `app/shared/offline/**` y los componentes nuevos son del desarrollador. `vite.config.ts` e `index.html` son compartidos: respetar los needles y no tocar más que el plugin y el registro del SW.
2. **Nombres de caché** con prefijo y versión: `api-v1`. Si cambia el formato de los datos, se sube la versión.
3. **Separación por usuario**: la caché de la API se borra completa al cerrar sesión (la app hace `caches.delete('api-v1')` desde la página: Cache Storage es accesible sin pasar por el SW). Los pendientes en IndexedDB se guardan con el `login` y solo se muestran o envían los del usuario actual. Al cerrar sesión se borran (spec 003, punto 14).
4. **Detección de "sin conexión"**: el aviso de datos guardados se muestra cuando la respuesta vino de la caché porque falló la red, no solo por `navigator.onLine` (que miente cuando hay WiFi sin internet).
5. **Validación offline**: reutilizar las mismas reglas del formulario de la spec 001. No duplicar reglas a mano en otro lugar.
6. **Rechazos**: si al sincronizar el servidor responde 400, el pendiente pasa a "rechazado" con el mensaje del servidor. Si responde 401, se deja pendiente y se reintenta después del login. Si hay error de red, se deja pendiente.
7. **Registro del SW** solo en el build de producción (en `npm start` molesta con la caché). Los E2E de PWA corren contra el build de producción servido por Spring Boot (`./mvnw -Pprod` o el jar), no contra el servidor de Vite.
8. **Almacenamiento finito**: la caché de API tiene `expiration: { maxEntries: 50 }`. No cachear nada que no sea `GET` ni respuestas que no sean 200.

## 3. Cómo se verifica
| Qué | Cómo |
|---|---|
| Instalable, SW activo | E2E (003-CA01): `navigator.serviceWorker.ready` resuelve; el manifest se sirve y es válido. Manual: Lighthouse → PWA |
| Offline | E2E con `context.setOffline(true)` de Playwright (003-CA02 a CA08) |
| Limpieza al cerrar sesión | E2E que inspecciona `caches.keys()` e IndexedDB después del logout (003-CA09, CA10) |
| Mobile | E2E con viewport 360×740 (003-CA11) |

## 4. Qué queda afuera y por qué
| Concepto | Motivo |
|---|---|
| `workbox-background-sync` | Pierde gastos si el JWT venció (ADR-0005) |
| `workbox-recipes`, service worker a mano | Las estrategias se declaran una por una en `runtimeCaching` para que se vean y se puedan defender; el SW lo genera Workbox |
| StaleWhileRevalidate, CacheFirst para la API | Los gastos tienen que estar actualizados cuando hay red: NetworkFirst es la correcta. CacheFirst solo aplica al app shell (vía precache) |
| Push notifications, permisos de cámara | No hay caso de uso |
| `navigator.storage.estimate()` | Con el límite de entradas alcanza para este volumen |
| SEO | La app es privada (requiere login) |

## 5. Referencias
Spec 003, ADR-0005, `seguridad.md` (S9), `testing.md`.
