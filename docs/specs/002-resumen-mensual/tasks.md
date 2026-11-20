# Tareas — Spec 002 (resumen mensual)

<!-- Lo escribe el implementador a partir del plan aprobado. Es la memoria de qué se hizo y qué no:
     se actualiza al cerrar cada tarea, así una sesión nueva puede retomar sin releer todo. -->

## Fase roja (agente testing, 2026-11-20)

| # | Tarea | Criterios que cubre | Estado | Commit |
|---|---|---|---|---|
| T1 | Contrato observable agregado a la spec (§5.1) por el documentador | todos | hecha | `e257276` |
| T2 | Stubs del contrato mínimo (lanzan `UnsupportedOperationException`) | — | hecha | `c083e63` |
| T3 | Unitarios del cálculo y del mes | 002-CA01 a CA07, CA09, CA10, CA11 | hecha (en rojo) | `e0eecec` |
| T4 | Integración del service y del endpoint (Testcontainers) | 002-CA01, CA07, CA08, CA10, CA11 | hecha (en rojo) | `e0eecec` |
| T5 | E2E del flujo escrito a mano con los textos de §5.1 | 002-CA12 | hecha (en rojo) | `e0eecec` |
| T6 | **Regrabar el E2E con codegen** cuando exista la pantalla "Resumen", antes del verify. Graba el humano: el agente testing levanta la app, le pasa el comando y el guion, y pule el código grabado (locators, aserciones, IDs) | 002-CA12 | **pendiente** | |

### Contrato para el coder

Paquete `com.trackergastos.service.resumen` (territorio del desarrollador). Las firmas son las que usan los tests: se pueden agregar dependencias al constructor del service, pero no cambiar nombres ni tipos sin avisar al agente testing.

| Clase | Firma | Qué espera el test |
|---|---|---|
| `ResumenMensualCalculadora` | `ResumenMensualDTO calcular(YearMonth mes, List<GastoDTO> gastosDelUsuario)` | Pura (se instancia con `new`). Recibe gastos de un solo usuario y de cualquier mes: usa los del `mes` para total y detalle y los del mes anterior para la variación |
| `MesResumen` | `static YearMonth resolver(String mes, LocalDate hoy)` | `null` → mes de `hoy`. Formato distinto de `AAAA-MM`, anterior a `2000-01` o posterior al mes de `hoy` → `MesInvalidoException` |
| `MesInvalidoException` | `extends RuntimeException` | La lanza `resolver` y la traduce el endpoint a 400 `error.mesinvalido` |
| `ResumenMensualService` (`@Service`) | `ResumenMensualDTO obtenerResumen(String mes)` | Usa solo los gastos del usuario autenticado; `null` = mes actual en `America/Argentina/Buenos_Aires` |
| `ResumenMensualDTO` | `mes` (`YearMonth`), `total`, `cantidad` (`long`), `categorias`, `diferencia`, `variacionPorcentual` (`null` = no informada) | Porcentajes en escala 0-100 con 1 decimal |
| `CategoriaResumenDTO` | `nombre`, `total`, `cantidad` (`long`), `participacion` | |
| Endpoint (spec §5.1) | `GET /api/resumen?mes=AAAA-MM` | JSON con `mes` serializado como `"2026-10"`; `variacionPorcentual` nula se omite o va `null` (el test usa `doesNotExist()`, que acepta las dos) |

### Tests escritos

