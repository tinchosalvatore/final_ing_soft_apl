import React, { useCallback, useEffect, useState } from 'react';
import { Alert, Button, Col, Form, Row, Table } from 'react-bootstrap';
import { useNavigate } from 'react-router';

import { ICategoria } from 'app/shared/model/categoria.model';
import { cargarConCache, useEnLinea } from 'app/shared/offline/conexion';
import { alCambiarPendientes } from 'app/shared/offline/eventos';
import { borrarPendiente } from 'app/shared/offline/pendientes.repositorio';
import { TEXTOS_OFFLINE } from 'app/shared/offline/textos';
import { usePendientes } from 'app/shared/offline/use-pendientes';

import { FiltroGastos, GastoApi, TAMANIO_PAGINA, borrarGasto, listarCategorias, listarGastos } from './gastos.api';
import { formatearFecha, formatearMonto } from './gastos.reglas';
import GastoBorrarDialog from './gasto-borrar-dialog';
import { TEXTOS } from './textos';

/**
 * "Mis gastos" (spec 001, reglas 4, 5 y 14): listado del usuario autenticado con filtros por fecha y categoría.
 * El backend decide qué gastos son del usuario y en qué orden van (S1, S2): acá solo se muestran.
 * Spec 003: sin conexión muestra los datos guardados con el aviso (puntos 4 y 5), los pendientes arriba (puntos 8 y 11)
 * y deshabilita editar y borrar (punto 13).
 */
