/**
 * Textos de "Mis gastos" (spec 001 §5.1). Son literales a propósito: la app es solo en español y los E2E buscan
 * exactamente estos textos. Cambiar uno de estos rompe el E2E 001-CA15.
 */
export const TEXTOS = {
  menu: 'Mis gastos',
  titulo: 'Mis gastos',
  nuevo: 'Nuevo gasto',
  editarTitulo: 'Editar gasto',
  monto: 'Monto',
  fecha: 'Fecha',
  categoria: 'Categoría',
  descripcion: 'Descripción',
  guardar: 'Guardar',
  cancelar: 'Cancelar',
  editar: 'Editar',
  borrar: 'Borrar',
  confirmar: 'Confirmar',
  confirmarBorrado: '¿Borrar este gasto? No se puede deshacer.',
  borrarTitulo: 'Borrar gasto',
  desde: 'Desde',
  hasta: 'Hasta',
  filtroCategoria: 'Filtrar por categoría',
  todas: 'Todas',
  elegir: 'Elegí una categoría',
  sinGastos: 'No hay gastos para mostrar.',
  anterior: 'Anterior',
  siguiente: 'Siguiente',
  errorGenerico: 'No se pudo guardar el gasto. Probá de nuevo.',
  noEncontrado: 'El gasto no existe.',
} as const;
