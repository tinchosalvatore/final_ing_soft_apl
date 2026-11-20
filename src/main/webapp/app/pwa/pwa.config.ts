import type { VitePWAOptions } from 'vite-plugin-pwa';

/**
 * Configuración de la PWA (spec 003, ADR-0005). Workbox genera el service worker a partir de esto (`generateSW`):
 * no hay ningún service worker escrito a mano. `vite.config.ts` solo importa este objeto.
 */
/**
 * Caché de las respuestas de la API (spec 003 §5.1). La usa también la app para borrarla al cerrar sesión.
 * Si cambia el formato de las respuestas guardadas, se sube la versión (`api-v2`).
 */
export const CACHE_API = 'api-v1';

export const pwaConfig: Partial<VitePWAOptions> = {
  strategies: 'generateSW',

  // ---------------------------------------------------------------------------------------------------------------
  // 1. Manifest: lo que hace que el navegador la reconozca como instalable (003-CA01). Se publica como
  //    /manifest.webmanifest y el plugin agrega el <link rel="manifest"> en index.html.
  // ---------------------------------------------------------------------------------------------------------------
  manifestFilename: 'manifest.webmanifest',
  manifest: {
    name: 'Tracker de Gastos', // nombre completo: al instalar y en la pantalla de bienvenida (punto 1)
    short_name: 'Gastos', // debajo del ícono, donde no entra el nombre completo
    description: 'Registrá tus gastos y mirá el resumen del mes',
    lang: 'es-AR',
    start_url: '/', // la ventana instalada abre en la raíz; el router de React decide a dónde va
    scope: '/', // URLs que quedan "dentro" de la app instalada (igual que el scope del service worker)
    display: 'standalone', // ventana propia, sin barra de direcciones (punto 1)
    orientation: 'portrait', // pensada para el celular (mobile first)
    theme_color: '#2c3e50', // barra de título o de estado; igual al <meta name="theme-color"> de index.html
    background_color: '#ffffff', // fondo de la pantalla de bienvenida mientras carga
    // Los PNG se publican en la raíz (los copia vite.config.ts), que Spring sirve sin sesión.
    icons: [
      { src: '/icono-192.png', sizes: '192x192', type: 'image/png' }, // mínimo que pide Chrome
      { src: '/icono-512.png', sizes: '512x512', type: 'image/png' }, // bienvenida e instalación
      { src: '/icono-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }, // Android lo recorta
    ],
  },

  // ---------------------------------------------------------------------------------------------------------------
  // 2. Registro y precache del app shell: la interfaz queda guardada al instalar el SW y abre sin red (003-CA02).
  // ---------------------------------------------------------------------------------------------------------------
  // Sin recarga automática: como no hay aviso de "versión nueva" (fuera de alcance), la versión nueva se activa la
  // próxima vez que se abre la app (punto 16).
  registerType: 'prompt',
  // El plugin agrega en index.html un registerSW.js que registra /sw.js con scope /.
  injectRegister: 'script',
  workbox: {
    // Todo el JS (también los módulos que se cargan bajo demanda, como "Mis gastos" y "Resumen"), CSS, HTML, íconos,
    // fuentes y las traducciones. Workbox guarda cada archivo con el hash de su contenido: en una versión nueva solo
    // se vuelve a descargar lo que cambió.
    globPatterns: ['**/*.{js,css,html,png,svg,ico,woff2,json}'],
    globIgnores: ['swagger-ui/**'], // Swagger es una herramienta del ADMIN: no hace falta sin red
    maximumFileSizeToCacheInBytes: 3 * 1024 * 1024, // por defecto Workbox no precachea archivos de más de 2 MB

    // Navegación offline de la SPA: cualquier URL que se abra (/mis-gastos, /resumen...) se responde con index.html
    // y React decide qué pantalla mostrar...
    navigateFallback: 'index.html',
    // ...salvo lo que no es la SPA: la API, actuator, la documentación de la API y Swagger.
    navigateFallbackDenylist: [/^\/api\//, /^\/management\//, /^\/v3\/api-docs/, /^\/swagger-ui\//],

    // Al publicar una versión nueva, borra el precache de la vieja. Sin skipWaiting ni clientsClaim: el SW nuevo
    // espera a que se cierre la app, así no cambia la versión a mitad de una carga (punto 16).
    cleanupOutdatedCaches: true,

    // -------------------------------------------------------------------------------------------------------------
    // 3. Estrategias de caché en runtime: una entrada por tipo de recurso. Lo que no es GET no se cachea nunca.
    // -------------------------------------------------------------------------------------------------------------
    runtimeCaching: [
      {
        // NetworkFirst: con red, siempre los datos del servidor (punto 6); sin red, la última respuesta guardada (punto 4).
        urlPattern: ({ url, sameOrigin }) => sameOrigin && /^\/api\/(gastos|categorias|resumen)(\/|$)/.test(url.pathname),
        method: 'GET',
        handler: 'NetworkFirst',
        options: {
          cacheName: CACHE_API, // 'api-v1' (spec §5.1); la app la borra al cerrar sesión y cuando entra otro usuario
          networkTimeoutSeconds: 5, // con señal mala (más de 5 s sin respuesta) usa lo guardado
          cacheableResponse: { statuses: [200] }, // nunca se guarda un 401, 404 o 500
          expiration: { maxEntries: 50 }, // cada filtro o página es una entrada: tope para no crecer sin límite
        },
      },
      {
        // NetworkFirst para la cuenta: sin red, la app necesita saber quién tiene la sesión para abrir las pantallas privadas (003-CA03).
        urlPattern: ({ url, sameOrigin }) => sameOrigin && url.pathname === '/api/account',
        method: 'GET',
        handler: 'NetworkFirst',
        options: {
          cacheName: CACHE_API, // misma caché: se borra junto con los datos al cerrar sesión
          networkTimeoutSeconds: 5,
          cacheableResponse: { statuses: [200] }, // con la sesión vencida el servidor responde 401 y eso no se guarda
        },
      },
      {
        // NetworkOnly para el resto de la API (login, administración, cuenta): datos sensibles o que solo tienen sentido con el servidor.
        urlPattern: ({ url, sameOrigin }) => sameOrigin && url.pathname.startsWith('/api/'),
        handler: 'NetworkOnly',
      },
    ],
  },
};

export default pwaConfig;