export const MisGastos = () => {
  const navigate = useNavigate();
  const [gastos, setGastos] = useState<GastoApi[]>([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [filtro, setFiltro] = useState<FiltroGastos>({});
  const [categorias, setCategorias] = useState<ICategoria[]>([]);
  const [aBorrar, setABorrar] = useState<GastoApi | null>(null);
  const [cargando, setCargando] = useState(true);
  const [guardados, setGuardados] = useState(false);
  const [noDisponible, setNoDisponible] = useState(false);
  const enLinea = useEnLinea();
  const pendientes = usePendientes();

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const resultado = await cargarConCache(() => listarGastos(filtro, pagina));
      setNoDisponible(resultado.estado === 'no-disponible');
      if (resultado.estado === 'ok') {
        setGastos(resultado.datos.gastos);
        setTotal(resultado.datos.total);
        setGuardados(resultado.guardados);
      }
    } finally {
      setCargando(false);
    }
  }, [filtro, pagina]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  // Cuando se sincroniza un pendiente, el listado se vuelve a pedir para que aparezca como gasto normal (punto 10).
  useEffect(() => alCambiarPendientes(() => void cargar()), [cargar]);

  useEffect(() => {
    listarCategorias()
      .then(setCategorias)
      .catch(() => setCategorias([]));
  }, []);

  const cambiarFiltro = (cambio: FiltroGastos) => {
    setPagina(0);
    setFiltro(actual => ({ ...actual, ...cambio }));
  };

  const confirmarBorrado = async () => {
    if (aBorrar?.id) {
      await borrarGasto(aBorrar.id);
    }
    setABorrar(null);
    await cargar();
  };

  const ultimaPagina = Math.max(0, Math.ceil(total / TAMANIO_PAGINA) - 1);

  return (
    <div>
      <h2 className="d-flex justify-content-between align-items-center">
        {TEXTOS.titulo}
        <Button variant="primary" onClick={() => navigate('/mis-gastos/nuevo')}>
          {TEXTOS.nuevo}
        </Button>
      </h2>

      <Form className="mb-3">
        <Row>
          <Col md={3}>
            <Form.Group controlId="filtro-desde">
              <Form.Label>{TEXTOS.desde}</Form.Label>
              <Form.Control type="date" value={filtro.desde ?? ''} onChange={e => cambiarFiltro({ desde: e.target.value })} />
            </Form.Group>
          </Col>
          <Col md={3}>
            <Form.Group controlId="filtro-hasta">
              <Form.Label>{TEXTOS.hasta}</Form.Label>
              <Form.Control type="date" value={filtro.hasta ?? ''} onChange={e => cambiarFiltro({ hasta: e.target.value })} />
            </Form.Group>
          </Col>
          <Col md={4}>
            <Form.Group controlId="filtro-categoria">
              <Form.Label>{TEXTOS.filtroCategoria}</Form.Label>
              <Form.Select value={filtro.categoriaId ?? ''} onChange={e => cambiarFiltro({ categoriaId: e.target.value })}>
                <option value="">{TEXTOS.todas}</option>
                {categorias.map(c => (
                  <option key={c.id} value={c.id}>
                    {c.nombre}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
          </Col>
        </Row>
      </Form>

      {guardados && !noDisponible && <Alert variant="warning">{TEXTOS_OFFLINE.datosGuardados}</Alert>}
      {noDisponible && <Alert variant="secondary">{TEXTOS_OFFLINE.noDisponible}</Alert>}

      {!cargando && gastos.length === 0 && pendientes.length === 0 ? (
        !noDisponible && <p>{TEXTOS.sinGastos}</p>
      ) : (
        <Table responsive striped>
          <thead>
            <tr>
              <th>{TEXTOS.fecha}</th>
              <th>{TEXTOS.categoria}</th>
              <th>{TEXTOS.descripcion}</th>
              <th className="text-end">{TEXTOS.monto}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {pendientes.map(p => (
              <tr key={`pendiente-${p.clave}`} className="table-warning">
                <td>{formatearFecha(p.gasto.fecha)}</td>
                <td>{p.gasto.categoria.nombre}</td>
                <td>{p.gasto.descripcion}</td>
                <td className="text-end">{formatearMonto(p.gasto.monto)}</td>
                <td className="text-end">
                  {p.estado === 'pendiente' ? (
                    <span className="badge text-bg-warning">{TEXTOS_OFFLINE.pendiente}</span>
                  ) : (
                    <>
                      <div className="text-danger small mb-1">{TEXTOS_OFFLINE.rechazado(p.motivo ?? TEXTOS_OFFLINE.motivoDesconocido)}</div>
                      <Button
                        size="sm"
                        variant="primary"
                        className="me-2"
                        onClick={() => navigate(`/mis-gastos/pendiente/${p.clave}/corregir`)}
                      >
                        {TEXTOS_OFFLINE.reintentar}
                      </Button>
                      <Button size="sm" variant="outline-danger" onClick={() => void borrarPendiente(p.clave!)}>
                        {TEXTOS_OFFLINE.descartar}
                      </Button>
                    </>
                  )}
                </td>
              </tr>
            ))}
            {gastos.map(gasto => (
              <tr key={gasto.id}>
                <td>{formatearFecha(gasto.fecha)}</td>
                <td>{gasto.categoria?.nombre}</td>
                <td>{gasto.descripcion}</td>
                <td className="text-end">{formatearMonto(gasto.monto)}</td>
                <td className="text-end">
                  <Button
                    size="sm"
                    variant="primary"
                    className="me-2"
                    disabled={!enLinea}
                    title={enLinea ? undefined : TEXTOS_OFFLINE.noDisponibleSinConexion}
                    onClick={() => navigate(`/mis-gastos/${gasto.id}/editar`)}
                  >
                    {TEXTOS.editar}
                  </Button>
                  <Button
                    size="sm"
                    variant="danger"
                    disabled={!enLinea}
                    title={enLinea ? undefined : TEXTOS_OFFLINE.noDisponibleSinConexion}
                    onClick={() => setABorrar(gasto)}
                  >
                    {TEXTOS.borrar}
                  </Button>
                  {!enLinea && <div className="text-muted small">{TEXTOS_OFFLINE.noDisponibleSinConexion}</div>}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}

      {total > TAMANIO_PAGINA && (
        <div className="d-flex justify-content-center gap-2">
          <Button variant="outline-secondary" disabled={pagina === 0} onClick={() => setPagina(p => p - 1)}>
            {TEXTOS.anterior}
          </Button>
          <Button variant="outline-secondary" disabled={pagina >= ultimaPagina} onClick={() => setPagina(p => p + 1)}>
            {TEXTOS.siguiente}
          </Button>
        </div>
      )}

      <GastoBorrarDialog abierto={aBorrar !== null} onConfirmar={() => void confirmarBorrado()} onCancelar={() => setABorrar(null)} />
    </div>
  );
};

export default MisGastos;
