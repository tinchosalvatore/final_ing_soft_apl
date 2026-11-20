# ADR-0005: PWA con Workbox parametrizado y gastos pendientes en IndexedDB

- Estado: Aceptado (revisado el 2026-11-20, antes de implementar: se pasó de `injectManifest` a `generateSW` parametrizado)
- Fecha: 2026-11-20

## Contexto
- La cátedra pidió desarrollar la PWA con **Workbox, parametrizándolo por configuración**, y explícitamente **no** con un service worker escrito a mano (en JS puro o equivalente).
- El frontend React de JHipster 9.4 compila con Vite y **no genera** service worker ni manifest listos para usar.
- La spec 003 pide consultar gastos sin conexión y **cargarlos** sin conexión para que se sincronicen al volver la red.
- La sesión es un JWT que vence: un gasto encolado offline puede intentar enviarse cuando el token ya no sirve.

## Decisión
- **Service worker generado por Workbox** (`vite-plugin-pwa` en modo **`generateSW`**). No hay ningún archivo de service worker escrito a mano.
- **Toda la parametrización vive en un archivo propio y legible**, `src/main/webapp/app/pwa/pwa.config.ts`, que exporta:
  - el **manifest** (nombre, íconos, colores, `display`);
  - la configuración de **precache** del app shell (`globPatterns`, `navigateFallback`);
  - las **estrategias de caché en runtime** (`runtimeCaching`), una entrada por tipo de recurso, con su `handler` (`NetworkFirst`, `NetworkOnly`, etc.), `cacheName`, `expiration` y `cacheableResponse`.

  `vite.config.ts` solo importa ese objeto y se lo pasa al plugin.
- **Estrategias:**
  - App shell (HTML, JS, CSS, íconos): precache.
  - `GET /api/gastos*`, `/api/categorias*` y `/api/resumen*`: **NetworkFirst**, caché `api-v1`, solo respuestas 200 y hasta 50 entradas.
  - `GET /api/account`: **NetworkFirst** (decisión D1 del agente pwa: sin esto la app no abre sin red con la sesión iniciada). Mitigación: si la app abre sin token, borra `api-v1`, para que una `/api/account` guardada no haga pasar por logueado a nadie.
  - El resto de `/api/**` (incluida la autenticación): **NetworkOnly**.
- **Base offline parametrizada** en `src/main/webapp/app/pwa/offline-db.config.ts`: nombre y versión de la base IndexedDB, *object stores* e índices (store `pendientes`, índice por `login`).
- **Gastos cargados offline:** los guarda **la app** en IndexedDB como pendientes y los envía al volver la conexión con el token vigente.
- **Al cerrar sesión**, la app borra la caché `api-v1` (Cache Storage es accesible desde la página, no hace falta código en el service worker) y los pendientes del usuario.

## Consecuencias
- Workbox se usa como lo pidió la cátedra: **declarativo**. Para defender cada decisión de caché alcanza con leer `pwa.config.ts`.
- No hay código de service worker para mantener; actualizar Workbox es actualizar el plugin.
- Como la sincronización la controla la app, un pendiente nunca se pierde por un token vencido. Contra: solo sincroniza con la app abierta.
- `NetworkFirst` sobre la API es un riesgo de privacidad en dispositivos compartidos, por eso es obligatorio limpiar al cerrar sesión.

- **Limitación aceptada:** con "Recordarme" y el JWT vencido, sin red se ven la cuenta y los datos guardados. Se considera que "Recordarme" significa un dispositivo de confianza (L1 en `seguridad.md`).

## Alternativas
- **Service worker escrito a mano (JS puro o `injectManifest` con un `sw.ts` propio):** es lo que la cátedra pidió no hacer. Descartado.
- **`workbox-background-sync` (cola en el service worker):** reenvía la petición original con el token viejo. Si el token venció, el servidor responde 401, Workbox lo da por enviado y el gasto se pierde. Descartado.
- **`workbox-recipes`:** esconden las estrategias. Se prefiere `runtimeCaching` explícito.
- **El service worker de Angular (`ngsw-config.json`):** ver ADR-0001.
