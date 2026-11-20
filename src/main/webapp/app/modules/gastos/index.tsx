import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import GastoForm from './gasto-form';
import MisGastos from './mis-gastos';

/** Rutas de "Mis gastos" (módulo propio, territorio del desarrollador: ADR-0008). */
const MisGastosRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<MisGastos />} />
    <Route path="nuevo" element={<GastoForm />} />
    <Route path=":id/editar" element={<GastoForm />} />
  </ErrorBoundaryRoutes>
);

export default MisGastosRoutes;
