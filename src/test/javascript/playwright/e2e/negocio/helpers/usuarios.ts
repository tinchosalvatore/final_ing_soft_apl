import { expect } from '@playwright/test';

import type { JHipster } from '../../../support/fixtures';

export interface UsuarioPrueba {
  login: string;
  password: string;
}

/**
 * Crea por API un usuario nuevo, activo y sin gastos (lo usan los E2E de negocio para afirmar valores exactos).
 * Se registra por /api/register y lo activa el admin. Al terminar, el test llama a `borrarUsuario`.
 */
export async function crearUsuarioActivo(jhi: JHipster): Promise<UsuarioPrueba> {
  const login = `e2e${Date.now()}`;
  const password = 'E2e-Prueba-2026';
  const { adminUsername, adminPassword } = jhi.credentials();
  await jhi.login(adminUsername, adminPassword);

  const registro = await jhi.authenticatedRequest({
    method: 'POST',
    url: '/api/register',
    body: { login, email: `${login}@prueba.local`, password, langKey: 'es' },
  });
  expect(registro.status()).toBe(201);

  const usuario = await (await jhi.authenticatedRequest({ url: `/api/admin/users/${login}` })).json();
  const activacion = await jhi.authenticatedRequest({
    method: 'PUT',
    url: `/api/admin/users/${login}`,
    body: { ...usuario, activated: true },
  });
  expect(activacion.status()).toBe(200);
  return { login, password };
}

/** Borra los gastos del usuario (con su propia sesión) y después el usuario (con la del admin). */
export async function borrarUsuario(jhi: JHipster, usuario: UsuarioPrueba): Promise<void> {
  await jhi.login(usuario.login, usuario.password);
  const gastos = (await (await jhi.authenticatedRequest({ url: '/api/gastos?size=100' })).json()) as { id: number }[];
  for (const gasto of Array.isArray(gastos) ? gastos : []) {
    await jhi.authenticatedRequest({ method: 'DELETE', url: `/api/gastos/${gasto.id}` });
  }
  const { adminUsername, adminPassword } = jhi.credentials();
  await jhi.login(adminUsername, adminPassword);
  await jhi.authenticatedRequest({ method: 'DELETE', url: `/api/admin/users/${usuario.login}` });
}

/** Hoy en America/Argentina/Buenos_Aires, como `AAAA-MM-DD` (spec 001, definición de "hoy"). */
export function hoyArgentina(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Argentina/Buenos_Aires' }).format(new Date());
}
