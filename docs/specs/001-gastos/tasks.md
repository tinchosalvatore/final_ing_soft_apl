# Tareas — Spec 001 (gastos y categorías)

<!-- Lo escribe el implementador a partir del plan aprobado. Es la memoria de qué se hizo y qué no:
     se actualiza al cerrar cada tarea, así una sesión nueva puede retomar sin releer todo. -->

## Fase roja (agente testing, 2026-11-20)

| # | Tarea | Criterios que cubre | Estado | Commit |
|---|---|---|---|---|
| T1 | Contrato observable agregado a la spec (§5.1) por el documentador | todos | hecha | `44624c9` |
| T2 | Integración del control de acceso (Testcontainers) | 001-CA01, CA04 a CA10 | hecha (en rojo) | ver commit `test(001)` |
| T3 | Integración de las validaciones | 001-CA02, CA03 | hecha (en rojo) | ver commit `test(001)` |
| T4 | Integración de las reglas de categorías | 001-CA11 a CA14 | hecha (en rojo) | ver commit `test(001)` |
| T5 | E2E del flujo escrito a mano con los textos de §5.1 | 001-CA15 | hecha (en rojo) | ver commit `test(001)` |
| T6 | **Regrabar el E2E con `npx playwright codegen`** cuando exista "Mis gastos", antes del verify | 001-CA15 | **pendiente** (agente testing) | |

### Contrato para el coder
No hace falta ninguna clase nueva: los tests usan la API que ya existe (`/api/gastos`, `/api/gastos/count`, `/api/categorias`) con los códigos y claves de la spec §5.1. Lo que los tests esperan además:

- **Categorías iniciales en todos los contextos de Liquibase**, también en `test` (001-CA14 consulta la base de los tests sin crear datos).
- `fieldErrors[].field` = `monto` / `fecha` también cuando la regla se valida en el service (CA02, CA03), al crear y al editar.
- `user.login` en la respuesta de crear o editar y `/count` restringido al dueño: ya están en la spec §5.1.
- El filtro `userId.equals` / `userId.in` y el conteo (`/api/gastos/count`) también quedan restringidos al dueño (CA06).
- **Tests generados que van a romperse**: `GastoResourceIT` y `CategoriaResourceIT` de JHipster asumen el CRUD abierto (crean gastos sin dueño, borran categorías como USER, esperan 400 `idnotfound`). Son del generador, no del agente testing: los ajusta el coder al implementar la 001 y lo deja anotado.

### Tests escritos

| Archivo | Test (`@DisplayName`) | Criterio |
|---|---|---|
| `src/test/java/com/trackergastos/web/rest/GastoControlAccesoIT.java` | `001-CA01 registrar gasto válido: se crea con dueño ana y aparece primero en su listado` | CA01 |
| | `001-CA04 dueño forzado al crear: se ignora beto y el dueño es ana` | CA04 |
| | `001-CA04 dueño forzado al editar: se edita pero el dueño sigue siendo ana` | CA04, §4 |
| | `001-CA05 listado aislado: ana ve exactamente sus 3 gastos` | CA05 |
| | `001-CA06 filtro no escapa al dueño: pedir los gastos de beto no devuelve ninguno de beto` | CA06 |
| | `001-CA06 el conteo filtrado tampoco escapa al dueño` | CA06 |
| | `001-CA07 ver gasto ajeno (IDOR): 404 idéntico a pedir un id inexistente` | CA07 |
| | `001-CA07 control: ana sí ve su propio gasto por id y en el listado` | CA07 (control) |
| | `001-CA08 editar y borrar gasto ajeno: 404 en PUT, PATCH y DELETE y el gasto de beto queda intacto` | CA08 |
| | `001-CA08 editar y borrar un id inexistente: 404 en PUT, PATCH y DELETE` | CA08, §5.1 |
| | `001-CA09 filtro por fecha y categoría: solo de ana, del rango y de Transporte, del más reciente al más antiguo` | CA09 |
| | `001-CA09 el orden lo impone el backend aunque el cliente mande otro sort` | CA09, §5.1 |
| | `001-CA09 filtro con desde posterior a hasta: listado vacío, sin error` | §4 |
| | `001-CA10 sin sesión: todas las operaciones de gastos dan 401` | CA10 |
| `src/test/java/com/trackergastos/web/rest/GastoValidacionIT.java` | `001-CA02 monto inválido al crear` (0; -10; 12.345; 10000000.01) | CA02 |
| | `001-CA02 monto inválido al editar (mismas reglas del punto 2)` (los mismos 4) | CA02 |
| | `001-CA02 bordes válidos del monto (0,01 y 10.000.000)` | §4 |
| | `001-CA03 fecha futura (mañana en Buenos Aires): 400 sobre la fecha y no se crea nada` | CA03 |
| | `001-CA03 fecha futura al editar: 400 sobre la fecha` | CA03 |
| `src/test/java/com/trackergastos/web/rest/CategoriaReglasIT.java` | `001-CA11 USER intenta crear, renombrar y borrar una categoría: 403 y nada cambia` | CA11 |
| | `001-CA11 USER sí puede listar las categorías (punto 9)` | punto 9 |
| | `001-CA12 nombre duplicado sin distinguir mayúsculas ni espacios` | CA12 |
| | `001-CA12 renombrar a un nombre que ya existe (solo cambia mayúsculas) también se rechaza` | CA12 |
| | `001-CA13 borrar categoría en uso: 400 error.categoriaenuso y la categoría sigue existiendo` | CA13 |
| | `001-CA13 borde: borrar una categoría sin gastos se permite` | §4 |
| | `001-CA14 categorías iniciales: existen las 6 del punto 13` | CA14 |
| `src/test/javascript/playwright/e2e/negocio/001-gastos.pw.ts` | `001-CA15 flujo E2E: carga, ve, edita y borra un gasto en "Mis gastos"` | CA15 |

