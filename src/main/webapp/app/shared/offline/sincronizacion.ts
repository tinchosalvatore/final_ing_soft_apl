import { Storage } from 'react-jhipster';

import axios from 'axios';

import { AUTHENTICATION_TOKEN_KEY } from 'app/shared/jhipster/constants';

import { TEXTOS_OFFLINE } from './textos';
import { type Pendiente, actualizarPendiente, borrarPendiente, listarPendientes } from './pendientes.repositorio';

/**
 * Sincronización de pendientes (spec 003, puntos 9 a 12; ADR-0005).
 *
 * Usa una instancia de axios PROPIA: la global tiene el interceptor de JHipster que, ante un 401, limpia la sesión.
 * Un 401 acá significa "sesión vencida" y el pendiente tiene que quedar en IndexedDB hasta el próximo login (§5.1).
 */
const cliente = axios.create({ baseURL: SERVER_API_URL, timeout: 60_000 });

interface Problem {
  fieldErrors?: { message?: string }[];
  detail?: string;
  title?: string;
}

/** Motivo del rechazo: el mensaje del servidor (error de campo de la spec 001 o el detalle del problem). */
export const motivoDelRechazo = (problem: Problem | undefined): string =>
  problem?.fieldErrors?.find(e => e.message)?.message ?? problem?.detail ?? problem?.title ?? TEXTOS_OFFLINE.motivoDesconocido;

const tokenActual = (): string | undefined => Storage.local.get(AUTHENTICATION_TOKEN_KEY) || Storage.session.get(AUTHENTICATION_TOKEN_KEY);

export type ResultadoEnvio = 'aceptado' | 'rechazado' | 'reintentar-despues';

/** Envía un pendiente. 2xx → se borra; 400 → rechazado con el motivo; 401, error de red u otro → queda pendiente. */
export const enviarPendiente = async (pendiente: Pendiente, token: string): Promise<ResultadoEnvio> => {
  const { monto, fecha, descripcion, categoria } = pendiente.gasto;
  try {
    await cliente.post(
      'api/gastos',
      { monto, fecha, descripcion, categoria: { id: categoria.id } },
      { headers: { Authorization: `Bearer ${token}` } },
    );
    await borrarPendiente(pendiente.clave!);
    return 'aceptado';
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 400) {
      await actualizarPendiente({ ...pendiente, estado: 'rechazado', motivo: motivoDelRechazo(error.response.data as Problem) });
      return 'rechazado';
    }
    return 'reintentar-despues';
  }
};

let enCurso: Promise<void> | null = null;

const ejecutar = async (login: string): Promise<void> => {
  const token = tokenActual();
  if (!token) return;
  for (const pendiente of await listarPendientes(login)) {
    if (pendiente.estado !== 'pendiente') continue;
    // Un rechazo no frena a los siguientes (caso borde de la spec); un 401 o un corte de red, sí.
    if ((await enviarPendiente(pendiente, token)) === 'reintentar-despues') return;
  }
};

/** Envía los pendientes del usuario en el orden en que se cargaron. Nunca corren dos sincronizaciones a la vez. */
export const sincronizarPendientes = (login: string): Promise<void> => {
  enCurso ??= ejecutar(login)
    .catch(() => undefined)
    .finally(() => {
      enCurso = null;
    });
  return enCurso;
};
