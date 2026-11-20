import { useCallback, useEffect, useState } from 'react';

import { useAppSelector } from 'app/config/store';

import { alCambiarPendientes } from './eventos';
import { type Pendiente, listarPendientes } from './pendientes.repositorio';

/** Login del usuario con sesión, o `null`. */
export const useLoginActual = (): string | null =>
  useAppSelector(state => (state.authentication.isAuthenticated ? (state.authentication.account?.login ?? null) : null));

/** Pendientes del usuario actual (nunca los de otro: punto 15), actualizados cada vez que cambian. */
export const usePendientes = (): Pendiente[] => {
  const login = useLoginActual();
  const [pendientes, setPendientes] = useState<Pendiente[]>([]);
  const leer = useCallback(() => {
    if (!login) {
      setPendientes([]);
      return;
    }
    listarPendientes(login)
      .then(setPendientes)
      .catch(() => setPendientes([]));
  }, [login]);
  useEffect(() => {
    leer();
    return alCambiarPendientes(leer);
  }, [leer]);
  return pendientes;
};
