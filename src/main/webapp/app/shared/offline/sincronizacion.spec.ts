import 'fake-indexeddb/auto';

import { Storage } from 'react-jhipster';

import { AxiosError, AxiosHeaders } from 'axios';

import { OFFLINE_DB } from 'app/pwa/offline-db.config';
import { AUTHENTICATION_TOKEN_KEY } from 'app/shared/jhipster/constants';

import { agregarPendiente, cerrarBaseParaTests, listarPendientes } from './pendientes.repositorio';
import { clienteSincronizacion, loginDelToken, motivoDelRechazo, sincronizarPendientes } from './sincronizacion';

/** JWT sin firma válida: alcanza para leer el `sub` (la firma la verifica el backend). */
const jwtDe = (login: string) => `x.${btoa(JSON.stringify({ sub: login }))}.y`;

const errorHttp = (status: number, data: unknown = {}) =>
  new AxiosError('error', String(status), undefined, undefined, {
    status,
    data,
    statusText: '',
    headers: {},
    config: { headers: new AxiosHeaders() },
  });

const gasto = (descripcion: string) => ({ monto: '100', fecha: '2026-11-20', descripcion, categoria: { id: 1, nombre: 'Ocio' } });

describe('Sincronización de pendientes (spec 003)', () => {
  beforeEach(() => {
    Storage.session.set(AUTHENTICATION_TOKEN_KEY, jwtDe('ana'));
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    Storage.session.remove(AUTHENTICATION_TOKEN_KEY);
    await cerrarBaseParaTests();
    await new Promise(resolve => {
      indexedDB.deleteDatabase(OFFLINE_DB.nombre).onsuccess = resolve;
    });
  });

  it('003-CA05: envía los pendientes en orden y los borra al ser aceptados', async () => {
    const post = vi.spyOn(clienteSincronizacion, 'post').mockResolvedValue({ status: 201 });
    await agregarPendiente('ana', gasto('uno'));
    await agregarPendiente('ana', gasto('dos'));

    await sincronizarPendientes('ana');

    expect(post.mock.calls.map(([, body]) => (body as { descripcion: string }).descripcion)).toEqual(['uno', 'dos']);
    expect(post.mock.calls[0][2]?.headers).toEqual({ Authorization: `Bearer ${jwtDe('ana')}` });
    expect(await listarPendientes('ana')).toEqual([]);
  });

  it('003-CA06: un 400 deja el pendiente "rechazado" con el motivo y sigue con los siguientes', async () => {
    vi.spyOn(clienteSincronizacion, 'post')
      .mockRejectedValueOnce(errorHttp(400, { fieldErrors: [{ field: 'categoria', message: 'La categoría no existe' }] }))
      .mockResolvedValueOnce({ status: 201 });
    await agregarPendiente('ana', gasto('rechazado'));
    await agregarPendiente('ana', gasto('aceptado'));

    await sincronizarPendientes('ana');

    const quedan = await listarPendientes('ana');
    expect(quedan).toHaveLength(1);
    expect(quedan[0]).toMatchObject({ estado: 'rechazado', motivo: 'La categoría no existe' });
  });

  it('003-CA07: un 401 corta la sincronización y deja todo pendiente (sin pasar por el interceptor de logout)', async () => {
    const post = vi.spyOn(clienteSincronizacion, 'post').mockRejectedValue(errorHttp(401));
    await agregarPendiente('ana', gasto('uno'));
    await agregarPendiente('ana', gasto('dos'));

    await sincronizarPendientes('ana');

    expect(post).toHaveBeenCalledTimes(1);
    expect((await listarPendientes('ana')).map(p => p.estado)).toEqual(['pendiente', 'pendiente']);
  });

  it('003-CA10: no envía los pendientes de un usuario con el token de otro', async () => {
    Storage.session.set(AUTHENTICATION_TOKEN_KEY, jwtDe('beto'));
    const post = vi.spyOn(clienteSincronizacion, 'post');
    await agregarPendiente('ana', gasto('de ana'));

    await sincronizarPendientes('ana');

    expect(post).not.toHaveBeenCalled();
    expect(await listarPendientes('ana')).toHaveLength(1);
  });

  it('el motivo del rechazo es el mensaje del servidor', () => {
    expect(motivoDelRechazo({ fieldErrors: [{ message: 'El monto tiene que ser mayor a 0' }] })).toBe('El monto tiene que ser mayor a 0');
    expect(motivoDelRechazo({ detail: 'Detalle' })).toBe('Detalle');
    expect(motivoDelRechazo(undefined)).toBe('el servidor no aceptó el gasto');
  });

  it('lee el login (sub) del token, y devuelve undefined si no es un JWT', () => {
    expect(loginDelToken(jwtDe('ana'))).toBe('ana');
    expect(loginDelToken('token-vencido')).toBeUndefined();
  });
});