| Archivo | Test (`@DisplayName`) | Criterio |
|---|---|---|
| `src/test/java/com/trackergastos/service/resumen/ResumenMensualCalculadoraTest.java` | `002-CA01 total y detalle por categoría` | CA01 |
| | `002-CA02 suma exacta sin errores de redondeo` | CA02 |
| | `002-CA03 redondeo de participación a 1 decimal (33,3 %)` | CA03 |
| | `002-CA03 la participación redondea la mitad hacia arriba (12,25 → 12,3)` | CA03 |
| | `002-CA04 variación positiva contra el mes anterior` | CA04 |
| | `002-CA05 mes anterior en 0: diferencia informada, variación porcentual no informada` | CA05 |
| | `002-CA06 cambio de año: enero se compara contra diciembre del año anterior` | CA06 |
| | `002-CA07 límites del mes: cuenta el 28/02 y no el 01/03` | CA07 |
| | `002-CA07 límites del mes: el día 1 cuenta y el último día del mes anterior no` | CA07 |
| | `002-CA09 empate de categorías: se ordena por nombre (Ocio antes que Salud)` | CA09 |
| | `002-borde mes sin gastos con mes anterior con gastos: total 0 y variación -100 %` | §4 |
| | `002-borde mes y mes anterior sin gastos: diferencia 0 y variación no informada` | §4 |
| | `002-borde variación negativa redondeada a 1 decimal` | §3.3 |
| `src/test/java/com/trackergastos/service/resumen/MesResumenTest.java` | `002-CA10 mes inválido: 2099-01 / 1999-12 / 2026-13 / abc` | CA10 |
| | `002-CA10 el mes siguiente al actual es inválido` | CA10 |
| | `002-CA10 los límites 2000-01 y el mes actual son válidos` | CA10 |
| | `002-CA11 sin mes se usa el mes actual` (y la variante de enero) | CA11 |
| `src/test/java/com/trackergastos/service/resumen/ResumenMensualServiceIT.java` | `002-CA08 aislamiento: el resumen de ana no incluye nada de beto` | CA08 |
| | `002-CA07 límites del mes contra la base` | CA07 |
| | `002-CA11 sin mes se devuelve el resumen del mes actual` | CA11 |
| `src/test/java/com/trackergastos/web/rest/ResumenResourceIT.java` | `002-CA01 GET /api/resumen?mes devuelve ... la forma del contrato` | CA01, §5.1 |
| | `002-CA10 mes inválido por API → 400 error.mesinvalido` (4 casos) | CA10, §5.1 |
| | `002-CA11 sin parámetro mes devuelve el mes actual` | CA11 |
| | `002 sin sesión → 401` | §5.1 |
| `src/test/javascript/playwright/e2e/negocio/002-resumen-mensual.pw.ts` | `002-CA12 flujo E2E: carga 2 gastos y ve el resumen con total y participación` | CA12 |

Apoyo: `src/test/java/com/trackergastos/DatosPruebaGastos.java` (crea usuarios, categorías y gastos con el `EntityManager`) y `src/test/javascript/playwright/e2e/negocio/helpers/usuarios.ts` (usuario nuevo por API: `/api/register` + activación del admin, y su borrado al final).

### Salida en rojo (2026-11-20)

`./mvnw -ntp verify -Dtest='MesResumenTest,ResumenMensualCalculadoraTest' -Dit.test='ResumenMensualServiceIT,ResumenResourceIT'`:

```
[ERROR] Tests run: 8, Failures: 5, Errors: 3, Skipped: 0 -- in com.trackergastos.service.resumen.MesResumenTest
[ERROR] Tests run: 13, Failures: 0, Errors: 13, Skipped: 0 -- in com.trackergastos.service.resumen.ResumenMensualCalculadoraTest
[ERROR]   ResumenMensualCalculadoraTest.ca01TotalYDetalle:54 » UnsupportedOperation 002: pendiente de implementar (fase roja)
[ERROR]   MesResumenTest.ca10MesInvalido:24  (espera MesInvalidoException, recibe UnsupportedOperationException)
[ERROR] Tests run: 21, Failures: 5, Errors: 16, Skipped: 0
[ERROR] Tests run: 3, Failures: 0, Errors: 3, Skipped: 0 -- in com.trackergastos.service.resumen.ResumenMensualServiceIT
[ERROR] Tests run: 7, Failures: 6, Errors: 0, Skipped: 0 -- in com.trackergastos.web.rest.ResumenResourceIT
```

Fallan 30 de 31. **El que pasa** es `002 sin sesión → 401`, porque lo garantiza la configuración de seguridad que generó JHipster para todo `/api/**`. Queda como test de regresión, no como prueba de la lógica (aprobado por el documentador). **Para el verificador: no es un test mal escrito.**

E2E, corrido con `npx playwright test --retries=0 e2e/negocio/002-resumen-mensual.pw.ts` contra la app levantada con `./mvnw` (perfil dev):

```
  ✘  1 [chromium] › e2e/negocio/002-resumen-mensual.pw.ts › 002-CA12 flujo E2E ... (30s)
    Error: locator.click: Test timeout of 30000ms exceeded.
      - waiting for getByRole('link', { name: 'Mis gastos' })
  1 failed
```

Falla porque todavía no existe el menú "Mis gastos". Es la falla esperada. Antes de fallar, el test ya había creado y activado al usuario nuevo por API, y el `afterEach` lo borró sin errores.

## Notas para la próxima sesión
- Los tests de integración crean los usuarios `ana` y `beto` y buscan las categorías por nombre o las crean, así funcionan antes y después de que se carguen las categorías iniciales de la 001.
- El E2E del CA12 crea un usuario nuevo por API, así puede afirmar valores exactos. Necesita que `/api/register` y la activación por `PUT /api/admin/users/{login}` sigan andando.
- Los E2E de la 002 asumen que existen "Mis gastos" y las categorías iniciales de la 001.