Apoyo: `src/test/java/com/trackergastos/DatosPruebaGastos.java` y `src/test/javascript/playwright/e2e/negocio/helpers/usuarios.ts` (los mismos archivos que en la rama de la 002).

### Salida en rojo (2026-11-20)

`./mvnw -ntp verify -Dit.test='GastoControlAccesoIT,GastoValidacionIT,CategoriaReglasIT'`:

```
[ERROR] Tests run: 7, Failures: 5, Errors: 0, Skipped: 0 -- in com.trackergastos.web.rest.CategoriaReglasIT
[ERROR] Tests run: 14, Failures: 11, Errors: 0, Skipped: 0 -- in com.trackergastos.web.rest.GastoControlAccesoIT
[ERROR] Tests run: 13, Failures: 10, Errors: 0, Skipped: 0 -- in com.trackergastos.web.rest.GastoValidacionIT
[ERROR] Tests run: 34, Failures: 26, Errors: 0, Skipped: 0
```

Cada falla se revisó y es por la razón esperada, por ejemplo:
- CA04: `expected: "ana" but was: "beto"`.
- CA05: `X-Total-Count expected:<3> but was:<5>`.
- CA07/CA08: `Status expected:<404> but was:<200>` (o `<400>` para el PUT inexistente).
- CA09: aparece un gasto de `beto` y el orden es por id ascendente.
- CA02/CA03: `expected:<400> but was:<201>` (o `<200>` al editar).
- CA11: `expected:<403> but was:<201>`.
- CA13: `expected:<400> but was:<204>`.
- CA14: faltan las categorías en la base.

**Los 8 que pasan sin la lógica**, a propósito (aprobado por el documentador): son controles o bordes que el código generado ya cumple y que la implementación no tiene que romper. **Para el verificador: no son tests mal escritos, son tests de regresión.**
- `001-CA10 sin sesión`: lo da la seguridad generada para `/api/**`.
- `001-CA07 control`: ana ve su propio gasto.
- `001-CA11 USER sí puede listar`.
- `001-CA13 borde: borrar sin gastos`.
- `001-CA09 desde posterior a hasta`.
- `001-CA02 bordes válidos` (3 casos).

Los que prueban los criterios del 001-CA01 al CA14 fallan todos.

E2E, corrido con `npx playwright test --retries=0 e2e/negocio/001-gastos.pw.ts` contra la app levantada con `./mvnw` (perfil dev):

```
  ✘  1 [chromium] › e2e/negocio/001-gastos.pw.ts › 001-CA15 flujo E2E ... (30s)
    Error: locator.click: Test timeout of 30000ms exceeded.
      - waiting for getByRole('link', { name: 'Mis gastos' })
  1 failed
```

Falla porque todavía no existe el menú "Mis gastos". Es la falla esperada.

## Notas para la próxima sesión
- Los tests crean `ana` y `beto` y buscan las categorías por nombre o las crean, así funcionan antes y después de la carga inicial (CA14).
- El E2E usa el usuario `user` de JHipster y una descripción única para encontrar la fila; si queda algo a medias, lo borra por API en el `afterEach`.
