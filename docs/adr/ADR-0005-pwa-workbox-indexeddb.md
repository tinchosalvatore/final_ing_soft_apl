# ADR-0005: PWA con Workbox y gastos pendientes en IndexedDB

- Estado: Aceptado
- Fecha: 2026-11-20

## Contexto
- La cátedra pidió desarrollar la PWA con **Workbox**, no con un service worker en JS puro.
- El frontend React de JHipster 9.4 compila con Vite y **no genera** service worker ni manifest listos para usar: hay que sumarlos.
- La spec 003 pide dos cosas: consultar gastos sin conexión y **cargarlos** sin conexión para que se sincronicen al volver la red.
- La sesión es un JWT que vence. Un gasto encolado offline puede intentar enviarse cuando el token ya no sirve.

## Decisión
- **Service worker con Workbox**, integrado con `vite-plugin-pwa` en modo `injectManifest`. El archivo `src/main/webapp/sw.ts` usa explícitamente `workbox-precaching` (app shell), `workbox-routing` y `workbox-strategies`.
- **Estrategias de caché:**
  - App shell (HTML, JS, CSS, íconos): precache.
  - `GET /api/gastos*`, `GET /api/categorias*` y `GET /api/resumen*`: **NetworkFirst** (dato fresco si hay red y último conocido si no), solo para respuestas 200.
  - El resto de `/api/**` (incluida la autenticación): **NetworkOnly**.
- **Gastos cargados offline:** los guarda **la app** (no el service worker) en **IndexedDB**, como pendientes. Al volver la conexión, o al abrir la app con conexión, la app los envía con el token vigente.
- **Al cerrar sesión** se borran las cachés de la API y los pendientes de IndexedDB.

## Consecuencias
- Workbox se ve explícito en el código: cada estrategia está nombrada en `sw.ts`.
- Como la app controla la sincronización, un gasto pendiente nunca se pierde por un token vencido: espera a que haya una sesión válida.
- La sincronización ocurre solo con la app abierta (no hay sincronización en segundo plano con la app cerrada).
- `NetworkFirst` sobre la API es un riesgo de privacidad en dispositivos compartidos. Por eso es obligatorio limpiar al cerrar sesión.

## Alternativas
- **`workbox-background-sync` (cola en el service worker):** reintenta incluso con la app cerrada, pero reenvía la petición original con el token viejo. Si el token venció, el servidor responde 401 y Workbox da el envío por exitoso, así que el gasto se pierde. Descartado.
- **Modo `generateSW`:** menos código, pero las estrategias quedan escondidas en la configuración y la decisión no se ve. Descartado.
- **Service worker en JS puro:** lo descartó la cátedra.
- **El service worker de Angular:** ver ADR-0001.
