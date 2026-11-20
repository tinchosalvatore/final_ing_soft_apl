import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import Gasto from './gasto';
import GastoDeleteDialog from './gasto-delete-dialog';
import GastoDetail from './gasto-detail';
import GastoUpdate from './gasto-update';

const GastoRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<Gasto />} />
    <Route path="new" element={<GastoUpdate />} />
    <Route path=":id">
      <Route index element={<GastoDetail />} />
      <Route path="edit" element={<GastoUpdate />} />
      <Route path="delete" element={<GastoDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default GastoRoutes;
