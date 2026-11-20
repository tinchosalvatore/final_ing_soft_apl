import { type DBSchema, type IDBPDatabase, openDB } from 'idb';

import { OFFLINE_DB } from 'app/pwa/offline-db.config';

import { avisarCambioPendientes } from './eventos';

/** Datos del gasto tal como se mandan a `POST /api/gastos` (spec 001), más el nombre de la categoría para mostrarlo. */
export interface GastoPendienteDatos {
  monto: string;
  fecha: string;
  descripcion: string | null;
  categoria: { id: number; nombre?: string };
}

/** Un gasto cargado sin conexión (spec 003, puntos 8 y 11). La clave autoincremental da el orden de carga. */
export interface Pendiente {
  clave?: number;
  login: string;
  gasto: GastoPendienteDatos;
  estado: 'pendiente' | 'rechazado';
  motivo?: string;
  creado: string;
}

interface TrackerOfflineDB extends DBSchema {
  pendientes: { key: number; value: Pendiente; indexes: { login: string } };
}

const { pendientes: STORE } = OFFLINE_DB.stores;

let conexion: Promise<IDBPDatabase<TrackerOfflineDB>> | null = null;

/** Abre la base del contrato (spec 003 §5.1) creando el store y el índice según `offline-db.config.ts`. */
const base = (): Promise<IDBPDatabase<TrackerOfflineDB>> => {
  conexion ??= openDB<TrackerOfflineDB>(OFFLINE_DB.nombre, OFFLINE_DB.version, {
    upgrade(db) {
      if (!db.objectStoreNames.contains(STORE.nombre)) {
        const store = db.createObjectStore(STORE.nombre, { keyPath: STORE.clave, autoIncrement: STORE.autoIncremental });
        store.createIndex(STORE.indices.login, STORE.indices.login);
      }
    },
  });
  return conexion;
};

/** Pendientes del usuario, en el orden en que se cargaron (punto 9). Nunca devuelve los de otro usuario (punto 15). */
export const listarPendientes = async (login: string): Promise<Pendiente[]> => {
  const todos = await (await base()).getAllFromIndex(STORE.nombre, STORE.indices.login, login);
  return todos.sort((a, b) => (a.clave ?? 0) - (b.clave ?? 0));
};

export const contarPendientes = async (login: string): Promise<number> =>
  (await base()).countFromIndex(STORE.nombre, STORE.indices.login, login);

export const obtenerPendiente = async (clave: number, login: string): Promise<Pendiente | undefined> => {
  const pendiente = await (await base()).get(STORE.nombre, clave);
  return pendiente?.login === login ? pendiente : undefined;
};

export const agregarPendiente = async (login: string, gasto: GastoPendienteDatos): Promise<number> => {
  const clave = await (await base()).add(STORE.nombre, { login, gasto, estado: 'pendiente', creado: new Date().toISOString() });
  avisarCambioPendientes();
  return clave;
};

export const actualizarPendiente = async (pendiente: Pendiente): Promise<void> => {
  await (await base()).put(STORE.nombre, pendiente);
  avisarCambioPendientes();
};

export const borrarPendiente = async (clave: number): Promise<void> => {
  await (await base()).delete(STORE.nombre, clave);
  avisarCambioPendientes();
};

/** Punto 14: borra los pendientes del usuario que cierra sesión. */
export const borrarPendientesDe = async (login: string): Promise<void> => {
  const db = await base();
  const tx = db.transaction(STORE.nombre, 'readwrite');
  for (const clave of await tx.store.index(STORE.indices.login).getAllKeys(login)) {
    await tx.store.delete(clave);
  }
  await tx.done;
  avisarCambioPendientes();
};

/** Solo para tests: cierra la conexión para poder borrar la base entre casos. */
export const cerrarBaseParaTests = async (): Promise<void> => {
  if (conexion) (await conexion).close();
  conexion = null;
};
