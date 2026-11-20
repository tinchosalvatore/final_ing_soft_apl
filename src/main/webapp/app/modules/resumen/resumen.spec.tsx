import React from 'react';

import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { Resumen } from './resumen';

// La pantalla pide el resumen al backend; acá no interesa el contenido, solo el selector de mes.
vi.mock('app/shared/offline/conexion', () => ({
  cargarConCache: vi.fn(() => Promise.resolve({ estado: 'no-disponible' })),
}));

describe('002 Resumen — límites del selector de mes (spec 002, punto 6)', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('002-CA10 "Mes anterior" se deshabilita en 2000-01 (el primer mes que se puede pedir)', () => {
    vi.setSystemTime(new Date('2000-02-15T15:00:00Z')); // mes actual en Buenos Aires: 2000-02
    render(<Resumen />);

    const anterior = screen.getByRole('button', { name: 'Mes anterior' });
    expect((anterior as HTMLButtonElement).disabled).toBe(false);

    fireEvent.click(anterior);

    expect(screen.getByText('2000-01')).toBeTruthy();
    expect((screen.getByRole('button', { name: 'Mes anterior' }) as HTMLButtonElement).disabled).toBe(true);
  });

  it('002-CA10 control: "Mes anterior" está habilitado en un mes posterior a 2000-01', () => {
    vi.setSystemTime(new Date('2026-11-20T15:00:00Z'));
    render(<Resumen />);

    expect((screen.getByRole('button', { name: 'Mes anterior' }) as HTMLButtonElement).disabled).toBe(false);
  });
});
