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
| T6 | **Regrabar el E2E con codegen** cuando exista "Mis gastos", antes del verify. Graba el humano: el agente testing levanta la app, le pasa el comando y el guion, y pule el código grabado (locators, aserciones, IDs) | 001-CA15 | **pendiente** | |
| T7 | Integración del registro deshabilitado (criterio nuevo, R1 del verify 000) | 001-CA16 | hecha (en rojo) | ver commit `test(001): 001-CA16` |

### Contrato para el coder
No hace falta ninguna clase nueva: los tests usan la API que ya existe (`/api/gastos`, `/api/gastos/count`, `/api/categorias`) con los códigos y claves de la spec §5.1. Lo que los tests esperan además:

- **001-CA16, registro deshabilitado por configuración:** el test pone `application.registro.habilitado=false` con `@TestPropertySource`. El coder la agrega a `ApplicationProperties` (prefijo `application`): `true` por defecto (dev y test) y `false` en `application-prod.yml`. Con `false`, `POST /api/register` responde **401** sin tocar la tabla de usuarios. Si prefiere otro nombre de propiedad o un perfil, avisa al agente testing y se cambia solo la anotación del test.
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
| `src/test/java/com/trackergastos/web/rest/RegistroDeshabilitadoIT.java` | `001-CA16 registro deshabilitado: el login de una cuenta desactivada → 401 y la cuenta queda intacta` | CA16 |
| | `001-CA16 registro deshabilitado: un login nuevo → 401 y no se crea ningún usuario` | CA16, regla 16 |
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

001-CA16 (`-Dit.test=RegistroDeshabilitadoIT`):

```
[ERROR] Tests run: 2, Failures: 0, Errors: 2, Skipped: 0 -- in com.trackergastos.web.rest.RegistroDeshabilitadoIT
Caused by: UnboundConfigurationPropertiesException: The elements [application.registro.habilitado] were left unbound.
```

Falla al levantar el contexto porque `ApplicationProperties` (`ignoreUnknownFields = false`) todavía no tiene la propiedad del contrato. Cuando el coder la agregue, sin el bloqueo el test falla por la razón de negocio: `/api/register` responde 201 y, en el primer caso, borra y recrea la cuenta desactivada (`UserService.removeNonActivatedUser`).

E2E, corrido con `npx playwright test --retries=0 e2e/negocio/001-gastos.pw.ts` contra la app levantada con `./mvnw` (perfil dev):

```
  ✘  1 [chromium] › e2e/negocio/001-gastos.pw.ts › 001-CA15 flujo E2E ... (30s)
    Error: locator.click: Test timeout of 30000ms exceeded.
      - waiting for getByRole('link', { name: 'Mis gastos' })
  1 failed
```

Falla porque todavía no existe el menú "Mis gastos". Es la falla esperada.

## Fase verde (agente coder, 2026-11-20)

Plan: `plan.md` (aprobado). Decisión estructural: **ADR-0008**.

