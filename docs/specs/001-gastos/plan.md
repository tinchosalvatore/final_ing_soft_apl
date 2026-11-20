# Plan — Spec 001 (gastos y categorías)

- Autor: agente coder (sesión `coder`, 2026-11-20)
- Estado: Aprobado (decisiones del documentador, con delegación del humano, 2026-11-20)

<!-- Lo escribe el implementador ANTES de codear. No se codea hasta que el humano lo apruebe. -->

## 1. Resumen de la solución

Las reglas de la spec van en la capa `service`, que se vuelve dueña del control de acceso:

- **`GastoService`** resuelve el usuario autenticado y opera siempre sobre "id **y** dueño = yo" (S1). Al crear o editar ignora el `user` del DTO (S3) y valida monto, fecha y categoría (S4).
- **`GastoQueryService`** pisa cualquier criterio de usuario con "dueño = yo" e impone el orden fecha desc, id desc (S2).
- **`CategoriaService`** controla los nombres únicos (normalizados) y el borrado de categorías en uso. **`CategoriaResource`** exige `ROLE_ADMIN` para escribir (S10).
- **`UserService.registerUser`** rechaza el registro si `application.registro.habilitado=false` (regla 16).

Los resources quedan finos: traducen "no es mío o no existe" a **404**, el mismo en los dos casos.

Para la UI, se arma un módulo propio "Mis gastos" en territorio del desarrollador, con los textos exactos de §5.1. No se editan los CRUD generados (ADR-0008).

## 2. Archivos que se tocan

### Backend
| Archivo | Territorio | Qué cambia |
|---|---|---|
| `service/GastoService.java` | Desarrollador (ADR-0007) | Reescrito: `save` / `update` / `partialUpdate` / `findOne` / `delete` filtran por dueño = usuario actual (`SecurityUtils.getCurrentUserLogin()`). El dueño lo asigna el servidor. Validación de monto, fecha y categoría. Devuelve `Optional` vacío o `false` cuando el gasto no es propio o no existe |
| `service/GastoQueryService.java` | Desarrollador (ADR-0007) | `createSpecification`: `criteria.setUserId(null)` + `user.login = actual` siempre. `findByCriteria` reemplaza el `Sort` del cliente por `fecha DESC, id DESC`. `countByCriteria` usa la misma especificación |
| `service/CategoriaService.java` | Desarrollador | Normaliza el nombre (`strip()`), valida largo 2-40 sobre el nombre normalizado, rechaza duplicados (`lower(trim(nombre))`, excluyendo el propio id al renombrar) y rechaza borrar si hay gastos con esa categoría |
| `service/UserService.java` | Compartido (generado, sin entidad) | Primera línea de `registerUser`: si el registro está deshabilitado, lanza `RegistroDeshabilitadoException` **antes** de `removeNonActivatedUser` |
| `service/CampoInvalidoException.java` (nueva) | Desarrollador | Error de validación de un campo (`entidad`, `campo`, `mensaje`) que se valida en el service |
| `service/ReglaDeNegocioException.java` (nueva) | Desarrollador | Error de negocio con clave (`nombreduplicado`, `categoriaenuso`) |
| `service/RegistroDeshabilitadoException.java` (nueva) | Desarrollador | `@ResponseStatus(UNAUTHORIZED)` |
| `web/rest/errors/ExceptionTranslator.java` | Compartido | `CampoInvalidoException` → 400 con `fieldErrors` (mismo formato que Bean Validation: `objectName`, `field`, `message`). `ReglaDeNegocioException` → 400 con `message` = `error.<clave>`. Ningún cambio al resto |
| `web/rest/GastoResource.java` | Desarrollador (ADR-0007) | Sin `GastoRepository`: todo pasa por el service. PUT / PATCH / DELETE / GET de un gasto no propio o inexistente → 404 (`ResponseStatusException(NOT_FOUND)`, el mismo cuerpo en los dos casos) |
| `web/rest/CategoriaResource.java` | Desarrollador (ADR-0007) | `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` en POST, PUT, PATCH y DELETE |
| `config/ApplicationProperties.java` | Compartido | `registro.habilitado` (default `true`), arriba del needle. El service la lee con `@Value`, porque ArchUnit no deja que `service` acceda a `config` |
| `repository/CategoriaRepository.java` | Generador → `i` (ADR-0008) | Solo la consulta normalizada del nombre (JPQL `lower(trim(...))`) |
| `config/application-prod.yml` + `application-secret-samples.yml` | Compartido | `application.registro.habilitado: ${samples.registro-habilitado:false}`; `samples.registro-habilitado: true` en el perfil de muestras |
| `config/liquibase/changelog/<fecha>_categorias_iniciales.xml` (nuevo) + `master.xml` | Compartido | Inserta las 6 categorías **sin contexto** (corre en dev, test y prod). Ids con `nextval('sequence_generator')`, para no chocar con los ids 1-10 del `faker` de dev |
| `GastoResourceIT.java`, `CategoriaResourceIT.java` (tests generados) | Generador → los ajusta el coder | Se adaptan a las reglas: gastos con dueño, 404 en vez de 400 `idnotfound`, escritura de categorías como ADMIN y nombres únicos. Se anota cada cambio en `tasks.md` |

