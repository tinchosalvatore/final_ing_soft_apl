import { expect, test } from '../../support/fixtures';
import { contarPendientes, esperarServiceWorker, gastosEnServidor, verConConexion } from './helpers/offline';
import { type UsuarioPrueba, borrarUsuario, crearUsuarioActivo } from './helpers/usuarios';

// DEMO, no reemplaza a 003-pwa-offline.pw.ts (que cubre 003-CA01 a CA11).
// El humano lo grabó con `npx playwright codegen` el 2026-11-21: es la parte C de docs/e2e/grabacion-codegen-2026-11-21.txt.
// Codegen no graba el corte de red de las DevTools, así que lo reemplaza `context.setOffline`.
// Corre contra el jar de prod, porque el service worker solo existe ahí.
test.use({ trace: 'on' });

test.describe('003 Demo: un gasto sin conexión que se sincroniza solo', () => {
  let usuario: UsuarioPrueba | undefined;

  test.afterEach(async ({ context, jhi }) => {
    await context.setOffline(false);
    if (usuario) await borrarUsuario(jhi, usuario);
    usuario = undefined;
  });

  test('003-CA04 y 003-CA05 demo: cargar sin red, verlo pendiente y verlo sincronizarse al volver la red', async ({
    jhi,
    page,
    context,
  }) => {
    usuario = await crearUsuarioActivo(jhi);
    await jhi.login(usuario.login, usuario.password);
    await page.goto('/');
    await esperarServiceWorker(page);
    await verConConexion(page); // con red quedan guardados "Mis gastos" y las categorías
    const descripcion = `Gasto sin conexión E2E demo ${Date.now()}`;

    await context.setOffline(true);
    await page.getByRole('button', { name: 'Nuevo gasto' }).click();
    await page.getByRole('textbox', { name: 'Monto' }).fill('250');
    await page.getByLabel('Categoría').selectOption({ label: 'Ocio' });
    await page.getByRole('textbox', { name: 'Descripción' }).fill(descripcion);
    await page.getByRole('button', { name: 'Guardar' }).click();

    const fila = page.getByRole('row').filter({ hasText: descripcion });
    await expect(fila).toContainText('Pendiente de sincronizar');
    await expect(page.getByText('1 pendiente', { exact: true })).toBeVisible();

    await context.setOffline(false);

    await expect(fila).not.toContainText('Pendiente de sincronizar');
    await expect(fila).toContainText('$ 250,00');
    await expect.poll(() => gastosEnServidor(jhi, descripcion)).toBe(1);
    expect(await contarPendientes(page, usuario.login)).toBe(0);
  });
});
