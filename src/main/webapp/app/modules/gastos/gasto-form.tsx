import React, { useEffect, useState } from 'react';
import { Alert, Button, Form } from 'react-bootstrap';
import { useNavigate, useParams } from 'react-router';

import axios from 'axios';

import { ICategoria } from 'app/shared/model/categoria.model';

import { guardarGasto, listarCategorias, obtenerGasto } from './gastos.api';
import { ErroresGasto, ValoresGasto, erroresDelBackend, hoyArgentina, normalizarMonto, validarGasto } from './gastos.reglas';
import { TEXTOS } from './textos';

const VACIO: ValoresGasto = { monto: '', fecha: '', categoriaId: '', descripcion: '' };

/**
 * Alta y edición de un gasto (spec 001, reglas 1, 2, 6 y 15). Valida lo mismo que el backend para la experiencia
 * de uso; si el backend rechaza algo, muestra su mensaje junto al campo sin perder lo escrito.
 */
export const GastoForm = () => {
  const navigate = useNavigate();
  const { id } = useParams<'id'>();
  const esEdicion = id !== undefined;

  const [valores, setValores] = useState<ValoresGasto>({ ...VACIO, fecha: hoyArgentina() });
  const [errores, setErrores] = useState<ErroresGasto>({});
  const [errorGeneral, setErrorGeneral] = useState<string | null>(null);
  const [categorias, setCategorias] = useState<ICategoria[]>([]);
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    void listarCategorias().then(setCategorias);
  }, []);

  useEffect(() => {
    if (!esEdicion) return;
    obtenerGasto(id)
      .then(gasto =>
        setValores({
          monto: String(gasto.monto),
          fecha: gasto.fecha,
          categoriaId: String(gasto.categoria?.id ?? ''),
          descripcion: gasto.descripcion ?? '',
        }),
      )
      .catch(() => setErrorGeneral(TEXTOS.noEncontrado));
  }, [id, esEdicion]);

  const cambiar = (campo: keyof ValoresGasto) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setValores(actual => ({ ...actual, [campo]: e.target.value }));
  };

  const guardar = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorGeneral(null);
    const erroresLocales = validarGasto(valores);
    setErrores(erroresLocales);
    if (Object.keys(erroresLocales).length > 0) return;

    setGuardando(true);
    try {
      await guardarGasto({
        id: esEdicion ? Number(id) : undefined,
        monto: normalizarMonto(valores.monto) ?? valores.monto,
        fecha: valores.fecha,
        descripcion: valores.descripcion.trim() === '' ? null : valores.descripcion,
        categoria: { id: Number(valores.categoriaId) },
      });
      navigate('/mis-gastos');
    } catch (error) {
      const problem = axios.isAxiosError(error) ? error.response?.data : undefined;
      const delBackend = erroresDelBackend(problem);
      if (Object.keys(delBackend).length > 0) {
        setErrores(delBackend);
      } else {
        setErrorGeneral(TEXTOS.errorGenerico);
      }
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div className="col-md-8">
      <h2>{esEdicion ? TEXTOS.editarTitulo : TEXTOS.nuevo}</h2>
      {errorGeneral && <Alert variant="danger">{errorGeneral}</Alert>}
      <Form noValidate onSubmit={e => void guardar(e)}>
        <Form.Group className="mb-3" controlId="gasto-monto">
          <Form.Label>{TEXTOS.monto}</Form.Label>
          <Form.Control type="text" inputMode="decimal" value={valores.monto} onChange={cambiar('monto')} isInvalid={!!errores.monto} />
          <Form.Control.Feedback type="invalid">{errores.monto}</Form.Control.Feedback>
        </Form.Group>
        <Form.Group className="mb-3" controlId="gasto-fecha">
          <Form.Label>{TEXTOS.fecha}</Form.Label>
          <Form.Control type="date" max={hoyArgentina()} value={valores.fecha} onChange={cambiar('fecha')} isInvalid={!!errores.fecha} />
          <Form.Control.Feedback type="invalid">{errores.fecha}</Form.Control.Feedback>
        </Form.Group>
        <Form.Group className="mb-3" controlId="gasto-categoria">
          <Form.Label>{TEXTOS.categoria}</Form.Label>
          <Form.Select value={valores.categoriaId} onChange={cambiar('categoriaId')} isInvalid={!!errores.categoria}>
            <option value="">{TEXTOS.elegir}</option>
            {categorias.map(c => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
          </Form.Select>
          <Form.Control.Feedback type="invalid">{errores.categoria}</Form.Control.Feedback>
        </Form.Group>
        <Form.Group className="mb-3" controlId="gasto-descripcion">
          <Form.Label>{TEXTOS.descripcion}</Form.Label>
          <Form.Control
            type="text"
            maxLength={140}
            value={valores.descripcion}
            onChange={cambiar('descripcion')}
            isInvalid={!!errores.descripcion}
          />
          <Form.Control.Feedback type="invalid">{errores.descripcion}</Form.Control.Feedback>
        </Form.Group>
        <Button variant="secondary" className="me-2" onClick={() => navigate('/mis-gastos')}>
          {TEXTOS.cancelar}
        </Button>
        <Button variant="primary" type="submit" disabled={guardando}>
          {TEXTOS.guardar}
        </Button>
      </Form>
    </div>
  );
};

export default GastoForm;
