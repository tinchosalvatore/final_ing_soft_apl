import axios from 'axios';

import { ICategoria } from 'app/shared/model/categoria.model';

/** Gasto tal como lo devuelve /api/gastos (fecha AAAA-MM-DD, monto numérico). */
export interface GastoApi {
  id?: number;
  monto: number | string;
  fecha: string;
  descripcion?: string | null;
  categoria: { id: number; nombre?: string };
}

export interface FiltroGastos {
  desde?: string;
  hasta?: string;
  categoriaId?: string;
}

export const TAMANIO_PAGINA = 20;

/** Listado del usuario autenticado. El orden lo impone el backend (fecha desc, id desc): no se manda `sort`. */
export const listarGastos = async (filtro: FiltroGastos, pagina: number) => {
  const params: Record<string, string | number> = { page: pagina, size: TAMANIO_PAGINA };
  if (filtro.desde) params['fecha.greaterThanOrEqual'] = filtro.desde;
  if (filtro.hasta) params['fecha.lessThanOrEqual'] = filtro.hasta;
  if (filtro.categoriaId) params['categoriaId.equals'] = filtro.categoriaId;
  const respuesta = await axios.get<GastoApi[]>('api/gastos', { params });
  return { gastos: respuesta.data, total: Number(respuesta.headers['x-total-count'] ?? respuesta.data.length) };
};

export const obtenerGasto = async (id: number | string) => (await axios.get<GastoApi>(`api/gastos/${id}`)).data;

export const guardarGasto = async (gasto: GastoApi) =>
  gasto.id ? (await axios.put<GastoApi>(`api/gastos/${gasto.id}`, gasto)).data : (await axios.post<GastoApi>('api/gastos', gasto)).data;

export const borrarGasto = async (id: number) => {
  await axios.delete(`api/gastos/${id}`);
};

export const listarCategorias = async () => (await axios.get<ICategoria[]>('api/categorias')).data;