### Frontend (módulo propio, territorio del desarrollador)
| Archivo | Qué |
|---|---|
| `app/modules/gastos/mis-gastos.tsx` | Listado con filtros desde/hasta y categoría, paginado. Botón "Nuevo gasto"; por fila, "Editar" y "Borrar". Montos con `Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' })` → `$ 1.500,50` |
| `app/modules/gastos/gasto-form.tsx` | Formulario con labels "Monto", "Fecha", "Categoría", "Descripción" y botones "Guardar" y "Cancelar". Valida lo mismo que el backend, para la experiencia de uso. Muestra junto al campo los `fieldErrors` que devuelve el backend, sin perder lo escrito (regla 15) |
| `app/modules/gastos/gasto-borrar-dialog.tsx` | Diálogo (`role="dialog"`) con "Confirmar" y "Cancelar" |
| `app/modules/gastos/gastos.reducer.ts` (o reutilizar `entities/gasto/gasto.reducer.ts`) | Llamadas a `/api/gastos` y `/api/categorias` |
| `app/routes.tsx`, `shared/layout/header/header.tsx`, `shared/layout/menus/*` | Compartido: ruta `/mis-gastos`, ítem de menú "Mis gastos" y "Categorías" (solo ADMIN). El menú "Entidades" generado queda solo para ADMIN (ADR-0008) |
| `i18n/es/misGastos.json` (nuevo) | Textos del módulo. Como es un archivo propio, una regeneración no lo pisa |
| Tests Vitest del módulo | Formato de montos, validaciones del formulario y mapeo de los `fieldErrors` |

## 3. Dónde vive cada regla de negocio

| Regla de la spec | Clase / método | Defensa |
|---|---|---|
| 1, 3: dueño = quien registra; se ignora el del DTO (CA01, CA04) | `GastoService.save` | S3 |
| 2: monto > 0, ≤ 10.000.000, ≤ 2 decimales (CA02) | `GastoService.validar` → `CampoInvalidoException("monto")` | S4 |
| 2: fecha ≤ hoy en `America/Argentina/Buenos_Aires` (CA03) | `GastoService.validar` (con `Clock` inyectable, para testear) → `CampoInvalidoException("fecha")` | S4 |
| 2: categoría existente | `GastoService.validar` → `CampoInvalidoException("categoria")` | S4 |
| 4: orden fecha desc, id desc, sin importar el `sort` del cliente (CA09) | `GastoQueryService.findByCriteria` | — |
| 5: filtro y `/count` solo propios (CA05, CA06, CA09) | `GastoQueryService.createSpecification` | S2 |
| 6, 7: ver, editar y borrar solo propios; ajeno = inexistente → 404 (CA07, CA08) | `GastoService.findOne / update / partialUpdate / delete` + `GastoResource` | S1 |
| 6: al editar no cambia el dueño (CA04) | `GastoService.update / partialUpdate` (no copian `user`) | S3 |
| 8: sin sesión → 401 (CA10) | `SecurityConfiguration` generado (sin cambios) | — |
| 10: solo ADMIN escribe categorías → 403 (CA11) | `CategoriaResource` `@PreAuthorize` | S10 |
| 11: nombre 2-40 y único sin distinguir mayúsculas ni espacios (CA12) | `CategoriaService.save / update / partialUpdate` | S4 |
| 12: no borrar categoría en uso (CA13) | `CategoriaService.delete` | — |
| 13: categorías iniciales (CA14) | Changelog de Liquibase sin contexto | XII |
| 16: sin registro público en prod (CA16) | `UserService.registerUser` + `application.registro.habilitado` | A07 / R1 |

