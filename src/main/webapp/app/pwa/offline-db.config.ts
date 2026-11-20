/**
 * Base offline de la PWA (spec 003 §5.1, ADR-0005): IndexedDB donde la app guarda los gastos cargados sin conexión.
 * La usa `app/shared/offline/pendientes.repositorio.ts` (con la librería `idb`).
 */
export const OFFLINE_DB = {
  nombre: 'tracker-offline',
  // Si cambia la forma de los datos guardados, se sube la versión y se migra en `upgrade` (los pendientes no se pierden).
  version: 1,
  stores: {
    pendientes: {
      nombre: 'pendientes',
      clave: 'clave', // clave autoincremental: da el orden en que se cargaron (punto 9)
      autoIncremental: true,
      indices: {
        login: 'login', // cada pendiente lleva el login de su dueño: solo se muestran y envían los del usuario actual (punto 15)
      },
    },
  },
} as const;
