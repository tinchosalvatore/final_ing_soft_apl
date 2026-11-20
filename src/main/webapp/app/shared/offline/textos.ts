/**
 * Textos de la PWA (spec 003 §5.1). Son literales a propósito: los E2E de la 003 buscan exactamente estos textos.
 */
export const TEXTOS_OFFLINE = {
  datosGuardados: 'Sin conexión: mostrando datos guardados',
  noDisponible: 'Esta información no está disponible sin conexión',
  pendiente: 'Pendiente de sincronizar',
  rechazado: (motivo: string) => `Rechazado: ${motivo}`,
  motivoDesconocido: 'el servidor no aceptó el gasto',
  reintentar: 'Reintentar',
  descartar: 'Descartar',
  noDisponibleSinConexion: 'No disponible sin conexión',
  corregirTitulo: 'Corregir gasto pendiente',
  indicador: (n: number) => (n === 1 ? '1 pendiente' : `${n} pendientes`),
  avisoLogout: (n: number) =>
    n === 1
      ? 'Tenés 1 gasto sin sincronizar. Si cerrás sesión se va a perder.'
      : `Tenés ${n} gastos sin sincronizar. Si cerrás sesión se van a perder.`,
  cerrarIgual: 'Cerrar sesión igual',
  cancelar: 'Cancelar',
  cerrarTitulo: 'Cerrar la sesión',
} as const;
