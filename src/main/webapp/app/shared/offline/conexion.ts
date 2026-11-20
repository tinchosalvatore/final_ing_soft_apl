import { useEffect, useState } from 'react';

import axios from 'axios';

/**
 * Detección de "sin conexión" (spec 003, punto 4; guía PWA §2.4).
 *
 * Con NetworkFirst la página recibe un 200 igual si la respuesta vino de la red o de la caché `api-v1`, así que no
 * alcanza con mirar la respuesta. Tampoco alcanza con `navigator.onLine`, que dice "con conexión" con un WiFi sin
 * internet. Por eso, cuando una pantalla carga datos que se pueden haber servido de la caché, se hace una sonda a
 * `/management/health`: el service worker no la intercepta (no está en `runtimeCaching`), así que cualquier respuesta
 * HTTP prueba que hay red y un error de red prueba que no la hay.
 */
const EVENTO = 'tracker:conexion';
const TIEMPO_SONDA_MS = 3000;

let sondaFallo = false;

const avisar = (fallo: boolean): boolean => {
  if (fallo !== sondaFallo) {
    sondaFallo = fallo;
    globalThis.dispatchEvent(new Event(EVENTO));
  }
  return !fallo;
};

/** Un pedido de axios que no llegó a tener respuesta (sin red, servidor caído o sin datos guardados en la caché). */
export const esErrorDeRed = (error: unknown): boolean => axios.isAxiosError(error) && !error.response;

/** `true` si el servidor responde. Actualiza el estado que muestran los botones (punto 13). */
export const hayConexion = async (): Promise<boolean> => {
  if (!navigator.onLine) return avisar(true);
  try {
    await fetch('management/health', { cache: 'no-store', signal: AbortSignal.timeout(TIEMPO_SONDA_MS) });
    return avisar(false);
  } catch {
    return avisar(true);
  }
};

const enLineaAhora = () => navigator.onLine && !sondaFallo;

/** Estado de conexión para la interfaz: se actualiza con los eventos `online`/`offline` del navegador y con la sonda. */
export const useEnLinea = (): boolean => {
  const [enLinea, setEnLinea] = useState(enLineaAhora);
  useEffect(() => {
    const actualizar = () => setEnLinea(enLineaAhora());
    const volvio = () => {
      sondaFallo = false;
      actualizar();
    };
    globalThis.addEventListener('online', volvio);
    globalThis.addEventListener('offline', actualizar);
    globalThis.addEventListener(EVENTO, actualizar);
    return () => {
      globalThis.removeEventListener('online', volvio);
      globalThis.removeEventListener('offline', actualizar);
      globalThis.removeEventListener(EVENTO, actualizar);
    };
  }, []);
  return enLinea;
};

/** Resultado de cargar una pantalla que se puede servir de la caché (puntos 4 a 6). */
export type Carga<T> = { estado: 'ok'; datos: T; guardados: boolean } | { estado: 'no-disponible' } | { estado: 'error'; error: unknown };

/**
 * Carga datos de la API y dice si son los guardados. Si el pedido falla por red es porque la pantalla nunca se vio con
 * conexión (NetworkFirst sin nada en la caché): "no disponible" (punto 5).
 */
export const cargarConCache = async <T>(pedido: () => Promise<T>): Promise<Carga<T>> => {
  try {
    const datos = await pedido();
    return { estado: 'ok', datos, guardados: !(await hayConexion()) };
  } catch (error) {
    if (esErrorDeRed(error)) {
      avisar(true);
      return { estado: 'no-disponible' };
    }
    return { estado: 'error', error };
  }
};
