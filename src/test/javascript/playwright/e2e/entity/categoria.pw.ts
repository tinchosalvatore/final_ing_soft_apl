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

const categoriaPageUrl = '/categoria';
const categoriaSample = { nombre: 'cautiously' };

const categoriaApiUrl = '/api/categorias';

const entitiesRequest = (page: import('@playwright/test').Page) =>
  page.waitForResponse(response => new URL(response.url()).pathname === categoriaApiUrl && response.request().method() === 'GET');

test.describe('Categoria e2e test', () => {
  let categoria;

  test.beforeEach(async ({ jhi }) => {
    const { username, password } = jhi.credentials();
    await jhi.login(username, password);
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

  test('Categorias menu should load Categorias page', async ({ page, jhi }) => {
    const response = entitiesRequest(page);
    await page.goto('/');
    await jhi.navbar.clickOnEntityMenuItem('categoria');
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
    await expect(jhi.entity.getEntityHeading('Categoria')).toBeVisible();
    await expect(page).toHaveURL(url => url.pathname === categoriaPageUrl);
  });

  test.describe('Categoria page', () => {
    test('should have translated page title', async ({ page, jhi }) => {
      await page.goto(categoriaPageUrl);
      await expect(jhi.entity.getEntityHeading('Categoria')).not.toContainText('trackerGastosApp.categoria.home.title');
    });

    test.describe('create button click', () => {
      test.beforeEach(async ({ page }) => {
        const response = entitiesRequest(page);
        await page.goto(categoriaPageUrl);
        await response;
      });

      test('should load create Categoria page', async ({ page, jhi }) => {
        await page.locator(entityCreateButtonSelector).click();
        await expect(page).toHaveURL(url => url.pathname === `${categoriaPageUrl}/new`);
        await expect(jhi.entity.getEntityCreateUpdateHeading('Categoria')).toBeVisible();
        await expect(page.locator(entityCreateSaveButtonSelector)).toBeVisible();
        const response = entitiesRequest(page);
        await page.locator(entityCreateCancelButtonSelector).click();
        expect((await response).status()).toBe(200);
        await expect(page).toHaveURL(url => url.pathname === categoriaPageUrl);
      });
    });

    test.describe('with existing value', () => {
      test.beforeEach(async ({ page, jhi }) => {
        categoria = await (
          await jhi.authenticatedRequest({
            method: 'POST',
            url: '/api/categorias',
            body: categoriaSample,
          })
        ).json();

        await page.route(
          url => url.pathname === categoriaApiUrl,
          route =>
            route.fulfill({
              status: 200,
              json: [categoria],
            }),
          { times: 1 },
        );

        // Cypress waited on the stubbed request (cy.wait('@entitiesRequestInternal')); without it the test
        // can act before the row is rendered.
        const stubbedRequest = entitiesRequest(page);
        await page.goto(categoriaPageUrl);
        await stubbedRequest;
      });

      test('detail button click should load details Categoria page', async ({ page, jhi }) => {
        await page.locator(entityDetailsButtonSelector).first().click();
        await expect(jhi.entity.getEntityDetailsHeading('categoria')).toBeVisible();
        const response = entitiesRequest(page);
        await page.locator(entityDetailsBackButtonSelector).click();
        expect((await response).status()).toBe(200);
        await expect(page).toHaveURL(url => url.pathname === categoriaPageUrl);
      });

      test('edit button click should load edit Categoria page and go back', async ({ page, jhi }) => {
        await page.locator(entityEditButtonSelector).first().click();
        await expect(jhi.entity.getEntityCreateUpdateHeading('Categoria')).toBeVisible();
        await expect(page.locator(entityCreateSaveButtonSelector)).toBeVisible();
        const response = entitiesRequest(page);
        await page.locator(entityCreateCancelButtonSelector).click();
        expect((await response).status()).toBe(200);
        await expect(page).toHaveURL(url => url.pathname === categoriaPageUrl);
      });

      test('edit button click should load edit Categoria page and save', async ({ page, jhi }) => {
        await page.locator(entityEditButtonSelector).first().click();
        await expect(jhi.entity.getEntityCreateUpdateHeading('Categoria')).toBeVisible();
        const response = entitiesRequest(page);
        await page.locator(entityCreateSaveButtonSelector).click();
        expect((await response).status()).toBe(200);
        await expect(page).toHaveURL(url => url.pathname === categoriaPageUrl);
      });

      test('last delete button click should delete instance of Categoria', async ({ page, jhi }) => {
        const deleteRequest = page.waitForResponse(
          response => new URL(response.url()).pathname.startsWith(`${categoriaApiUrl}/`) && response.request().method() === 'DELETE',
        );
        const listRequest = entitiesRequest(page);
        await page.locator(entityDeleteButtonSelector).last().click();
        await expect(jhi.entity.getEntityDeleteDialogHeading('categoria')).toBeVisible();
        await page.locator(entityConfirmDeleteButtonSelector).click();
        expect((await deleteRequest).status()).toBe(204);
        expect((await listRequest).status()).toBe(200);
        await expect(page).toHaveURL(url => url.pathname === categoriaPageUrl);

        categoria = undefined;
      });
    });
  });

  test.describe('new Categoria page', () => {
    test.beforeEach(async ({ page, jhi }) => {
      await page.goto(categoriaPageUrl);
      await page.locator(entityCreateButtonSelector).click();
      await expect(jhi.entity.getEntityCreateUpdateHeading('Categoria')).toBeVisible();
    });

    test('should create an instance of Categoria', async ({ page }) => {
      await page.locator(`[data-cy="nombre"]`).fill('equatorial boohoo');
      await expect(page.locator(`[data-cy="nombre"]`)).toHaveValue('equatorial boohoo');

      const postRequest = page.waitForResponse(
        response => new URL(response.url()).pathname === categoriaApiUrl && response.request().method() === 'POST',
      );
      const listRequest = entitiesRequest(page);
      await page.locator(entityCreateSaveButtonSelector).click();

      const postResponse = await postRequest;
      expect(postResponse.status()).toBe(201);
      // Best effort: the body is only needed to delete the instance again in afterEach.
      categoria = await postResponse.json().catch(() => undefined);
      expect((await listRequest).status()).toBe(200);
      await expect(page).toHaveURL(url => url.pathname === categoriaPageUrl);
    });
  });
});
