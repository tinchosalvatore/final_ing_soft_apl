import dayjs from 'dayjs';

import { ICategoria } from 'app/shared/model/categoria.model';
import { IUser } from 'app/shared/model/user.model';

export interface IGasto {
  id?: number;
  monto?: number;
  fecha?: dayjs.Dayjs;
  descripcion?: string | null;
  categoria?: ICategoria;
  user?: IUser | null;
}

export const defaultValue: Readonly<IGasto> = {};
