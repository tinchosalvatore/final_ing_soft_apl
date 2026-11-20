import type { RuntimeCaching } from 'workbox-build';

import { CACHE_API, pwaConfig } from './pwa.config';

const entradas = pwaConfig.workbox?.runtimeCaching as RuntimeCaching[];

/** Primera entrada de `runtimeCaching` que atiende la URL (Workbox usa la primera que coincide). */
const estrategiaPara = (ruta: string, method = 'GET') => {
  const url = new URL(ruta, 'http://localhost:8080');
  const entrada = entradas.find(
    e =>
      (e.method ?? 'GET') === method &&
      typeof e.urlPattern === 'function' &&
      e.urlPattern({ url, sameOrigin: true, request: new Request(url), event: {} as never }),
  );
  return entrada ? { handler: entrada.handler, cache: entrada.options?.cacheName } : undefined;
};

describe('Configuración de Workbox (spec 003, ADR-0005)', () => {
  it('003-CA01: genera el SW con Workbox (generateSW) y el manifest del contrato', () => {
    expect(pwaConfig.strategies).toBe('generateSW');
    expect(pwaConfig.manifestFilename).toBe('manifest.webmanifest');
    expect(pwaConfig.manifest).toMatchObject({ name: 'Tracker de Gastos', short_name: 'Gastos', display: 'standalone', start_url: '/' });
  });

  it('003-CA03: NetworkFirst en api-v1 para gastos, categorías, resumen y la cuenta', () => {
    expect(CACHE_API).toBe('api-v1');
    for (const ruta of ['/api/gastos?page=0', '/api/gastos/3', '/api/categorias', '/api/resumen?mes=2026-11', '/api/account']) {
      expect(estrategiaPara(ruta)).toEqual({ handler: 'NetworkFirst', cache: 'api-v1' });
    }
  });

  it('el resto de la API (login, administración) es NetworkOnly', () => {
    for (const ruta of ['/api/authenticate', '/api/admin/users', '/api/account/change-password']) {
      expect(estrategiaPara(ruta)?.handler).toBe('NetworkOnly');
    }
  });

  it('lo que no es GET no se cachea nunca', () => {
    expect(estrategiaPara('/api/gastos', 'POST')?.handler).not.toBe('NetworkFirst');
  });

  it('003-CA02: la navegación offline cae en index.html, salvo la API', () => {
    expect(pwaConfig.workbox?.navigateFallback).toBe('index.html');
    expect(pwaConfig.workbox?.navigateFallbackDenylist?.some(r => r.test('/api/gastos'))).toBe(true);
  });
});
