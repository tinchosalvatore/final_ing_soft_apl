import 'fake-indexeddb/auto';

import { OFFLINE_DB } from 'app/pwa/offline-db.config';

import {
  agregarPendiente,
  borrarPendientesDe,
  cerrarBaseParaTests,
  contarPendientes,
  listarPendientes,
  obtenerPendiente,
} from './pendientes.repositorio';

const gasto = (descripcion: string) => ({ monto: '100', fecha: '2026-11-20', descripcion, categoria: { id: 1, nombre: 'Ocio' } });

describe('Repositorio de pendientes (spec 003)', () => {
  afterEach(async () => {
    await cerrarBaseParaTests();
    await new Promise(resolve => {
      indexedDB.deleteDatabase(OFFLINE_DB.nombre).onsuccess = resolve;
    });
  });

  it('003-CA04: guarda el pendiente con el login de su dueño, en la base y el store del contrato', async () => {
    await agregarPendiente('ana', gasto('uno'));
    expect(await contarPendientes('ana')).toBe(1);
    const bases = await indexedDB.databases();
    expect(bases.find(b => b.name === 'tracker-offline')?.version).toBe(1);
  });

  it('003-CA05: lista en el orden en que se cargaron', async () => {
    await agregarPendiente('ana', gasto('uno'));
    await agregarPendiente('ana', gasto('dos'));
    await agregarPendiente('ana', gasto('tres'));
    expect((await listarPendientes('ana')).map(p => p.gasto.descripcion)).toEqual(['uno', 'dos', 'tres']);
  });

  it('003-CA10: nunca devuelve los pendientes de otro usuario', async () => {
    const clave = await agregarPendiente('ana', gasto('de ana'));
    expect(await listarPendientes('beto')).toEqual([]);
    expect(await obtenerPendiente(clave, 'beto')).toBeUndefined();
    expect((await obtenerPendiente(clave, 'ana'))?.gasto.descripcion).toBe('de ana');
  });

  it('003-CA09: al cerrar sesión se borran los pendientes del usuario', async () => {
    await agregarPendiente('ana', gasto('uno'));
    await agregarPendiente('ana', gasto('dos'));
    await agregarPendiente('beto', gasto('de beto'));
    await borrarPendientesDe('ana');
    expect(await contarPendientes('ana')).toBe(0);
    expect(await contarPendientes('beto')).toBe(1);
  });
});
