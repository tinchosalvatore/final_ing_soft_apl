import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import Resumen from './resumen';

/** Ruta de la pantalla "Resumen" (spec 002; módulo propio, ADR-0008). */
const ResumenRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<Resumen />} />
  </ErrorBoundaryRoutes>
);

export default ResumenRoutes;
