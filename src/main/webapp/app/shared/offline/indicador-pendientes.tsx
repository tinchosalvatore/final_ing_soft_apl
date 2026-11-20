import React from 'react';
import { Badge } from 'react-bootstrap';

import { TEXTOS_OFFLINE } from './textos';
import { usePendientes } from './use-pendientes';

/** Punto 8: cuántos gastos faltan sincronizar (incluye los rechazados). Con 0 no se muestra. */
export const IndicadorPendientes = () => {
  const cantidad = usePendientes().length;
  if (cantidad === 0) return null;
  return (
    <Badge bg="warning" text="dark" className="ms-2 me-auto align-self-center" role="status">
      {TEXTOS_OFFLINE.indicador(cantidad)}
    </Badge>
  );
};

export default IndicadorPendientes;
