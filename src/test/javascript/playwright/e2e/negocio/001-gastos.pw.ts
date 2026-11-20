import { expect, test } from '../../support/fixtures';
import { hoyArgentina } from './helpers/usuarios';

// FASE ROJA: escrito a mano con los textos de la spec 001 §5.1, porque "Mis gastos" todavía no existe.
// PENDIENTE: regrabar con `npx playwright codegen http://localhost:8080` cuando exista la UI, antes del verify.
test.use({ trace: 'on' });

test.describe('001 Gastos', () => {
  const descripcion = `E2E 001-CA15 ${Date.now()}`;

  test.beforeEach(async ({ jhi }) => {
    const { username, password } = jhi.credentials();
    await jhi.login(username, password);
  });

  test.afterEach(async ({ jhi }) => {
    // Limpieza por API por si el test falló a mitad de camino.
    const response = await jhi.authenticatedRequest({ url: `/api/gastos?descripcion.equals=${encodeURIComponent(descripcion)}` });
    const restantes = response.ok() ? ((await response.json()) as { id: number }[]) : [];
    for (const gasto of restantes) {
      await jhi.authenticatedRequest({ method: 'DELETE', url: `/api/gastos/${gasto.id}` });
    }
  });

  test('001-CA15 flujo E2E: carga, ve, edita y borra un gasto en "Mis gastos"', async ({ page }) => {
    await page.goto('/');

    // Cargar
    await page.getByRole('link', { name: 'Mis gastos' }).click();
    await page.getByRole('button', { name: 'Nuevo gasto' }).click();
    await page.getByLabel('Monto').fill('1500.50');
    await page.getByLabel('Fecha').fill(hoyArgentina());
    await page.getByLabel('Categoría').selectOption({ label: 'Supermercado' });
    await page.getByLabel('Descripción').fill(descripcion);
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Verlo en "Mis gastos"
    const fila = page.getByRole('row').filter({ hasText: descripcion });
    await expect(fila).toBeVisible();
    await expect(fila).toContainText('$ 1.500,50');
    await expect(fila).toContainText('Supermercado');

    // Editar
    await fila.getByRole('button', { name: 'Editar' }).click();
    await expect(page.getByLabel('Monto')).toHaveValue(/1500[.,]5/);
    await page.getByLabel('Monto').fill('2000');
    await page.getByLabel('Categoría').selectOption({ label: 'Transporte' });
    await page.getByRole('button', { name: 'Guardar' }).click();
    await expect(fila).toContainText('$ 2.000,00');
    await expect(fila).toContainText('Transporte');

    // Borrar
    await fila.getByRole('button', { name: 'Borrar' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Confirmar' }).click();
    await expect(page.getByRole('row').filter({ hasText: descripcion })).toHaveCount(0);
  });
});
