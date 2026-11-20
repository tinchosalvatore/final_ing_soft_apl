import React, { useEffect, useState } from 'react';
import { Alert, Button, Card, Col, Row, Table } from 'react-bootstrap';

import { formatearMonto, hoyArgentina } from 'app/modules/gastos/gastos.reglas';
import { cargarConCache } from 'app/shared/offline/conexion';
import { TEXTOS_OFFLINE } from 'app/shared/offline/textos';

import { ResumenMensual, obtenerResumen } from './resumen.api';
import {
  PRIMER_MES,
  formatearDiferencia,
  formatearPorcentaje,
  formatearVariacion,
  mesActual,
  moverMes,
  sentidoVariacion,
} from './resumen.reglas';
import { TEXTOS_RESUMEN as T } from './textos';

/**
 * Pantalla "Resumen" (spec 002, punto 7): total del mes, variación contra el anterior y detalle por categoría.
 * El resumen lo calcula el backend con los gastos del usuario autenticado (002-CA08).
 */
export const Resumen = () => {
  const actual = mesActual(hoyArgentina());
  const [mes, setMes] = useState(actual);
  const [resumen, setResumen] = useState<ResumenMensual | null>(null);
  const [error, setError] = useState(false);
  // Spec 003, puntos 4 y 5: datos guardados con aviso, o "no disponible" si este mes nunca se vio con conexión.
  const [guardados, setGuardados] = useState(false);
  const [noDisponible, setNoDisponible] = useState(false);

  useEffect(() => {
    setError(false);
    void cargarConCache(() => obtenerResumen(mes)).then(resultado => {
      setNoDisponible(resultado.estado === 'no-disponible');
      setError(resultado.estado === 'error');
      setGuardados(resultado.estado === 'ok' && resultado.guardados);
      setResumen(resultado.estado === 'ok' ? resultado.datos : null);
    });
  }, [mes]);

  const variacionInformada = resumen?.variacionPorcentual !== null && resumen?.variacionPorcentual !== undefined;

  return (
    <div>
      <h2>{T.titulo}</h2>
      <div className="d-flex align-items-center gap-3 mb-3">
        {/* Spec 002, punto 7: el anterior se deshabilita en el primer mes que se puede pedir (2000-01) */}
        <Button variant="outline-secondary" disabled={mes <= PRIMER_MES} onClick={() => setMes(m => moverMes(m, -1))}>
          {T.mesAnterior}
        </Button>
        <strong aria-live="polite">{mes}</strong>
        {/* Spec 002, punto 7: el siguiente se deshabilita en el mes actual */}
        <Button variant="outline-secondary" disabled={mes >= actual} onClick={() => setMes(m => moverMes(m, 1))}>
          {T.mesSiguiente}
        </Button>
      </div>

      {error && <Alert variant="danger">{T.error}</Alert>}
      {guardados && <Alert variant="warning">{TEXTOS_OFFLINE.datosGuardados}</Alert>}
      {noDisponible && <Alert variant="secondary">{TEXTOS_OFFLINE.noDisponible}</Alert>}

      {resumen && (
        <>
          <Row className="mb-3">
            <Col md={4}>
              <Card body>
                <div className="text-muted">{T.total}</div>
                <div className="fs-3" data-cy="resumen-total">
                  {formatearMonto(resumen.total)}
                </div>
                <div>
                  {T.cantidad}: {resumen.cantidad}
                </div>
              </Card>
            </Col>
            <Col md={8}>
              <Card body>
                <div className="text-muted">{T.variacion}</div>
                {variacionInformada ? (
                  <div className="fs-4">
                    {sentidoVariacion(resumen.diferencia)} {formatearVariacion(resumen.variacionPorcentual ?? 0)}
                  </div>
                ) : (
                  <div className="fs-5">{T.sinDatosMesAnterior}</div>
                )}
                <div>
                  {T.diferencia}: <span>{formatearDiferencia(resumen.diferencia)}</span>
                </div>
              </Card>
            </Col>
          </Row>

          {resumen.categorias.length === 0 ? (
            <p>{T.sinGastos}</p>
          ) : (
            <Table responsive striped>
              <thead>
                <tr>
                  <th>{T.categoria}</th>
                  <th className="text-end">{T.total}</th>
                  <th className="text-end">{T.cantidad}</th>
                  <th className="text-end">{T.participacion}</th>
                </tr>
              </thead>
              <tbody>
                {resumen.categorias.map(fila => (
                  <tr key={fila.nombre}>
                    <td>{fila.nombre}</td>
                    <td className="text-end">{formatearMonto(fila.total)}</td>
                    <td className="text-end">{fila.cantidad}</td>
                    <td className="text-end">{formatearPorcentaje(fila.participacion)}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </>
      )}
    </div>
  );
};

export default Resumen;
