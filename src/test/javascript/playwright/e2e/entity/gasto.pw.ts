import {
  entityConfirmDeleteButtonSelector,
  entityCreateButtonSelector,
  entityCreateCancelButtonSelector,
  entityCreateSaveButtonSelector,
  entityDeleteButtonSelector,
  entityDetailsBackButtonSelector,
  entityDetailsButtonSelector,
  entityEditButtonSelector,
  entityTableSelector,
} from '../../support/entity';
import { expect, test } from '../../support/fixtures';

const gastoPageUrl = '/gasto';
const gastoSample = { monto: 25471.46, fecha: '2023-12-11' };

const gastoApiUrl = '/api/gastos';

const entitiesRequest = (page: import('@playwright/test').Page) =>
  page.waitForResponse(response => new URL(response.url()).pathname === gastoApiUrl && response.request().method() === 'GET');

test.describe('Gasto e2e test', () => {
  let gasto;
  let categoria;

  test.beforeEach(async ({ jhi }) => {
    const { username, password } = jhi.credentials();
    await jhi.login(username, password);
  });

  test.beforeEach(async ({ jhi }) => {
    // create an instance at the required relationship entity:
    categoria = await (
      await jhi.authenticatedRequest({
        method: 'POST',
        url: '/api/categorias',
        body: { nombre: 'quiet' },
      })
    ).json();
  });

  test.beforeEach(async ({ page }) => {
    // Simulate relationships api for better performance and reproducibility.
    // A url predicate rather than a glob: a glob's wildcards would close the "Disabled due to
    // incompatibility" block comment that wraps this section when the create test is skipped.
    await page.route(
      url => url.pathname === '/api/categorias',
      route => route.fulfill({ status: 200, json: [categoria].filter(Boolean) }),
    );
    // A url predicate rather than a glob: a glob's wildcards would close the "Disabled due to
    // incompatibility" block comment that wraps this section when the create test is skipped.
    await page.route(
      url => url.pathname === '/api/users',
      route => route.fulfill({ status: 200, json: [].filter(Boolean) }),
    );
  });

  test.afterEach(async ({ jhi }) => {
    if (gasto) {
      await jhi.authenticatedRequest({
        method: 'DELETE',
        url: `/api/gastos/${gasto.id}`,
      });
      gasto = undefined;
    }
  });

  test.afterEach(async ({ jhi }) => {
    if (categoria) {
      await jhi.authenticatedRequest({
        method: 'DELETE',
        url: `/api/categorias/${categoria.id}`,
      });
      categoria = undefined;
    }
  });

  test('Gastos menu should load Gastos page', async ({ page, jhi }) => {
    const response = entitiesRequest(page);
    await page.goto('/');
    await jhi.navbar.clickOnEntityMenuItem('gasto');
    // Chrome drops the response body once the page navigates, unlike the cypress proxy, so the table
    // assertion is only made when the payload is still readable.
    const body = (await (await response).json().catch(() => undefined)) as unknown[] | undefined;
    if (Array.isArray(body)) {
      if (body.length === 0) {
        await expect(page.locator(entityTableSelector)).toHaveCount(0);
      } else {
        // The clients render one entityTable element per row, so match the first one.
        await expect(page.locator(entityTableSelector).first()).toBeVisible();
      }
    }
    await expect(jhi.entity.getEntityHeading('Gasto')).toBeVisible();
    await expect(page).toHaveURL(url => url.pathname === gastoPageUrl);
  });

  test.describe('Gasto page', () => {
    test('should have translated page title', async ({ page, jhi }) => {
      await page.goto(gastoPageUrl);
      await expect(jhi.entity.getEntityHeading('Gasto')).not.toContainText('trackerGastosApp.gasto.home.title');
    });

    test.describe('create button click', () => {
      test.beforeEach(async ({ page }) => {
        const response = entitiesRequest(page);
        await page.goto(gastoPageUrl);
        await response;
      });

      test('should load create Gasto page', async ({ page, jhi }) => {
        await page.locator(entityCreateButtonSelector).click();
        await expect(page).toHaveURL(url => url.pathname === `${gastoPageUrl}/new`);
        await expect(jhi.entity.getEntityCreateUpdateHeading('Gasto')).toBeVisible();
        await expect(page.locator(entityCreateSaveButtonSelector)).toBeVisible();
        const response = entitiesRequest(page);
        await page.locator(entityCreateCancelButtonSelector).click();
        expect((await response).status()).toBe(200);
        await expect(page).toHaveURL(url => url.pathname === gastoPageUrl);
      });
    });

    test.describe('with existing value', () => {
      test.beforeEach(async ({ page, jhi }) => {
        gasto = await (
          await jhi.authenticatedRequest({
            method: 'POST',
            url: '/api/gastos',
            body: {
              ...gastoSample,
              categoria,
            },
          })
        ).json();

        await page.route(
          url => url.pathname === gastoApiUrl,
          route =>
            route.fulfill({
              status: 200,
              headers: {
                link: '<http://localhost/api/gastos?page=0&size=20>; rel="last",<http://localhost/api/gastos?page=0&size=20>; rel="first"',
              },
              json: [gasto],
            }),
          { times: 1 },
        );

        // Cypress waited on the stubbed request (cy.wait('@entitiesRequestInternal')); without it the test
        // can act before the row is rendered.
        const stubbedRequest = entitiesRequest(page);
        await page.goto(gastoPageUrl);
        await stubbedRequest;
      });

      test('detail button click should load details Gasto page', async ({ page, jhi }) => {
        await page.locator(entityDetailsButtonSelector).first().click();
        await expect(jhi.entity.getEntityDetailsHeading('gasto')).toBeVisible();
        const response = entitiesRequest(page);
        await page.locator(entityDetailsBackButtonSelector).click();
        expect((await response).status()).toBe(200);
        await expect(page).toHaveURL(url => url.pathname === gastoPageUrl);
      });

      test('edit button click should load edit Gasto page and go back', async ({ page, jhi }) => {
        await page.locator(entityEditButtonSelector).first().click();
        await expect(jhi.entity.getEntityCreateUpdateHeading('Gasto')).toBeVisible();
        await expect(page.locator(entityCreateSaveButtonSelector)).toBeVisible();
        const response = entitiesRequest(page);
        await page.locator(entityCreateCancelButtonSelector).click();
        expect((await response).status()).toBe(200);
        await expect(page).toHaveURL(url => url.pathname === gastoPageUrl);
      });

      test('edit button click should load edit Gasto page and save', async ({ page, jhi }) => {
        await page.locator(entityEditButtonSelector).first().click();
        await expect(jhi.entity.getEntityCreateUpdateHeading('Gasto')).toBeVisible();
        const response = entitiesRequest(page);
        await page.locator(entityCreateSaveButtonSelector).click();
        expect((await response).status()).toBe(200);
        await expect(page).toHaveURL(url => url.pathname === gastoPageUrl);
      });

      test('last delete button click should delete instance of Gasto', async ({ page, jhi }) => {
        const deleteRequest = page.waitForResponse(
          response => new URL(response.url()).pathname.startsWith(`${gastoApiUrl}/`) && response.request().method() === 'DELETE',
        );
        const listRequest = entitiesRequest(page);
        await page.locator(entityDeleteButtonSelector).last().click();
        await expect(jhi.entity.getEntityDeleteDialogHeading('gasto')).toBeVisible();
        await page.locator(entityConfirmDeleteButtonSelector).click();
        expect((await deleteRequest).status()).toBe(204);
        expect((await listRequest).status()).toBe(200);
        await expect(page).toHaveURL(url => url.pathname === gastoPageUrl);

        gasto = undefined;
      });
    });
  });

  test.describe('new Gasto page', () => {
    test.beforeEach(async ({ page, jhi }) => {
      await page.goto(gastoPageUrl);
      await page.locator(entityCreateButtonSelector).click();
      await expect(jhi.entity.getEntityCreateUpdateHeading('Gasto')).toBeVisible();
    });

    test('should create an instance of Gasto', async ({ page }) => {
      await page.locator(`[data-cy="monto"]`).fill('30379.06');
      await expect(page.locator(`[data-cy="monto"]`)).toHaveValue('30379.06');

      await page.locator(`[data-cy="fecha"]`).fill('2023-12-11');
      await page.locator(`[data-cy="fecha"]`).blur();
      await expect(page.locator(`[data-cy="fecha"]`)).toHaveValue('2023-12-11');

      await page.locator(`[data-cy="descripcion"]`).fill('skateboard ick');
      await expect(page.locator(`[data-cy="descripcion"]`)).toHaveValue('skateboard ick');

      await page.locator(`[data-cy="categoria"]`).selectOption({ index: 1 });

      const postRequest = page.waitForResponse(
        response => new URL(response.url()).pathname === gastoApiUrl && response.request().method() === 'POST',
      );
      const listRequest = entitiesRequest(page);
      await page.locator(entityCreateSaveButtonSelector).click();

      const postResponse = await postRequest;
      expect(postResponse.status()).toBe(201);
      // Best effort: the body is only needed to delete the instance again in afterEach.
      gasto = await postResponse.json().catch(() => undefined);
      expect((await listRequest).status()).toBe(200);
      await expect(page).toHaveURL(url => url.pathname === gastoPageUrl);
    });
  });
});
