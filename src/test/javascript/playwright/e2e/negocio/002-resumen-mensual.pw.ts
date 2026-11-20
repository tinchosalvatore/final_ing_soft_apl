import { expect, test } from '../../support/fixtures';
import { type UsuarioPrueba, borrarUsuario, crearUsuarioActivo, hoyArgentina } from './helpers/usuarios';

// FASE ROJA: escrito a mano con los textos de la spec 002 §5.1, porque la pantalla "Resumen" todavía no existe.
// PENDIENTE: regrabar con `npx playwright codegen http://localhost:8080` cuando exista la UI, antes del verify.
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
    usuario = await crearUsuarioActivo(jhi);
    await jhi.login(usuario.login, usuario.password);
    await page.goto('/');

    for (const { monto, categoria } of [
      { monto: '1000', categoria: 'Supermercado' },
      { monto: '500', categoria: 'Transporte' },
    ]) {
      await page.getByRole('link', { name: 'Mis gastos' }).click();
      await page.getByRole('button', { name: 'Nuevo gasto' }).click();
      await page.getByLabel('Monto').fill(monto);
      await page.getByLabel('Fecha').fill(hoyArgentina());
      await page.getByLabel('Categoría').selectOption({ label: categoria });
      await page.getByRole('button', { name: 'Guardar' }).click();
      await expect(page.getByRole('cell', { name: categoria })).toBeVisible();
    }

    await page.getByRole('link', { name: 'Resumen' }).click();

    await expect(page.getByText('$ 1.500,00')).toBeVisible();
    const supermercado = page.getByRole('row').filter({ hasText: 'Supermercado' });
    await expect(supermercado).toContainText('66,7 %');
    const transporte = page.getByRole('row').filter({ hasText: 'Transporte' });
    await expect(transporte).toContainText('33,3 %');
  });
});
