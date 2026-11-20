# Verificación — Spec 001 (gastos y categorías)

- Verificador: agente seguridad (no participó en la implementación ni en los tests de la 001)
- Rama / PR: [PR #5](https://github.com/tinchosalvatore/final_ing_soft_apl/pull/5), rama `spec/001-gastos`, verificado sobre `7954f2a`
- Fecha: 2026-11-20
- Resultado: **Cumple con observaciones**

<!-- No alcanza con que compile. Se contrasta contra la spec: ¿cada criterio tiene un test que lo representa de verdad? -->

**Resumen.** Los 16 criterios tienen un test que los representa. Las defensas S1, S2, S3, S10 y la regla 16 (R1) resisten todos los ataques que probé contra un jar de prod. Los logs no llevan montos ni descripciones.

Hay una observación **media** (O1): un `PATCH` con una descripción de más de 140 caracteres no lo valida el service. Responde **500** y el cuerpo expone nombres de clases internas. El dato no se guarda, pero rompe la regla 6 de la spec ("al editar se aplican las mismas reglas del punto 2") y la defensa S4.

El primer CI del PR estuvo en rojo (O2), por dos motivos que en local no se veían.

## 1. Criterios de aceptación

Los tests del agente testing están en `src/test/java/com/trackergastos/web/rest/` y en `src/test/javascript/playwright/e2e/negocio/`.

| ID | Test que lo cubre (archivo:nombre) | ¿El test representa el criterio? | Resultado |
|---|---|---|---|
| 001-CA01 | `GastoControlAccesoIT`: `001-CA01 registrar gasto válido…` | sí. Verifica el dueño en la respuesta y en la base, y que aparece primero aunque haya otro gasto con la misma fecha | ✅ |
| 001-CA02 | `GastoValidacionIT`: `001-CA02 monto inválido al crear` y `…al editar` (0, -10, 12.345, 10000000.01), más `bordes válidos` | sí para POST y PUT: comprueba `fieldErrors[].field = monto` y que no se crea ni modifica nada. **PATCH sin test** (ver O3) | ✅ |
| 001-CA03 | `GastoValidacionIT`: `001-CA03 fecha futura…` (crear y editar) | sí: usa "mañana en Buenos Aires", con la misma zona que el service | ✅ |
| 001-CA04 | `GastoControlAccesoIT`: `001-CA04 dueño forzado al crear` y `…al editar` | sí para POST y PUT. El PATCH con `user` lo probé a mano: se ignora (§4, A6). Sin test (O3) | ✅ |
| 001-CA05 | `GastoControlAccesoIT`: `001-CA05 listado aislado` | sí: ids exactos y `X-Total-Count = 3` | ✅ |
| 001-CA06 | `GastoControlAccesoIT`: `001-CA06 filtro no escapa al dueño` (`userId.equals` y `userId.in`) y `…el conteo filtrado tampoco escapa` | sí | ✅ |
| 001-CA07 | `GastoControlAccesoIT`: `001-CA07 ver gasto ajeno (IDOR)…` y el control `ana sí ve su propio gasto` | sí: compara el cuerpo del 404 ajeno con el del inexistente, sin `instance` ni `path`, como fija §5.1 | ✅ |
| 001-CA08 | `GastoControlAccesoIT`: `001-CA08 editar y borrar gasto ajeno…` y `…un id inexistente` | sí: PUT, PATCH y DELETE, y el gasto de `beto` queda intacto campo por campo | ✅ |
| 001-CA09 | `GastoControlAccesoIT`: `001-CA09 filtro por fecha y categoría`, `…orden impuesto`, `…desde posterior a hasta` | sí: rango, categoría, otro usuario, orden con desempate por id, y un `sort` del cliente ignorado | ✅ |
| 001-CA10 | `GastoControlAccesoIT`: `001-CA10 sin sesión…` | sí: las 7 operaciones, incluido `/count` | ✅ |
| 001-CA11 | `CategoriaReglasIT`: `001-CA11 USER intenta crear, renombrar y borrar…` y `USER sí puede listar` | sí: POST, PUT, PATCH y DELETE dan 403 y nada cambia | ✅ |
| 001-CA12 | `CategoriaReglasIT`: `001-CA12 nombre duplicado…` y `…renombrar a un nombre que ya existe` | sí | ✅ |
| 001-CA13 | `CategoriaReglasIT`: `001-CA13 borrar categoría en uso` y el borde `sin gastos se permite` | sí | ✅ |
| 001-CA14 | `CategoriaReglasIT`: `001-CA14 categorías iniciales` | sí. Además, en mi corrida de prod las 6 están en una base vacía (el changelog no tiene contexto) | ✅ |
| 001-CA15 | `e2e/negocio/001-gastos.pw.ts`: `001-CA15 flujo E2E…` | sí, el flujo y los textos son los de §5.1. **Pendiente T6:** está escrito a mano y falta regrabarlo con codegen. Lo regraba el humano en un PR aparte; no bloquea | ⚠️ pendiente T6 |
| 001-CA16 | `RegistroDeshabilitadoIT`: `001-CA16 … cuenta desactivada → 401 y la cuenta queda intacta` y `… login nuevo → 401` | sí: compara login, email, hash, clave de activación y estado, y cuenta los usuarios. El test fuerza la propiedad con `@TestPropertySource`. Que **prod** la deje en `false` lo comprobé aparte (§4, A9) | ✅ |

### Tests en rojo antes de la implementación

Según `tasks.md`, en la fase roja fallaron todos los tests de CA01 a CA14 (26 de 34) y CA16. Los 8 que pasaban sin la lógica son controles y bordes que el código generado ya cumplía, y están documentados como tests de regresión. Revisé esa lista: ninguno es un test "vacío" que pase siempre.

## 2. Casos borde no contemplados

Son propuestas. No corregí nada: lo que haya que cambiar lo deciden el documentador y el coder.

### O1 (media): `PATCH` con una descripción de más de 140 caracteres → 500 con detalles internos

- **Qué pasa:** `GastoService.validar` controla monto, fecha y categoría, pero **no la descripción**. El POST y el PUT la validan con `@Valid` en el DTO. El PATCH no usa `@Valid`, igual que en el código generado, así que el largo lo controla recién Hibernate al hacer el commit:
  ```
  PATCH /api/gastos/1353  {"id":1353,"descripcion":"<141 caracteres>"}
  → 500 {"detail":"Error while committing the transaction [Validation failed for classes [com.trackergastos.domain.Gasto] ...
         ConstraintViolationImpl{... propertyPath=descripcion, rootBeanClass=class com.trackergastos.domain.Gasto ...","title":"Internal Server Error"}
  ```
  Reproducido con el perfil `prod` real (A7). El POST con 141 caracteres da 400 con `fieldErrors[].field = descripcion`, que es lo correcto.
- **Impacto:**
  - La descripción no se guarda.
  - Rompe la regla 6 de la spec ("al editar se aplican las mismas reglas del punto 2") y S4 ("validación de largos en el backend").
  - Expone el paquete y las clases del dominio (S7).
  - La UI no lo dispara, porque edita con PUT, pero la API está expuesta.
- **Propuesta:** que `GastoService.validar` también controle `descripcion ≤ 140` con `CampoInvalidoException("descripcion")`, y que el agente testing sume el caso. Decide el documentador si va antes del merge o en un PR chico.

### O2 (baja, proceso): el primer CI del PR estuvo en rojo y en local no se veía

El run 44784849476 (`b81b8a2`) falló por dos motivos que ya corrigió el coder en `3ab34de` y `466965c`:

1. **`backend:doc:test`:** el javadoc corre sin compilar, y los imports puntuales del metamodelo (`Gasto_`, `Categoria_`, `User_`) en `GastoSpecifications` no existían todavía. Como el javadoc es el primer subpaso, **no corrió ningún test de backend**. Lo reproduje en local (`./mvnw javadoc:javadoc`: los mismos 3 errores).
2. **`.gitignore`:** el patrón generado `*.log*` excluía `gastos.logica.ts` y su test, que nunca se commitearon. En local la app andaba porque los archivos estaban en disco. Ahora se llaman `gastos.reglas.*`. Los copias viejas que quedaban en mi worktree eran idénticas (solo cambiaba el import) y las borré antes de repetir `npm test`. Revisé `git status --ignored src docs` y no hay ningún otro archivo fuente ignorado.

- **Propuesta:**
  - Lo que ya anotó el coder en `tasks.md`: antes de pushear, correr `npm run ci:backend:test` y `npm run ci:frontend:test`.
  - Que el agente ci o el documentador evalúen achicar `*.log*` en el `.gitignore` (territorio compartido) a `*.log` y `*.log.*`. Cualquier archivo con `.log` en el nombre (`*.logger.ts`, `*.logica.ts`) puede volver a caer.

### O3 (baja): faltan tests de PATCH para CA02, CA03 y CA04

Las reglas de edición se prueban con PUT. El código del PATCH es otro método (`partialUpdate`, que copia los campos a mano). Lo probé a mano:
- PATCH con `user = beto` → el dueño sigue siendo el original.
- PATCH con otra categoría y un `nombre` forzado → cambia la categoría, pero el nombre de la categoría no se toca.

No hay test que lo cuide si alguien vuelve a usar `GastoMapper.partialUpdate`. **Propuesta:** que el agente testing sume casos PATCH a CA02, CA03 y CA04 (y a O1, si se corrige).

### O4 (para la spec 004, S7): errores 400 y 500 con detalles internos en prod

Además de O1, el 400 `idinvalid` (id de la URL ≠ id del cuerpo) devuelve en `detail` el texto de una clase Java interna (`ProblemDetailWithCause[type=..., properties='{message=error.idinvalid, params=gasto}']`). Viene del `ExceptionTranslator` generado, en el perfil `prod`. **Propuesta:** sumarlo como ejemplo a 004-CA05.

### O5 (para la spec 004): el `LoggingAspect` de dev loguea los DTO

En el perfil `dev`, `LoggingAspect` (generado) loguea en DEBUG los argumentos de cada método de `service` y `web.rest`, con `GastoDTO.toString()` incluido: monto y descripción. En prod no está activo: en mis corridas de prod no aparece ningún monto ni descripción. **Propuesta:** que la spec 004 decida si se excluyen esos argumentos o se acepta que en dev aparezcan.

### O6 (baja, sin spec): borrar un usuario que tiene gastos

`gasto.user_id` es una FK hacia `jhi_user`. Ninguna spec dice qué pasa si un ADMIN borra una cuenta con gastos: ¿se rechaza, se borran los gastos o falla con un 500? No lo probé, porque no hay un criterio contra el cual contrastarlo. **Propuesta:** que el documentador decida si es un caso borde de la 001 o queda fuera de alcance.

## 3. Revisión del diff (`origin/main...7954f2a`)

| Tema | Resultado |
|---|---|
| **Archivos fuera del plan** | Ninguno relevante. `GastoSpecifications.java` no figura en la tabla del plan §2, pero sí en el ADR-0008 (Alternativas) y en `tasks.md` C3. `textos.ts` reemplaza al `i18n/es/misGastos.json` del plan, y lo justifica el ADR-0008. `gastos.reglas.ts` es la "lógica pura" que el plan nombra como tests Vitest del módulo |
| **Territorio del generador** | Solo `repository/CategoriaRepository.java` (consulta JPQL normalizada), declarado en el ADR-0008 con `i`. No se tocaron `domain/`, `service/dto/`, `service/mapper/`, `.jhipster/` ni `entities/` |
| **Tests generados ajustados** | `GastoResourceIT` (dueño en `createEntity`, 404 en lugar de 400 `idnotfound`, filtro `userId` ignorado, se borran dos métodos de eager load con mocks de un método que ya no existe), `CategoriaResourceIT` (corre como ADMIN), `header.spec.tsx` (USER sin "Entidades") y los E2E `entity/*.pw.ts` (loguean como ADMIN). **Cada ajuste responde a una regla de la spec (§5.1, S2, S10, ADR-0008), no a "que pasen".** El cambio de `getAllGastosByUserIsEqualToSomething` a `…FilterIsIgnored` invierte la expectativa a propósito: es S2 |
| **Tests del agente testing** | No se modificaron: `git log` de esos archivos solo tiene commits `test(001)` |
| **Secretos** | Ninguno. `application-prod.yml` suma `application.registro.habilitado: ${samples.registro-habilitado:false}`, con el mismo patrón del paso 000 |
| **Needles** | `master.xml`: el `include` de las categorías iniciales va entre los needles, sin tocarlos. `ApplicationProperties`: la propiedad va arriba del needle |
| **Logs (regla 6)** | En `GastoService`, `GastoQueryService` y `GastoResource` todos los logs son DEBUG y llevan solo ids o el número de página. `CategoriaService` y `CategoriaResource` loguean el DTO de categoría (nombre), que no es un dato sensible. En prod, con un monto y una descripción marcados (`DATO-SENSIBLE-…`, `777.77`, `555.55`), el log tiene **0 apariciones** (A10) |
| **Frontend** | Sin `dangerouslySetInnerHTML`, `eval` ni `localStorage` en `modules/gastos/`. La ruta `/mis-gastos` exige `ROLE_USER` |
| **Dependencias nuevas** | Ninguna |

### ADR-0008

Es coherente con lo implementado:
- El módulo propio está en `app/modules/gastos/` y no se editó el CRUD generado.
- El menú "Entidades" y "Categorías" quedan solo para ADMIN.
- La tabla de archivos que se modifican a mano coincide con el diff.

Una aclaración: los `i` y `n` del ADR son **instrucciones para quien regenere**, no una marca guardada en el repo. No hay `.yo-resolve`, el archivo de Yeoman donde se puede dejar asentado `skip` por archivo. Es coherente con la regla 3 de `AGENTS.md`: al regenerar se revisa el diff. **Propuesta opcional:** si se quiere que sea automático, el documentador puede evaluar un `.yo-resolve` con los archivos del ADR-0007 y el ADR-0008.

## 4. Evidencia

### CI

- Run [44784849476](https://github.com/tinchosalvatore/final_ing_soft_apl/actions/runs/44784849476) (`b81b8a2`) y el anterior (`cd9cd2e`): **rojos** (O2). No llegó a correr ningún test de backend.
- Run [44785987834](https://github.com/tinchosalvatore/final_ing_soft_apl/actions/runs/44785987834) (`7954f2a`, con el fix): **verde**.
  - Pasan Gitleaks, Semgrep y Trivy.
  - Backend: 58 unitarios y 206 de integración, 0 fallas.
  - Frontend: Vitest, 181/181.
  - E2E: 57 pasan y 1 se omite (`administration.pw.ts › /docs`, el mismo del paso 000). Entre los que pasan está `001-CA15 flujo E2E` (1.9 s).

### Tests en local (worktree `spec/001-gastos`)

- `./mvnw -ntp -Dskip.installnodenpm -Dskip.npm verify` sobre `b81b8a2`: **BUILD SUCCESS**. 58 tests unitarios (con ArchUnit `TechnicalStructureTest`) y 206 de integración, 0 fallas. Incluye `GastoControlAccesoIT` 14/14, `GastoValidacionIT` 13/13, `CategoriaReglasIT` 7/7, `RegistroDeshabilitadoIT` 2/2, `GastoResourceIT` 39/39 y `CategoriaResourceIT` 16/16. Los cambios posteriores (`3ab34de`) solo tocan imports y javadoc.
- `npm test` sobre `7954f2a`, sin los archivos ignorados: 20 archivos, **181/181**.
- `./mvnw javadoc:javadoc` sobre `b81b8a2`: falla con los mismos 3 errores del CI (O2).

### Ataques contra un jar de prod

- Jar armado con `-Pprod`, sin el front.
- Postgres `postgres:18.6` aislado en el puerto 55432.
- Usuarios: `user` (atacante), `beto` (víctima, creado por registro y activado con la clave de la base) y `admin`.
- El contenedor y el jar se borraron al terminar.

| # | Perfil | Ataque | Resultado |
|---|---|---|---|
| A1 | `e2e,secret-samples,prod` | S3: `POST` con `"user":{"id":<beto>,"login":"beto"}` | 201 con `user = {id: 2, login: "user"}`. Se ignora el dueño forzado |
| A2 | ídem | S1: GET, PUT, PATCH y DELETE del gasto de `beto` (id 1351) | 404 en los 4 casos. El cuerpo es igual al de un id inexistente (salvo `instance` y `path`). El gasto de `beto` queda intacto |
| A3 | ídem | S1: PUT a la URL de un gasto propio con el id de un ajeno en el cuerpo | 400 `idinvalid`, sin tocar nada (ver O4) |
| A4 | ídem | S2: `?userId.equals=<beto>`, `?id.equals=1351`, `?id.in=1351,<propio>`, `?descripcion.contains=secreto` | Solo gastos propios o lista vacía. Nunca aparece un gasto de `beto` |
| A5 | ídem | S2: `/api/gastos/count`, con y sin `userId.in=<beto>` y `distinct=true` | `user` → 1 (los suyos), `beto` → 2 |
| A6 | ídem | S3 por PATCH: `user` forzado, y `categoria` con `nombre: "HACK"` | El dueño no cambia, la categoría cambia y su nombre no se toca |
| A7 | `prod` real | S4: PATCH con descripción de 141 caracteres | **500 con detalles internos** (O1). POST con 141 → 400 `fieldErrors.descripcion` |
| A8 | `e2e,secret-samples,prod` | S4: categoría inexistente, monto `99999999999999999999999`, fecha de mañana (Buenos Aires) | 400 con `fieldErrors` sobre `categoria`, `monto` y `fecha`. `12.340` y `"1e3"` se aceptan: son valores válidos, sin redondeo |
| A9 | `prod` real (`registro.habilitado` sin definir → `false`) | R1 / 001-CA16: `POST /api/register` con el login `user` (desactivado por S5) y con un login nuevo | 401 "Registro deshabilitado" en los dos casos. `jhi_user` queda **idéntica** antes y después: id, login, email, activado, hash y clave. Con una contraseña de 3 caracteres responde 400 de validación antes del 401, sin tocar la tabla |
| A10 | `prod` real | Regla 6: PATCH y POST con descripción `DATO-SENSIBLE-…` y montos `777.77` y `-555.55`, y después `grep` en el log | 0 apariciones |
| A11 | `e2e,secret-samples,prod` | S10: USER hace POST, PUT, PATCH y DELETE sobre `/api/categorias` | 403 en los 4. El GET da 200. `/api/admin/users` como USER → 403 |
| A12 | ídem, como ADMIN | CA12 y CA13: `" supermercado "`, `"SUPERMERCADO"`, renombrar a `" ocio"`; borrar Supermercado en uso | 400 `error.nombreduplicado` (3 casos) y 400 `error.categoriaenuso`. `"Super mercado"` → 201 (otro nombre, correcto) |
| A13 | ídem | CA10: sin token y con un token inválido, en `/api/gastos` y `/count` | 401 |
| A14 | `prod` real, base vacía | CA14 | Las 6 categorías existen (ids de `sequence_generator`) |
