import { expect, test } from '../../support/fixtures';
import {
  apiAdmin,
  cargarGasto,
  contarPendientes,
  crearGastoPorApi,
  esperarServiceWorker,
  gastosEnServidor,
  mananaArgentina,
  respuestasEnCache,
  verConConexion,
} from './helpers/offline';
import { type UsuarioPrueba, borrarUsuario, crearUsuarioActivo } from './helpers/usuarios';

// FASE ROJA: escrito a mano con el contrato de la spec 003 §5.1, porque la PWA todavía no existe.
// PENDIENTE: el humano lo regraba con codegen cuando exista la PWA; el agente testing pule el código grabado.
// Corre contra el JAR DE PROD: el service worker solo existe en el build de producción (ver tasks.md).
test.use({ trace: 'on' });

const AVISO_GUARDADOS = 'Sin conexión: mostrando datos guardados';
const PENDIENTE = 'Pendiente de sincronizar';

test.describe('003 PWA y carga sin conexión', () => {
  let usuario: UsuarioPrueba | undefined;
  let otros: UsuarioPrueba[] = [];

  /** Usuario nuevo y sin gastos, con la sesión iniciada y el service worker controlando la página. */
  async function entrarConUsuarioNuevo(jhi: Parameters<typeof crearUsuarioActivo>[0]): Promise<UsuarioPrueba> {
    usuario = await crearUsuarioActivo(jhi);
    await jhi.login(usuario.login, usuario.password);
    await jhi.page.goto('/');
    await esperarServiceWorker(jhi.page);
    return usuario;
  }

  test.afterEach(async ({ context, jhi }) => {
    await context.setOffline(false);
    for (const u of [usuario, ...otros]) {
      if (u) await borrarUsuario(jhi, u);
    }
    usuario = undefined;
    otros = [];
  });

  test('003-CA01 instalable: manifest válido y service worker activo con scope /', async ({ page, jhi }) => {
    await page.goto('/');

    await expect(page.locator('link[rel="manifest"]')).toHaveAttribute('href', '/manifest.webmanifest');
    const respuesta = await jhi.request.get('/manifest.webmanifest');
    expect(respuesta.status()).toBe(200);
    const manifest = await respuesta.json();
    expect(manifest.name).toBe('Tracker de Gastos');
    expect(manifest.short_name).toBe('Gastos');
    expect(manifest.display).toBe('standalone');
    expect(manifest.start_url).toBe('/');
    const iconos = manifest.icons as { sizes: string; purpose?: string }[];
    expect(iconos.map(i => i.sizes)).toEqual(expect.arrayContaining(['192x192', '512x512']));
    expect(iconos.some(i => (i.purpose ?? '').includes('maskable'))).toBe(true);

    const scope = await page.evaluate(async () => (await navigator.serviceWorker.ready).scope);
    expect(new URL(scope).pathname).toBe('/');
    // `ready` resuelve con el SW en "activating" o "activated": se espera a que termine de activarse.
    await expect.poll(() => page.evaluate(async () => (await navigator.serviceWorker.ready).active?.state)).toBe('activated');
  });

  test('003-CA02 abre sin conexión: después de abrirla con red, recarga sin red y la interfaz carga', async ({ page, context, jhi }) => {
    await entrarConUsuarioNuevo(jhi);

    await context.setOffline(true);
    await page.reload();

    expect(page.url()).not.toContain('chrome-error');
    await expect(page.getByRole('link', { name: 'Mis gastos' })).toBeVisible();
  });

  test('003-CA03 consulta sin conexión: "Mis gastos" muestra los datos guardados con el aviso', async ({ page, context, jhi }) => {
    await entrarConUsuarioNuevo(jhi);
    const descripcion = `E2E 003-CA03 ${Date.now()}`;
    await crearGastoPorApi(jhi, { monto: 1234.5, categoria: 'Supermercado', descripcion });
    await page.getByRole('link', { name: 'Mis gastos' }).click();
    await expect(page.getByRole('row').filter({ hasText: descripcion })).toBeVisible();
    await expect(page.getByText(AVISO_GUARDADOS)).toHaveCount(0); // con red: datos del servidor, sin aviso (punto 6)

    await context.setOffline(true);
    await page.reload();
    await page.getByRole('link', { name: 'Mis gastos' }).click();

    await expect(page.getByRole('row').filter({ hasText: descripcion })).toContainText('$ 1.234,50');
    await expect(page.getByText(AVISO_GUARDADOS)).toBeVisible();
  });

  test('003-CA03 borde: una pantalla nunca vista con red muestra que no está disponible sin conexión', async ({ page, context, jhi }) => {
    await entrarConUsuarioNuevo(jhi);

    await context.setOffline(true);
    await page.getByRole('link', { name: 'Resumen' }).click();

    await expect(page.getByText('Esta información no está disponible sin conexión')).toBeVisible();
  });

  test('003-CA04 carga sin conexión: el gasto queda "Pendiente de sincronizar" y el indicador muestra 1', async ({
    page,
    context,
    jhi,
  }) => {
    const u = await entrarConUsuarioNuevo(jhi);
    await verConConexion(page);
    const descripcion = `E2E 003-CA04 ${Date.now()}`;

    await context.setOffline(true);
    await cargarGasto(page, { monto: '500', categoria: 'Transporte', descripcion });

    await expect(page.getByRole('row').filter({ hasText: descripcion })).toContainText(PENDIENTE);
    await expect(page.getByText('1 pendiente', { exact: true })).toBeVisible();
    expect(await contarPendientes(page, u.login)).toBe(1);
  });

  test('003-CA04 borde: sin conexión no se puede editar ni borrar un gasto ya sincronizado (punto 13)', async ({ page, context, jhi }) => {
    await entrarConUsuarioNuevo(jhi);
    const descripcion = `E2E 003-CA04b ${Date.now()}`;
    await crearGastoPorApi(jhi, { monto: 100, categoria: 'Ocio', descripcion });
    await verConConexion(page);

    await context.setOffline(true);
    const fila = page.getByRole('row').filter({ hasText: descripcion });

    await expect(fila.getByRole('button', { name: 'Editar' })).toBeDisabled();
    await expect(fila.getByRole('button', { name: 'Borrar' })).toBeDisabled();
    await expect(fila.getByText('No disponible sin conexión').first()).toBeVisible();
  });

  test('003-CA05 sincronización: con 2 pendientes y la red de vuelta, llegan una vez cada uno', async ({ page, context, jhi }) => {
    const u = await entrarConUsuarioNuevo(jhi);
    await verConConexion(page);
    const primero = `E2E 003-CA05 uno ${Date.now()}`;
    const segundo = `E2E 003-CA05 dos ${Date.now()}`;

    await context.setOffline(true);
    await cargarGasto(page, { monto: '100', categoria: 'Supermercado', descripcion: primero });
    await cargarGasto(page, { monto: '200', categoria: 'Transporte', descripcion: segundo });
    await expect(page.getByText('2 pendientes', { exact: true })).toBeVisible();

    await context.setOffline(false);

    await expect(page.getByText(PENDIENTE)).toHaveCount(0);
    await expect(page.getByText(/\d+ pendientes?$/)).toHaveCount(0);
    await expect.poll(() => gastosEnServidor(jhi, primero)).toBe(1);
    await expect.poll(() => gastosEnServidor(jhi, segundo)).toBe(1);
    expect(await contarPendientes(page, u.login)).toBe(0);
    await expect(page.getByRole('row').filter({ hasText: primero })).toBeVisible();
    await expect(page.getByRole('row').filter({ hasText: segundo })).toBeVisible();
  });

  test('003-CA06 rechazo: categoría borrada antes de sincronizar → "Rechazado", se corrige con "Reintentar"', async ({
    page,
    context,
    jhi,
  }) => {
    const admin = await apiAdmin(jhi);
    const nombre = `E2E003 ${Date.now()}`.slice(0, 40);
    const categoria = (await (await admin.post('/api/categorias', { data: { nombre } })).json()) as { id: number };
    const u = await entrarConUsuarioNuevo(jhi);
    await verConConexion(page);
    const descripcion = `E2E 003-CA06 ${Date.now()}`;

    await context.setOffline(true);
    await cargarGasto(page, { monto: '300', categoria: nombre, descripcion });
    await expect(page.getByRole('row').filter({ hasText: descripcion })).toContainText(PENDIENTE);
    expect((await admin.delete(`/api/categorias/${categoria.id}`)).status()).toBe(204);
    await context.setOffline(false);

    const fila = page.getByRole('row').filter({ hasText: descripcion });
    await expect(fila).toContainText(/Rechazado: \S+/);
    await expect(fila.getByRole('button', { name: 'Reintentar' })).toBeVisible();
    await expect(fila.getByRole('button', { name: 'Descartar' })).toBeVisible();

    // Corregir: "Reintentar" abre el formulario con los datos del pendiente; se cambia la categoría y se reenvía.
    await fila.getByRole('button', { name: 'Reintentar' }).click();
    await expect(page.getByText('Corregir gasto pendiente')).toBeVisible();
    await expect(page.getByLabel('Descripción')).toHaveValue(descripcion);
    await page.getByLabel('Categoría').selectOption({ label: 'Otros' });
    await page.getByRole('button', { name: 'Guardar' }).click();

    await expect.poll(() => gastosEnServidor(jhi, descripcion)).toBe(1);
    await expect(page.getByRole('row').filter({ hasText: descripcion })).not.toContainText('Rechazado');
    expect(await contarPendientes(page, u.login)).toBe(0);
    await admin.dispose();
  });

  test('003-CA06 rechazo: un pendiente rechazado se puede descartar', async ({ page, context, jhi }) => {
    const admin = await apiAdmin(jhi);
    const nombre = `E2E003d ${Date.now()}`.slice(0, 40);
    const categoria = (await (await admin.post('/api/categorias', { data: { nombre } })).json()) as { id: number };
    const u = await entrarConUsuarioNuevo(jhi);
    await verConConexion(page);
    const descripcion = `E2E 003-CA06d ${Date.now()}`;

    await context.setOffline(true);
    await cargarGasto(page, { monto: '300', categoria: nombre, descripcion });
    expect((await admin.delete(`/api/categorias/${categoria.id}`)).status()).toBe(204);
    await context.setOffline(false);
    const fila = page.getByRole('row').filter({ hasText: descripcion });
    await expect(fila).toContainText(/Rechazado: \S+/);

    await fila.getByRole('button', { name: 'Descartar' }).click();

    await expect(page.getByRole('row').filter({ hasText: descripcion })).toHaveCount(0);
    expect(await contarPendientes(page, u.login)).toBe(0);
    expect(await gastosEnServidor(jhi, descripcion)).toBe(0);
    await admin.dispose();
  });

  test('003-CA07 sesión vencida: el pendiente se mantiene y se sincroniza después de iniciar sesión', async ({ page, context, jhi }) => {
    const u = await entrarConUsuarioNuevo(jhi);
    await verConConexion(page);
    const descripcion = `E2E 003-CA07 ${Date.now()}`;

    await context.setOffline(true);
    await cargarGasto(page, { monto: '150', categoria: 'Salud', descripcion });
    // Sesión vencida: el token del navegador deja de ser válido mientras no hay red.
    await page.evaluate(() => window.sessionStorage.setItem('jhi-authenticationToken', JSON.stringify('token-vencido')));
    await context.setOffline(false);

    // Al volver la red con la sesión vencida, el pendiente sigue en el dispositivo y no llega al servidor.
    // Se espera a propósito: hay que dar tiempo a que la app intente sincronizar y comprobar que NO lo hizo.
    // eslint-disable-next-line playwright/no-wait-for-timeout
    await page.waitForTimeout(3000);
    expect(await gastosEnServidor(jhi, descripcion)).toBe(-1); // con el token vencido, la API responde 401
    expect(await contarPendientes(page, u.login)).toBe(1);

    await jhi.login(u.login, u.password);
    await page.goto('/mis-gastos');

    await expect.poll(() => gastosEnServidor(jhi, descripcion)).toBe(1);
    await expect.poll(() => contarPendientes(page, u.login)).toBe(0);
  });

  test('003-CA08 validación offline: monto 0 y fecha futura dan el error de siempre y no crean pendientes', async ({
    page,
    context,
    jhi,
  }) => {
    const u = await entrarConUsuarioNuevo(jhi);
    await verConConexion(page);

    await context.setOffline(true);
    await cargarGasto(page, { monto: '0', categoria: 'Ocio', descripcion: 'E2E 003-CA08 monto' });
    await expect(page.getByText('El monto tiene que ser mayor a 0')).toBeVisible();

    await page.getByLabel('Monto').fill('100');
    await page.getByLabel('Fecha').fill(mananaArgentina());
    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(page.getByText('La fecha no puede ser posterior a hoy')).toBeVisible();

    expect(await contarPendientes(page, u.login)).toBe(0);
    await expect(page.getByText(/\d+ pendientes?$/)).toHaveCount(0);
  });

  test('003-CA09 logout limpia datos: aviso de 1 pendiente y no quedan datos de la API ni pendientes', async ({ page, context, jhi }) => {
    await entrarConUsuarioNuevo(jhi);
    await verConConexion(page);
    await context.setOffline(true);
    await cargarGasto(page, { monto: '50', categoria: 'Otros', descripcion: `E2E 003-CA09 ${Date.now()}` });
    expect(await respuestasEnCache(page)).toBeGreaterThan(0); // control: había datos guardados

    await jhi.navbar.clickOnLogoutItem();
    await expect(page.getByText('Tenés 1 gasto sin sincronizar. Si cerrás sesión se va a perder.')).toBeVisible();
    await page.getByRole('button', { name: 'Cerrar sesión igual' }).click();
    // El logout termina recargando la página en "/": se espera a que cargue antes de inspeccionar el navegador.
    await page.waitForURL(url => url.pathname === '/');

    await expect.poll(() => respuestasEnCache(page)).toBe(0);
    await expect.poll(() => contarPendientes(page)).toBe(0);
  });

  test('003-CA09 logout con pendientes: "Cancelar" no cierra la sesión ni borra nada', async ({ page, context, jhi }) => {
    const u = await entrarConUsuarioNuevo(jhi);
    await verConConexion(page);
    await context.setOffline(true);
    await cargarGasto(page, { monto: '50', categoria: 'Otros', descripcion: `E2E 003-CA09c uno ${Date.now()}` });
    await cargarGasto(page, { monto: '60', categoria: 'Otros', descripcion: `E2E 003-CA09c dos ${Date.now()}` });

    await jhi.navbar.clickOnLogoutItem();
    await expect(page.getByText('Tenés 2 gastos sin sincronizar. Si cerrás sesión se van a perder.')).toBeVisible();
    await page.getByRole('button', { name: 'Cancelar' }).click();

    expect(await contarPendientes(page, u.login)).toBe(2);
    await expect(page.getByRole('link', { name: 'Mis gastos' })).toBeVisible();
  });

  test('003-CA10 aislamiento en el dispositivo: beto, sin red, no ve nada de ana', async ({ page, context, jhi }) => {
    const ana = await entrarConUsuarioNuevo(jhi);
    const deAna = `E2E 003-CA10 de ana ${Date.now()}`;
    const pendienteDeAna = `E2E 003-CA10 pendiente de ana ${Date.now()}`;
    await crearGastoPorApi(jhi, { monto: 999, categoria: 'Salud', descripcion: deAna });
    await verConConexion(page);
    await expect(page.getByRole('row').filter({ hasText: deAna })).toBeVisible();
    await context.setOffline(true);
    await cargarGasto(page, { monto: '77', categoria: 'Salud', descripcion: pendienteDeAna });
    await jhi.navbar.clickOnLogoutItem();
    await page.getByRole('button', { name: 'Cerrar sesión igual' }).click();
    await context.setOffline(false);

    const beto = await crearUsuarioActivo(jhi);
    otros.push(beto);
    await jhi.login(beto.login, beto.password);
    await page.goto('/');
    await context.setOffline(true);
    await page.getByRole('link', { name: 'Mis gastos' }).click();

    await expect(page.getByText(deAna)).toHaveCount(0);
    await expect(page.getByText(pendienteDeAna)).toHaveCount(0);
    await expect(page.getByText(PENDIENTE)).toHaveCount(0);
    expect(await contarPendientes(page, ana.login)).toBe(0);
  });
});

test.describe('003 PWA en el celular', () => {
  test.use({ viewport: { width: 360, height: 740 } });

  let usuario: UsuarioPrueba | undefined;

  test.afterEach(async ({ jhi }) => {
    if (usuario) await borrarUsuario(jhi, usuario);
    usuario = undefined;
  });

  test('003-CA11 mobile: "Mis gastos", el formulario y el resumen se usan sin scroll horizontal a 360×740', async ({ page, jhi }) => {
    usuario = await crearUsuarioActivo(jhi);
    await jhi.login(usuario.login, usuario.password);
    const sinScrollHorizontal = () => page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth);

    await page.goto('/mis-gastos');
    await expect(page.getByRole('button', { name: 'Nuevo gasto' })).toBeVisible();
    expect(await sinScrollHorizontal()).toBe(true);

    await page.getByRole('button', { name: 'Nuevo gasto' }).click();
    await expect(page.getByLabel('Monto')).toBeVisible();
    expect(await sinScrollHorizontal()).toBe(true);

    await page.goto('/resumen');
    await expect(page.getByRole('button', { name: 'Mes anterior' })).toBeVisible();
    expect(await sinScrollHorizontal()).toBe(true);
  });
});
