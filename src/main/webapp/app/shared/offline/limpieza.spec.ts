import { Storage } from 'react-jhipster';

import { AUTHENTICATION_TOKEN_KEY } from 'app/shared/jhipster/constants';

import { borrarCacheSiNoHaySesion } from './limpieza';

describe('Limpieza de datos guardados (spec 003, punto 15)', () => {
  const borrar = vi.fn(() => Promise.resolve(true));

  beforeEach(() => {
    Object.assign(globalThis, { caches: { delete: borrar } });
    borrar.mockClear();
  });

  afterEach(() => {
    Storage.session.remove(AUTHENTICATION_TOKEN_KEY);
  });

  it('003-CA10: sin token al abrir la app, borra la caché api-v1', async () => {
    await borrarCacheSiNoHaySesion();
    expect(borrar).toHaveBeenCalledWith('api-v1');
  });

  it('con token, la conserva (sirve para abrir sin red)', async () => {
    Storage.session.set(AUTHENTICATION_TOKEN_KEY, 'x.y.z');
    await borrarCacheSiNoHaySesion();
    expect(borrar).not.toHaveBeenCalled();
  });
});
