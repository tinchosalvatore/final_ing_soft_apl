/**
 * Aviso dentro de la página de que cambiaron los pendientes (se agregó, se sincronizó, se rechazó o se descartó uno).
 * Lo escuchan el indicador del menú y "Mis gastos" para volver a leer IndexedDB y el listado.
 */
const EVENTO = 'tracker:pendientes';

export const avisarCambioPendientes = (): void => {
  globalThis.dispatchEvent(new Event(EVENTO));
};

export const alCambiarPendientes = (escuchar: () => void): (() => void) => {
  globalThis.addEventListener(EVENTO, escuchar);
  return () => globalThis.removeEventListener(EVENTO, escuchar);
};
