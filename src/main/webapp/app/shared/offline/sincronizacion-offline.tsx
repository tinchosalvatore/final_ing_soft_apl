import { useEffect } from 'react';

import { alIniciarSesion } from './limpieza';
import { sincronizarPendientes } from './sincronizacion';
import { useLoginActual } from './use-pendientes';

/**
 * Punto 9: con la sesión iniciada, sincroniza al abrir la app y cada vez que vuelve la conexión.
 * No dibuja nada; va una sola vez en `app.tsx`.
 */
export const SincronizacionOffline = () => {
  const login = useLoginActual();
  useEffect(() => {
    if (!login) return undefined;
    void alIniciarSesion(login).then(() => sincronizarPendientes(login));
    const alVolverLaRed = () => void sincronizarPendientes(login);
    globalThis.addEventListener('online', alVolverLaRed);
    return () => globalThis.removeEventListener('online', alVolverLaRed);
  }, [login]);
  return null;
};

export default SincronizacionOffline;
