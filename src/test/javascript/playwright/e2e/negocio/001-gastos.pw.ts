import { expect, test } from '../../support/fixtures';

// El humano grabó este test con `npx playwright codegen` el 2026-11-21. Es la parte A de
// docs/e2e/grabacion-codegen-2026-11-21.txt. El agente testing lo pulió:
// - el login usa el helper `jhi.login`;
// - las categorías se eligen por label, no por id;
// - la fila se busca por la descripción;
// - cada resultado del criterio tiene su aserción;
// - los datos se limpian por API.
test.use({ trace: 'on' });

test.describe('001 Gastos', () => {
  const descripcion = `Gasto de prueba E2E 001-CA15 ${Date.now()}`;

  test.beforeEach(async ({ jhi }) => {
    // El criterio pide el usuario `user` de JHipster.
    const { username, password } = jhi.credentials();
    await jhi.login(username, password);
  });

  test.afterEach(async ({ jhi }) => {
    // Limpieza por API, por si el test falló a mitad de camino.
    const response = await jhi.authenticatedRequest({ url: `/api/gastos?descripcion.equals=${encodeURIComponent(descripcion)}` });
    const restantes = response.ok() ? ((await response.json()) as { id: number }[]) : [];
    for (const gasto of restantes) {
      await jhi.authenticatedRequest({ method: 'DELETE', url: `/api/gastos/${gasto.id}` });
    }
  });

  test('001-CA15 flujo E2E: carga, ve, edita y borra un gasto en "Mis gastos"', async ({ page }) => {
    await page.goto('/');

    // Cargar (la fecha ya viene con la de hoy)
    await page.getByRole('link', { name: 'Mis gastos' }).click();
    await page.getByRole('button', { name: 'Nuevo gasto' }).click();
    await page.getByRole('textbox', { name: 'Monto' }).fill('1500,50');
    await page.getByLabel('Categoría').selectOption({ label: 'Supermercado' });
    await page.getByRole('textbox', { name: 'Descripción' }).fill(descripcion);
    await page.getByRole('button', { name: 'Guardar' }).click();

    // Verlo en "Mis gastos"
    const fila = page.getByRole('row').filter({ hasText: descripcion });
    await expect(fila).toBeVisible();
    await expect(fila).toContainText('$ 1.500,50');
    await expect(fila).toContainText('Supermercado');

    // Editar
    await fila.getByRole('button', { name: 'Editar' }).click();
    await expect(page.getByRole('textbox', { name: 'Monto' })).toHaveValue(/1500[.,]5/);
    await page.getByRole('textbox', { name: 'Monto' }).fill('2000');
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