## 4. Tecnologías y dependencias nuevas

Ninguna. El formato de montos usa `Intl.NumberFormat`, que es nativo.

## 5. Decisiones tomadas por el agente

1. **Las validaciones de monto y fecha van en el service, no con Bean Validation en el DTO.** El DTO es territorio del generador, y la regla de "hoy" necesita la zona de Buenos Aires. Para que el contrato §5.1 (`fieldErrors[].field`) se cumpla igual, el `ExceptionTranslator` traduce `CampoInvalidoException` con el mismo formato que un error de Bean Validation.
2. **No encontrado = `ResponseStatusException(NOT_FOUND)`**, igual que `ResponseUtil.wrapOrNotFound` del GET generado, para que el cuerpo sea idéntico en todos los casos (CA07).
3. **PATCH sin el mapper.** `GastoMapper.partialUpdate` copiaría `user` y aplicaría el patch sobre la categoría *gestionada* (le cambiaría el id). Por eso los campos se copian a mano en el service.
4. **Las categorías iniciales usan `nextval('sequence_generator')`**, para no chocar con los datos `faker` de dev (ids 1-10). Los ids quedan altos (1050, 1100, …), pero eso no se ve.
5. **Registro deshabilitado en `UserService`**, antes de cualquier acceso a la tabla de usuarios. Sin esto, `removeNonActivatedUser` borra la cuenta desactivada (lo que prueba CA16).

## 6. Decisiones (resueltas por el documentador, 2026-11-20)

1. **Nombre de categoría:** se guarda con `strip()` y se respetan las mayúsculas (spec, regla 11).
2. **`user` en la respuesta:** queda `{ id, login }`, sin email ni nombre (spec §5.1). No se toca `GastoMapper`.
3. **UI:** un módulo propio en `app/modules/gastos/`, y el menú "Entidades" solo para ADMIN. Queda en el **ADR-0008**.
4. **Repositorios:** se resuelve con `Specification` y métodos derivados desde el service. A `CategoriaRepository` solo se le agrega la consulta normalizada del nombre, y el archivo queda marcado `i` (ADR-0008).
5. **Registro:**
   - Prod real: `registro.habilitado=false`.
   - Perfil `secret-samples` (CI y demo): `true`, con el patrón `${...:${samples.x}}` del paso 000 (spec, regla 16).
   - El helper de testing no cambia.
6. **Logs:** en los archivos que reescribo, solo ids. El resto lo hace la spec 004.

## 7. Riesgos

- **Regenerar el código** pisaría `GastoService`, `GastoQueryService`, `GastoResource` y `CategoriaResource`. Mitigación: ADR-0007 y ADR-0008 (`n`/`i` al regenerar) y los tests del agente testing, que fallan si se pierde una regla.
- **Huso horario:** "hoy" se calcula con `America/Argentina/Buenos_Aires`, no con la zona del servidor. Si un servidor en UTC usara su propia zona, entre las 21:00 y las 24:00 de Argentina aceptaría gastos con fecha de "mañana" en Argentina.
- **E2E 001-CA15:** el test escrito a mano supone `getByLabel` y `role="dialog"`. Si algún detalle de la UI no coincide, aviso al documentador y al agente testing; no toco el test.
