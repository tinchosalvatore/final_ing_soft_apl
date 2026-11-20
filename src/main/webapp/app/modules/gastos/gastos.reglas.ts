/**
 * Lógica pura de "Mis gastos": formato de montos y validaciones para la experiencia de uso.
 * Las mismas reglas las valida el backend, que es la autoridad (spec 001, regla 2; defensa S4).
 */

/** "Hoy" según la spec 001: la fecha actual en America/Argentina/Buenos_Aires, como AAAA-MM-DD. */
export const hoyArgentina = (ahora: Date = new Date()): string =>
  new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Argentina/Buenos_Aires' }).format(ahora);

const NUMERO_ES_AR = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** Formato de montos de la spec 001 §5.1: `$ 1.500,50` (punto de miles, coma decimal, espacio común). */
export const formatearMonto = (monto: number | string | null | undefined): string =>
  monto === null || monto === undefined || monto === '' ? '' : `$ ${NUMERO_ES_AR.format(Number(monto))}`;

/** Fecha AAAA-MM-DD → DD/MM/AAAA, sin pasar por Date (evita corrimientos de zona horaria). */
export const formatearFecha = (fecha: string | null | undefined): string => {
  if (!fecha) return '';
  const [anio, mes, dia] = fecha.split('-');
  return `${dia}/${mes}/${anio}`;
};

const MONTO_TEXTO = /^\d+([.,]\d+)?$/;

/** Interpreta lo que escribió el usuario ("1500,50" o "1500.50"). Devuelve el texto normalizado con punto, o null. */
export const normalizarMonto = (texto: string): string | null => {
  const limpio = texto.trim();
  return MONTO_TEXTO.test(limpio) ? limpio.replace(',', '.') : null;
};

export interface ValoresGasto {
  monto: string;
  fecha: string;
  categoriaId: string;
  descripcion: string;
}

export type ErroresGasto = Partial<Record<'monto' | 'fecha' | 'categoria' | 'descripcion', string>>;

const MONTO_MAXIMO = 10_000_000;

/** Reglas del punto 2 de la spec 001, con los mismos mensajes que devuelve el backend. */
export const validarGasto = (valores: ValoresGasto, hoy: string = hoyArgentina()): ErroresGasto => {
  const errores: ErroresGasto = {};
  const monto = normalizarMonto(valores.monto);
  if (!valores.monto.trim()) {
    errores.monto = 'El monto es obligatorio';
  } else if (monto === null) {
    errores.monto = 'El monto tiene que ser un número (por ejemplo, 1500,50)';
  } else if (Number(monto) <= 0) {
    errores.monto = 'El monto tiene que ser mayor a 0';
  } else if (Number(monto) > MONTO_MAXIMO) {
    errores.monto = 'El monto no puede superar 10.000.000';
  } else if ((monto.split('.')[1] ?? '').replace(/0+$/, '').length > 2) {
    errores.monto = 'El monto puede tener como máximo 2 decimales';
  }
  if (!valores.fecha) {
    errores.fecha = 'La fecha es obligatoria';
  } else if (valores.fecha > hoy) {
    errores.fecha = 'La fecha no puede ser posterior a hoy';
  }
  if (!valores.categoriaId) {
    errores.categoria = 'La categoría es obligatoria';
  }
  if (valores.descripcion.length > 140) {
    errores.descripcion = 'La descripción puede tener como máximo 140 caracteres';
  }
  return errores;
};

interface FieldErrorVM {
  field: string;
  message: string;
}

/** Errores de campo del backend (problem de JHipster, spec 001 §5.1) → errores del formulario (regla 15). */
export const erroresDelBackend = (problem: { fieldErrors?: FieldErrorVM[] } | undefined): ErroresGasto => {
  const errores: ErroresGasto = {};
  for (const { field, message } of problem?.fieldErrors ?? []) {
    if (field === 'monto' || field === 'fecha' || field === 'categoria' || field === 'descripcion') {
      errores[field] = message;
    }
  }
  return errores;
};
