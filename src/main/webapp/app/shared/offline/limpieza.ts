import { Storage } from 'react-jhipster';

import axios from 'axios';

import { AUTHENTICATION_TOKEN_KEY } from 'app/shared/jhipster/constants';

import { CACHE_API } from 'app/pwa/pwa.config';

import { borrarPendientesDe } from './pendientes.repositorio';

/** Último usuario que usó la app en este dispositivo (solo el login, sin datos). */
const ULTIMO_LOGIN = 'tracker-offline-ultimo-login';

/** Borra las respuestas de la API guardadas. Cache Storage se puede borrar desde la página, sin pasar por el SW. */
export const borrarCacheApi = async (): Promise<void> => {
  if ('caches' in globalThis) await caches.delete(CACHE_API);
};

/** Punto 14: al cerrar sesión se borran los datos guardados y los pendientes del usuario. */
export const limpiarAlCerrarSesion = async (login: string | undefined): Promise<void> => {
  await borrarCacheApi();
  if (login) await borrarPendientesDe(login);
  localStorage.removeItem(ULTIMO_LOGIN);
};

/**
 * Punto 15: si la sesión de otro usuario venció sin cerrar sesión, sus datos guardados no se le muestran al nuevo.
 * Cuando inicia sesión un usuario distinto del último, se borra la caché y se vuelve a guardar su `/api/account`
 * (sin eso la app no podría abrir con su sesión sin conexión).
 */
export const alIniciarSesion = async (login: string): Promise<void> => {
  if (localStorage.getItem(ULTIMO_LOGIN) === login) return;
  await borrarCacheApi();
  localStorage.setItem(ULTIMO_LOGIN, login);
  await axios.get('api/account').catch(() => undefined);
};

/**
 * Al abrir la app: si no hay token (la sesión se perdió sin cerrar sesión, por ejemplo al cerrar el navegador con un
 * token de sesión), se borra la caché de la API. Los pendientes no se tocan: esperan el login de su dueño (punto 12).
 */
export const borrarCacheSiNoHaySesion = async (): Promise<void> => {
  if (!Storage.local.get(AUTHENTICATION_TOKEN_KEY) && !Storage.session.get(AUTHENTICATION_TOKEN_KEY)) {
    await borrarCacheApi();
  }
};
