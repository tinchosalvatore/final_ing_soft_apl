# Guía: PWA con Workbox

Responsable: implementador de la spec 003. El tester graba los E2E offline.

## 1. Qué se aplica
| Concepto | Se aplica como |
|---|---|
| Manifest | Nombre "Tracker de Gastos", `short_name` "Gastos", `display: standalone`, `start_url: /`, colores de tema e íconos de 192 y 512 px (incluido uno `maskable`) |
| Service worker con **Workbox** (no JS puro, lo pidió la cátedra) | `vite-plugin-pwa` en modo **`injectManifest`**, con un SW propio en `src/main/webapp/sw.ts` (ADR-0005) |
| Precaching / app shell | `precacheAndRoute(self.__WB_MANIFEST)` para HTML, JS, CSS e íconos |
| Estrategias de caché | **NetworkFirst** para `GET /api/gastos*`, `/api/categorias*` y `/api/resumen*`, con `CacheableResponsePlugin({ statuses: [200] })`. **NetworkOnly** para el resto de `/api/**` (incluido `/api/authenticate`) y para todo lo que no sea `GET` |
| Navegación offline | `NavigationRoute` que sirve el `index.html` precacheado (SPA) |
| Limpieza de versiones viejas | `cleanupOutdatedCaches()` de Workbox (evento `activate`) |
| IndexedDB | Gastos **pendientes**, gestionados por la app (no por el SW), con la librería `idb`. Un *object store* `pendientes` con clave autoincremental y un índice por `login` del usuario |
| Sincronización | La app escucha `online` y el arranque con sesión válida; envía los pendientes en orden con el token actual (ADR-0005) |
| Mobile first | Revisar las pantallas de las specs 001 a 003 a 360 px de ancho |
| Application panel de DevTools | Para depurar: estado del SW, Cache Storage, IndexedDB, simular offline |

## 2. Cómo lo tiene que hacer el agente
1. **Territorio**: `sw.ts`, `app/shared/offline/**` y los componentes nuevos son del desarrollador. `vite.config.ts` e `index.html` son compartidos: respetar los needles y no tocar más que el plugin y el registro del SW.
2. **Nombres de caché** con prefijo y versión: `api-v1`. Si cambia el formato de los datos, se sube la versión.
3. **Separación por usuario**: la caché de la API se borra completa al cerrar sesión (el SW expone un mensaje `LOGOUT` que hace `caches.delete('api-v1')`). Los pendientes en IndexedDB se guardan con el `login` y solo se muestran o envían los del usuario actual. Al cerrar sesión se borran (spec 003, punto 14).
4. **Detección de "sin conexión"**: el aviso de datos guardados se muestra cuando la respuesta vino de la caché porque falló la red, no solo por `navigator.onLine` (que miente cuando hay WiFi sin internet).
5. **Validación offline**: reutilizar las mismas reglas del formulario de la spec 001. No duplicar reglas a mano en otro lugar.
6. **Rechazos**: si al sincronizar el servidor responde 400, el pendiente pasa a "rechazado" con el mensaje del servidor. Si responde 401, se deja pendiente y se reintenta después del login. Si hay error de red, se deja pendiente.
7. **Registro del SW** solo en el build de producción (en `npm start` molesta con la caché). Los E2E de PWA corren contra el build de producción servido por Spring Boot (`./mvnw -Pprod` o el jar), no contra el servidor de Vite.
8. **Almacenamiento finito**: la caché de API tiene `ExpirationPlugin({ maxEntries: 50 })`. No cachear nada que no sea `GET` ni respuestas que no sean 200.

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
| `workbox-recipes` | Las estrategias se declaran una por una para que se vean en el código y se puedan defender |
| StaleWhileRevalidate, CacheFirst para la API | Los gastos tienen que estar actualizados cuando hay red: NetworkFirst es la correcta. CacheFirst solo aplica al app shell (vía precache) |
| Push notifications, permisos de cámara | No hay caso de uso |
| `navigator.storage.estimate()` | Con el límite de entradas alcanza para este volumen |
| SEO | La app es privada (requiere login) |

## 5. Referencias
Spec 003, ADR-0005, `seguridad.md` (S9), `testing.md`.
