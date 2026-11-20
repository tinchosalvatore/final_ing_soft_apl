import React, { useEffect, useState } from 'react';
import { Button } from 'react-bootstrap';
import { useNavigate } from 'react-router';

import { useAppSelector } from 'app/config/store';
import Logout from 'app/modules/login/logout';

import { limpiarAlCerrarSesion } from './limpieza';
import { contarPendientes } from './pendientes.repositorio';
import { TEXTOS_OFFLINE as T } from './textos';

type Paso = 'contando' | 'avisando' | 'limpiando' | 'listo';

/**
 * Punto 14: "Cerrar la sesión" pasa por acá antes del logout generado (que no se modifica).
 * Si hay pendientes avisa y espera la confirmación; después borra la caché `api-v1` y los pendientes del usuario.
 */
export const CerrarSesion = () => {
  const navigate = useNavigate();
  const login: string | undefined = useAppSelector(state => state.authentication.account?.login);
  const [paso, setPaso] = useState<Paso>('contando');
  const [cantidad, setCantidad] = useState(0);

  useEffect(() => {
    if (!login) {
      setPaso('limpiando');
      return;
    }
    contarPendientes(login)
      .catch(() => 0)
      .then(n => {
        setCantidad(n);
        setPaso(n > 0 ? 'avisando' : 'limpiando');
      });
  }, [login]);

  useEffect(() => {
    if (paso !== 'limpiando') return;
    limpiarAlCerrarSesion(login)
      .catch(() => undefined)
      .finally(() => setPaso('listo'));
  }, [paso]);

  if (paso === 'listo') return <Logout />;
  if (paso !== 'avisando') return null;
  return (
    <div className="p-3">
      <h4>{T.cerrarTitulo}</h4>
      <p>{T.avisoLogout(cantidad)}</p>
      <Button variant="danger" className="me-2" onClick={() => setPaso('limpiando')}>
        {T.cerrarIgual}
      </Button>
      <Button variant="secondary" onClick={() => navigate(-1)}>
        {T.cancelar}
      </Button>
    </div>
  );
};

export default CerrarSesion;
