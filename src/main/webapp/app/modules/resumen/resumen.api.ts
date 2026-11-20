import axios from 'axios';

export interface CategoriaResumen {
  nombre: string;
  total: number;
  cantidad: number;
  participacion: number;
}

export interface ResumenMensual {
  mes: string;
  total: number;
  cantidad: number;
  categorias: CategoriaResumen[];
  diferencia: number;
  variacionPorcentual?: number | null;
}

/** `GET /api/resumen?mes=AAAA-MM` (spec 002 §5.1). */
export const obtenerResumen = async (mes: string) => (await axios.get<ResumenMensual>('api/resumen', { params: { mes } })).data;
