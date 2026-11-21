import { expect, test } from '../../support/fixtures';
import { type UsuarioPrueba, borrarUsuario, crearUsuarioActivo } from './helpers/usuarios';

// El humano grabó este test con `npx playwright codegen` el 2026-11-21. Es la parte B de
// docs/e2e/grabacion-codegen-2026-11-21.txt. El agente testing lo pulió:
// - el usuario nuevo se crea por API;
// - el login usa el helper `jhi.login`;
// - las categorías se eligen por label;
// - cada resultado del criterio tiene su aserción;
// - el usuario se borra al final.
test.use({ trace: 'on' });

test.describe('002 Resumen mensual', () => {
  let usuario: UsuarioPrueba | undefined;

  test.afterEach(async ({ jhi }) => {
    if (usuario) {
      await borrarUsuario(jhi, usuario);
      usuario = undefined;
    }
  });

  test('002-CA12 flujo E2E: carga 2 gastos y ve el resumen con total y participación', async ({ jhi, page }) => {
    // Usuario nuevo y sin gastos, para que los valores sean exactos (spec 002, CA12)
    usuario = await crearUsuarioActivo(jhi);
    await jhi.login(usuario.login, usuario.password);
    await page.goto('/');
    await page.getByRole('link', { name: 'Mis gastos' }).click();

    for (const { monto, categoria } of [
      { monto: '1000', categoria: 'Supermercado' },
      { monto: '500', categoria: 'Transporte' },
    ]) {
      await page.getByRole('button', { name: 'Nuevo gasto' }).click();
      await page.getByRole('textbox', { name: 'Monto' }).fill(monto);
      await page.getByLabel('Categoría').selectOption({ label: categoria });
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('cell', { name: categoria })).toBeVisible();
    }

    await page.getByRole('link', { name: 'Resumen' }).click();

    // exact: la diferencia contra el mes anterior también muestra el monto, con signo (+$ 1.500,00)
    await expect(page.getByText('$ 1.500,00', { exact: true })).toBeVisible();
    // Usuario nuevo: el mes anterior está en 0, así que la diferencia se informa y la variación porcentual no (spec 002 §3.3)
    await expect(page.getByText('+$ 1.500,00', { exact: true })).toBeVisible();
    await expect(page.getByText('Sin datos del mes anterior')).toBeVisible();
    await expect(page.getByRole('row').filter({ hasText: 'Supermercado' })).toContainText('66,7 %');
    await expect(page.getByRole('row').filter({ hasText: 'Transporte' })).toContainText('33,3 %');
  });
});
