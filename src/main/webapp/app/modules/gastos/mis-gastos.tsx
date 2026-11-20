import React, { useCallback, useEffect, useState } from 'react';
import { Button, Col, Form, Row, Table } from 'react-bootstrap';
import { useNavigate } from 'react-router';

import { ICategoria } from 'app/shared/model/categoria.model';

import { FiltroGastos, GastoApi, TAMANIO_PAGINA, borrarGasto, listarCategorias, listarGastos } from './gastos.api';
import { formatearFecha, formatearMonto } from './gastos.reglas';
import GastoBorrarDialog from './gasto-borrar-dialog';
import { TEXTOS } from './textos';

/**
 * "Mis gastos" (spec 001, reglas 4, 5 y 14): listado del usuario autenticado con filtros por fecha y categoría.
 * El backend decide qué gastos son del usuario y en qué orden van (S1, S2): acá solo se muestran.
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

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const resultado = await listarGastos(filtro, pagina);
      setGastos(resultado.gastos);
      setTotal(resultado.total);
    } finally {
      setCargando(false);
    }
  }, [filtro, pagina]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  useEffect(() => {
    void listarCategorias().then(setCategorias);
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

      {!cargando && gastos.length === 0 ? (
        <p>{TEXTOS.sinGastos}</p>
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
            {gastos.map(gasto => (
              <tr key={gasto.id}>
                <td>{formatearFecha(gasto.fecha)}</td>
                <td>{gasto.categoria?.nombre}</td>
                <td>{gasto.descripcion}</td>
                <td className="text-end">{formatearMonto(gasto.monto)}</td>
                <td className="text-end">
                  <Button size="sm" variant="primary" className="me-2" onClick={() => navigate(`/mis-gastos/${gasto.id}/editar`)}>
                    {TEXTOS.editar}
                  </Button>
                  <Button size="sm" variant="danger" onClick={() => setABorrar(gasto)}>
                    {TEXTOS.borrar}
                  </Button>
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
