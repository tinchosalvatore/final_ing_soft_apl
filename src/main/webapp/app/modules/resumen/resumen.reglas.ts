/** Lógica pura de la pantalla "Resumen" (spec 002 §5.1). El cálculo lo hace el backend; acá solo se formatea. */
import { formatearMonto } from 'app/modules/gastos/gastos.reglas';

import { TEXTOS_RESUMEN } from './textos';

const PORCENTAJE_ES_AR = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

/** Participación: `75,0 %` (escala 0-100, como la devuelve el backend). */
export const formatearPorcentaje = (valor: number | string): string => `${PORCENTAJE_ES_AR.format(Number(valor))} %`;

/** Variación con signo: `+20,0 %`, `-100,0 %`, `0,0 %`. */
export const formatearVariacion = (valor: number | string): string => {
  const n = Number(valor);
  const signo = n > 0 ? '+' : n < 0 ? '-' : '';
  return `${signo}${formatearPorcentaje(Math.abs(n))}`;
};

/** Diferencia en pesos con signo (spec 002 §5.1): `+$ 200,00`, `-$ 200,00`, `$ 0,00`. */
export const formatearDiferencia = (valor: number | string): string => {
  const n = Number(valor);
  const signo = n > 0 ? '+' : n < 0 ? '-' : '';
  return `${signo}${formatearMonto(Math.abs(n))}`;
};

/** Sentido de la variación: "Aumento", "Baja" o, con diferencia exactamente 0, "Sin cambios". */
export const sentidoVariacion = (diferencia: number | string): string => {
  const n = Number(diferencia);
  return n > 0 ? TEXTOS_RESUMEN.aumento : n < 0 ? TEXTOS_RESUMEN.baja : TEXTOS_RESUMEN.sinCambios;
};

/** Mes `AAAA-MM` desplazado `delta` meses (sin Date, para no depender de la zona horaria). */
export const moverMes = (mes: string, delta: number): string => {
  const [anio, numero] = mes.split('-').map(Number);
  const indice = anio * 12 + (numero - 1) + delta;
  const nuevoAnio = Math.floor(indice / 12);
  const nuevoMes = (indice % 12) + 1;
  return `${nuevoAnio}-${String(nuevoMes).padStart(2, '0')}`;
};

/** El mes que contiene a "hoy" en Buenos Aires, como `AAAA-MM`. */
export const mesActual = (hoy: string): string => hoy.slice(0, 7);
