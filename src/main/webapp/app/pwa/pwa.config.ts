import type { VitePWAOptions } from 'vite-plugin-pwa';

/**
 * Configuración de la PWA (spec 003, ADR-0005). Workbox genera el service worker a partir de esto (`generateSW`):
 * no hay ningún service worker escrito a mano. `vite.config.ts` solo importa este objeto.
 */
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
    icons: [
      { src: '/content/images/pwa/icono-192.png', sizes: '192x192', type: 'image/png' }, // mínimo que pide Chrome
      { src: '/content/images/pwa/icono-512.png', sizes: '512x512', type: 'image/png' }, // bienvenida e instalación
      { src: '/content/images/pwa/icono-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }, // Android lo recorta
    ],
  },
};

export default pwaConfig;
