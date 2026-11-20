import { describe, expect, it } from 'vitest';

import { erroresDelBackend, formatearFecha, formatearMonto, hoyArgentina, normalizarMonto, validarGasto } from './gastos.reglas';

const HOY = '2026-11-20';
const valido = { monto: '1500,50', fecha: HOY, categoriaId: '1', descripcion: '' };

describe('Mis gastos — formato (spec 001 §5.1)', () => {
  it('formatea montos como $ 1.500,50 (es-AR, con espacio común)', () => {
    expect(formatearMonto(1500.5)).toBe('$ 1.500,50');
    expect(formatearMonto('2000')).toBe('$ 2.000,00');
    expect(formatearMonto(10000000)).toBe('$ 10.000.000,00');
    expect(formatearMonto(0.01)).toBe('$ 0,01');
  });

  it('formatea fechas AAAA-MM-DD como DD/MM/AAAA sin corrimiento de zona', () => {
    expect(formatearFecha('2026-01-01')).toBe('01/01/2026');
  });

  it('calcula hoy en Buenos Aires: a las 01:00 UTC todavía es el día anterior', () => {
    expect(hoyArgentina(new Date('2026-11-21T01:00:00Z'))).toBe('2026-11-20');
    expect(hoyArgentina(new Date('2026-11-21T04:00:00Z'))).toBe('2026-11-21');
  });

  it('acepta coma o punto decimal y rechaza texto', () => {
    expect(normalizarMonto('1500,50')).toBe('1500.50');
    expect(normalizarMonto(' 1500.5 ')).toBe('1500.5');
    expect(normalizarMonto('1.500,50')).toBeNull();
    expect(normalizarMonto('abc')).toBeNull();
  });
});

describe('Mis gastos — validaciones del formulario (mismas reglas que el backend, spec 001 regla 2)', () => {
  it('un gasto válido no tiene errores', () => {
    expect(validarGasto(valido, HOY)).toEqual({});
  });

  it.each(['0', '-10', '12,345', '10000000,01'])('001-CA02 monto inválido: %s', monto => {
    expect(validarGasto({ ...valido, monto }, HOY).monto).toBeDefined();
  });

  it.each(['0,01', '10000000', '10000000,00', '12,30'])('001-CA02 borde válido: %s', monto => {
    expect(validarGasto({ ...valido, monto }, HOY).monto).toBeUndefined();
  });

  it('001-CA03 fecha de mañana es inválida y la de hoy es válida', () => {
    expect(validarGasto({ ...valido, fecha: '2026-11-21' }, HOY).fecha).toBeDefined();
    expect(validarGasto({ ...valido, fecha: HOY }, HOY).fecha).toBeUndefined();
  });

  it('exige categoría y limita la descripción a 140 caracteres', () => {
    const errores = validarGasto({ ...valido, categoriaId: '', descripcion: 'x'.repeat(141) }, HOY);
    expect(errores.categoria).toBeDefined();
    expect(errores.descripcion).toBeDefined();
  });
});

describe('Mis gastos — errores del backend junto al campo (regla 15)', () => {
  it('mapea fieldErrors del problem de JHipster a los campos del formulario', () => {
    const problem = {
      fieldErrors: [
        { objectName: 'gasto', field: 'monto', message: 'El monto tiene que ser mayor a 0' },
        { objectName: 'gasto', field: 'otro', message: 'ignorado' },
      ],
    };
    expect(erroresDelBackend(problem)).toEqual({ monto: 'El monto tiene que ser mayor a 0' });
    expect(erroresDelBackend(undefined)).toEqual({});
  });
});