| # | Tarea | Criterios que cubre | Estado | Commit |
|---|---|---|---|---|
| C1 | Registro deshabilitado en prod: `application.registro.habilitado` (`true` por defecto, `false` en prod real, `true` en `secret-samples`). Lo controla `UserService.registerUser` antes de tocar la tabla de usuarios | 001-CA16, regla 16 | hecha | `feat(001): registro público deshabilitado…` |
| C2 | `CampoInvalidoException` y `ReglaDeNegocioException`, traducidas por el `ExceptionTranslator` a `fieldErrors` / `error.<clave>` | §5.1 | hecha | `feat(001): errores de reglas de negocio…` |
| C3 | `GastoService`, `GastoQueryService`, `GastoSpecifications` y `GastoResource`: dueño = yo, 404 para un gasto ajeno o inexistente, validaciones, orden impuesto, logs solo con ids | 001-CA01 a CA10 (S1 a S4) | hecha | `feat(001): control de acceso y validaciones…` |
| C4 | `CategoriaService` (`strip`, 2 a 40 caracteres, únicos normalizados, no se borra en uso), `CategoriaRepository.existeNombreNormalizado` y `@PreAuthorize` ADMIN en `CategoriaResource` | 001-CA11 a CA13 (S10) | hecha | `feat(001): reglas de categorías…` |
| C5 | Changelog de las categorías iniciales, sin contexto | 001-CA14 | hecha | `feat(001): categorías iniciales…` |
| C6 | Ajuste de los tests generados (ver abajo) | — | hecha | `test(001): …` |
| C7 | Módulo "Mis gastos" (`app/modules/gastos/`): listado con filtros, formulario y diálogo de borrado. Menú "Mis gastos"; "Categorías" y "Entidades" solo ADMIN. Tests Vitest de formato y validaciones | 001-CA15, reglas 14 y 15 | hecha | `feat(001): módulo Mis gastos…`, `feat(001): menú…` |
| C8 | ADR-0008 y su entrada en el índice | — | hecha | `docs(001): ADR-0008…` |
| C9 | Integrar `main` (PR #4 de ci: pipeline nuevo, Tomcat y Jackson) | — | hecha | merge `origin/main` |
| C10 | Push y PR #5 (de borrador a "ready for review") | — | hecha | |
| T6 | **Regrabar el E2E con codegen**: ahora sí existe "Mis gastos". Graba el humano y pule el agente testing | 001-CA15 | **pendiente (testing)** | |

### Tests generados que se ajustaron (no son del agente testing)
- `GastoResourceIT`:
  - `createEntity` le asigna el dueño `user`, que es el del `@WithMockUser`.
  - `putNonExistingGasto` y `patchNonExistingGasto` esperan **404** (antes 400 `idnotfound`), como pide la spec §5.1.
  - `getAllGastosByUserIsEqualToSomething` pasa a `getAllGastosByUserFilterIsIgnored`: el filtro `userId` se ignora (S2).
  - Se borraron los dos métodos de eager load con mocks, que no eran `@Test` y usaban `GastoService.findAllWithEagerRelationships`, que ya no existe.
- `CategoriaResourceIT`: corre como ADMIN (S10).
- `header.spec.tsx`: un USER ve "Mis gastos" y no el menú "Entidades" (ADR-0008).
- E2E `entity/gasto.pw.ts` y `entity/categoria.pw.ts`: loguean como ADMIN, porque el CRUD generado es solo para ADMIN (ADR-0008, S10).

**No se modificó ningún test del agente testing.**

### Evidencia (2026-11-20)
- Tests del agente testing, todos en verde: `CategoriaReglasIT` 7/7, `GastoControlAccesoIT` 14/14, `GastoValidacionIT` 13/13 y `RegistroDeshabilitadoIT` 2/2.
- `./mvnw -ntp verify` (con ArchUnit): 58 tests unitarios y 206 de integración, todos en verde.
- `npm test` (lint + Vitest): 181 en verde.
- E2E contra la app levantada con `./mvnw` (perfil dev): `e2e/negocio/001-gastos.pw.ts` (001-CA15) pasa, y la suite completa también (58 tests; los 8 de `entity/` primero fallaron como USER y pasan como ADMIN).

### Notas del coder
- "Hoy" se calcula siempre con `America/Argentina/Buenos_Aires` (`GastoService.ZONA_HOY`), sin `Clock` inyectable. Los tests de testing usan la misma zona.
- Los textos del módulo están en `textos.ts` y no en un JSON de i18n (ADR-0008). Solo las claves de error de negocio van en `i18n/es/reglas.json`.
- El CRUD generado de gastos sigue accesible escribiendo `/gasto` en la URL, pero el backend aplica las mismas reglas.

## Notas para la próxima sesión
- Los tests crean `ana` y `beto` y buscan las categorías por nombre o las crean, así funcionan antes y después de la carga inicial (CA14).
- El E2E usa el usuario `user` de JHipster y una descripción única para encontrar la fila; si queda algo a medias, lo borra por API en el `afterEach`.
