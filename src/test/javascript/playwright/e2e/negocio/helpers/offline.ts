import { type APIRequestContext, type Page, expect, request as playwrightRequest } from '@playwright/test';

import { type JHipster, test } from '../../../support/fixtures';
import { hoyArgentina } from './usuarios';

/** Nombres de la spec 003 §5.1 (contrato observable). */
export const CACHE_API = 'api-v1';
export const DB_OFFLINE = 'tracker-offline';
export const STORE_PENDIENTES = 'pendientes';

/** Mañana en America/Argentina/Buenos_Aires, como AAAA-MM-DD. */
export function mananaArgentina(): string {
  const [anio, mes, dia] = hoyArgentina().split('-').map(Number);
  return new Date(Date.UTC(anio, mes - 1, dia + 1)).toISOString().slice(0, 10);
}

/** Espera a que el service worker esté activo y controle la página (si no la controla, recarga una vez). */
export async function esperarServiceWorker(page: Page): Promise<void> {
  await page.evaluate(() => navigator.serviceWorker.ready.then(() => undefined));
  const controlada = await page.evaluate(() => navigator.serviceWorker.controller !== null);
  if (!controlada) {
    await page.reload();
    await page.evaluate(() => navigator.serviceWorker.ready.then(() => undefined));
  }
  await expect.poll(() => page.evaluate(() => navigator.serviceWorker.controller !== null)).toBe(true);
}

/**
 * Cuenta los pendientes de la IndexedDB del contrato. Con `login`, usa el índice `login`; sin él, cuenta todos.
 * No crea la base si no existe (abrirla crearía una versión 1 vacía y rompería la de la app).
 */
export async function contarPendientes(page: Page, login?: string): Promise<number> {
  return page.evaluate(
    async ([db, store, usuario]) => {
      const bases = await indexedDB.databases();
      if (!bases.some(b => b.name === db)) return 0;
      return new Promise<number>((resolve, reject) => {
        const apertura = indexedDB.open(db);
        apertura.onerror = () => reject(new Error(String(apertura.error)));
        apertura.onsuccess = () => {
          const conexion = apertura.result;
          if (!conexion.objectStoreNames.contains(store)) {
            conexion.close();
            resolve(0);
            return;
          }
          const almacen = conexion.transaction(store, 'readonly').objectStore(store);
          const pedido = usuario ? almacen.index('login').count(usuario) : almacen.count();
          pedido.onsuccess = () => {
            conexion.close();
            resolve(pedido.result);
          };
          pedido.onerror = () => reject(new Error(String(pedido.error)));
        };
      });
    },
    [DB_OFFLINE, STORE_PENDIENTES, login ?? ''] as const,
  );
}

/** Cantidad de respuestas de la API guardadas en la caché del contrato (0 si la caché no existe). */
export async function respuestasEnCache(page: Page): Promise<number> {
  return page.evaluate(async nombre => {
    if (!(await caches.keys()).includes(nombre)) return 0;
    return (await (await caches.open(nombre)).keys()).length;
  }, CACHE_API);
}

/** Crea un gasto por API con la sesión del navegador (tiene que haber red). */
export async function crearGastoPorApi(jhi: JHipster, datos: { monto: number; categoria: string; descripcion: string }): Promise<void> {
  const categorias = (await (await jhi.authenticatedRequest({ url: '/api/categorias' })).json()) as { id: number; nombre: string }[];
  const categoria = categorias.find(c => c.nombre === datos.categoria);
  expect(categoria, `categoría ${datos.categoria}`).toBeDefined();
  const respuesta = await jhi.authenticatedRequest({
    method: 'POST',
    url: '/api/gastos',
    body: { monto: datos.monto, fecha: hoyArgentina(), descripcion: datos.descripcion, categoria: { id: categoria!.id } },
  });
  expect(respuesta.status()).toBe(201);
}

/** Cuántos gastos con esa descripción tiene en el servidor el usuario de la sesión (tiene que haber red). */
export async function gastosEnServidor(jhi: JHipster, descripcion: string): Promise<number> {
  const respuesta = await jhi.authenticatedRequest({ url: `/api/gastos?descripcion.equals=${encodeURIComponent(descripcion)}` });
  if (!respuesta.ok()) return -1;
  return ((await respuesta.json()) as unknown[]).length;
}

/**
 * Contexto de API del admin, separado del navegador: no le afecta `context.setOffline` (lo usa 003-CA06 para
 * borrar una categoría mientras el usuario está sin conexión).
 */
export async function apiAdmin(jhi: JHipster): Promise<APIRequestContext> {
  const { adminUsername, adminPassword } = jhi.credentials();
  const baseURL = test.info().project.use.baseURL;
  const anonimo = await playwrightRequest.newContext({ baseURL });
  const login = await anonimo.post('/api/authenticate', { data: { username: adminUsername, password: adminPassword } });
  expect(login.status()).toBe(200);
  const { id_token: token } = (await login.json()) as { id_token: string };
  await anonimo.dispose();
  return playwrightRequest.newContext({ baseURL, extraHTTPHeaders: { Authorization: `Bearer ${token}` } });
}

/** Carga un gasto con el formulario de "Mis gastos" (con o sin red). */
export async function cargarGasto(
  page: Page,
  datos: { monto: string; categoria: string; descripcion: string; fecha?: string },
): Promise<void> {
  await page.getByRole('link', { name: 'Mis gastos' }).click();
  await page.getByRole('button', { name: 'Nuevo gasto' }).click();
  await page.getByLabel('Monto').fill(datos.monto);
  await page.getByLabel('Fecha').fill(datos.fecha ?? hoyArgentina());
  await page.getByLabel('Categoría').selectOption({ label: datos.categoria });
  await page.getByLabel('Descripción').fill(datos.descripcion);
  await page.getByRole('button', { name: 'Guardar' }).click();
}

/** Con red: recorre "Mis gastos" y abre el formulario, así quedan guardados el listado y las categorías. */
export async function verConConexion(page: Page): Promise<void> {
  await page.getByRole('link', { name: 'Mis gastos' }).click();
  await expect(page.getByRole('button', { name: 'Nuevo gasto' })).toBeVisible();
  await page.getByRole('button', { name: 'Nuevo gasto' }).click();
  await expect(page.getByLabel('Categoría')).toBeVisible();
  await page.getByRole('button', { name: 'Cancelar' }).click();
  await expect(page.getByRole('button', { name: 'Nuevo gasto' })).toBeVisible();
}
