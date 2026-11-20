import { describe, expect, it } from 'vitest';

import { formatearDiferencia, formatearPorcentaje, formatearVariacion, mesActual, moverMes, sentidoVariacion } from './resumen.reglas';

describe('Resumen — formato (spec 002 §5.1)', () => {
  it('participación con 1 decimal y coma: 75,0 % / 66,7 %', () => {
    expect(formatearPorcentaje(75)).toBe('75,0 %');
    expect(formatearPorcentaje('66.7')).toBe('66,7 %');
  });

  it('variación con signo: +20,0 %, -100,0 % y 0,0 %', () => {
    expect(formatearVariacion(20)).toBe('+20,0 %');
    expect(formatearVariacion(-100)).toBe('-100,0 %');
    expect(formatearVariacion(0)).toBe('0,0 %');
  });

  it('diferencia en pesos con signo: +$ 200,00, -$ 200,00 y $ 0,00', () => {
    expect(formatearDiferencia(200)).toBe('+$ 200,00');
    expect(formatearDiferencia('-200')).toBe('-$ 200,00');
    expect(formatearDiferencia(0)).toBe('$ 0,00');
  });

  it('sentido: Aumento, Baja o Sin cambios', () => {
    expect(sentidoVariacion(1)).toBe('Aumento');
    expect(sentidoVariacion(-0.01)).toBe('Baja');
    expect(sentidoVariacion(0)).toBe('Sin cambios');
  });
});

describe('Resumen — selector de mes', () => {
  it('mueve meses cruzando el año (002-CA06: el anterior a 2026-01 es 2025-12)', () => {
    expect(moverMes('2026-01', -1)).toBe('2025-12');
    expect(moverMes('2025-12', 1)).toBe('2026-01');
    expect(moverMes('2026-11', -1)).toBe('2026-10');
  });

  it('el mes actual es el de hoy en Buenos Aires', () => {
    expect(mesActual('2026-11-20')).toBe('2026-11');
  });
});
