# Plan — Spec 002 (resumen mensual)

- Autor: agente coder (sesión `coder`, 2026-11-20)
- Estado: Borrador

<!-- Lo escribe el implementador ANTES de codear. No se codea hasta que el humano lo apruebe. -->

## 1. Resumen de la solución

El cálculo es una clase **pura**, `ResumenMensualCalculadora`: recibe los gastos y devuelve el resumen, sin acceso a la base ni a la seguridad. Así los criterios de cálculo (CA01 a CA07 y CA09) se prueban con tests unitarios rápidos.

`ResumenMensualService` resuelve el mes (`MesResumen.resolver`) y trae los gastos del usuario autenticado del mes pedido y del anterior. Para eso **reusa `GastoQueryService.findByCriteria`**, que ya impone "dueño = yo" (S2, spec 001), con un filtro de fechas. Después le pasa los gastos a la calculadora.

Un endpoint fino, `ResumenResource` (`GET /api/resumen?mes=AAAA-MM`), expone el resultado. En el front, una pantalla "Resumen" con selector de mes.

Se implementan los stubs y el contrato que dejó el agente testing en `tasks.md`, sin cambiar firmas.

## 2. Archivos que se tocan

| Archivo | Territorio | Qué cambia |
|---|---|---|
| `service/resumen/MesResumen.java` | Desarrollador | `resolver`: formato `^\d{4}-\d{2}$` + `YearMonth.parse`; rango `[2000-01, mes de hoy]`; `null` → mes de hoy. Cualquier otro caso → `MesInvalidoException` |
| `service/resumen/MesInvalidoException.java` | Desarrollador | Pasa a extender `ReglaDeNegocioException` (clave `mesinvalido`). Sigue siendo una `RuntimeException` y conserva el constructor `(String)` del contrato. El `ExceptionTranslator` de la 001 ya la traduce a 400 `error.mesinvalido`, sin tocarlo |
| `service/resumen/ResumenMensualCalculadora.java` | Desarrollador | Cálculo (ver §3) |
| `service/resumen/ResumenMensualService.java` | Desarrollador | `obtenerResumen(mes)`: `GastoQueryService.findByCriteria(criteria con fecha ≥ primer día del mes anterior y ≤ último día del mes, Pageable.unpaged())` → calculadora |
| `service/resumen/ResumenMensualDTO.java`, `CategoriaResumenDTO.java` | Desarrollador | Sin cambios de forma: totales con escala 2 y porcentajes con escala 1 |
| `service/GastoService.java` | Desarrollador | `hoy()` pasa a `public`: una sola definición de "hoy" para las dos specs |
| `web/rest/ResumenResource.java` (nuevo) | Desarrollador | `GET /api/resumen` con `@RequestParam(required = false) String mes` |
| `app/modules/resumen/` (nuevo: `resumen.tsx`, `resumen.logica.ts`, `resumen.api.ts`, `textos.ts`, tests) | Desarrollador | Pantalla "Resumen" (ver §3) |
| `app/routes.tsx`, `shared/layout/header/header.tsx` | Compartido (ADR-0008) | Ruta `/resumen` e ítem de menú "Resumen" |
| `header.spec.tsx` (generado, ya ajustado en la 001) | ADR-0008 | Un USER ve "Resumen" |

## 3. Dónde vive cada regla de negocio

| Regla de la spec | Clase / método |
|---|---|
| 3.1 / 3.6: mes por defecto y rango válido (CA10, CA11) | `MesResumen.resolver` |
| 3.2: total, cantidad y detalle por categoría (CA01) | `ResumenMensualCalculadora.calcular` |
| 3.3: suma exacta con `BigDecimal`, sin `double` (CA02) | `ResumenMensualCalculadora` |
| 3.3: participación = total de la categoría × 100 / total del mes, con `divide(…, 1, HALF_UP)` sobre el cociente exacto (CA03) | `ResumenMensualCalculadora` |
| 3.3: variación % = diferencia × 100 / total anterior, con `divide(…, 1, HALF_UP)`; `null` si el anterior es 0 (CA04, CA05) | `ResumenMensualCalculadora` |
| Mes anterior con `YearMonth.minusMonths(1)`, que resuelve el cambio de año (CA06) | `ResumenMensualCalculadora` |
| Límites del mes por `YearMonth.from(fecha)` (CA07) | `ResumenMensualCalculadora` (y el filtro de fechas del service) |
| 4: orden por total desc y nombre asc (CA09) | `ResumenMensualCalculadora` |
| 5: solo gastos del usuario (CA08) | `ResumenMensualService` → `GastoQueryService` (S2) |
| 7: pantalla "Resumen" (CA12) | `app/modules/resumen/resumen.tsx` |

**Pantalla "Resumen"** (textos exactos de §5.1):
- Mes en `AAAA-MM`, con los botones "Mes anterior" y "Mes siguiente". "Mes siguiente" se deshabilita en el mes actual.
- Total `$ 1.500,00` y cantidad de gastos.
- Variación: "Aumento" o "Baja", con `+20,0 %` o `-100,0 %`, y la diferencia en pesos. Si no se informa, "Sin datos del mes anterior".
- Tabla con categoría, total, cantidad y participación (`66,7 %`).

## 4. Tecnologías y dependencias nuevas

Ninguna.

## 5. Decisiones tomadas por el agente

1. **Reusar `GastoQueryService` y no escribir una consulta nueva.** El aislamiento por usuario (CA08) sale del mismo código que ya prueban los tests de la 001. No hace falta tocar `GastoRepository`, que es territorio del generador.
2. **El cálculo se hace en memoria** con los gastos de dos meses de un usuario. Es un volumen chico, y la calculadora pura es la que pide el contrato de testing. Una agregación en SQL (`GROUP BY`) sería más rápida con muchos datos, pero duplicaría las reglas de redondeo.
3. **`MesInvalidoException extends ReglaDeNegocioException`**: reusa la traducción a 400 `error.<clave>` de la 001.

## 6. Decisiones pendientes

Las consulto primero con el documentador.

1. **Variación exactamente 0 %** (mismo total que el mes anterior, distinto de 0). La spec dice "Aumento" o "Baja". Propuesta: mostrar "Sin cambios" y `0,0 %`. Es un texto nuevo, y no lo usa ningún E2E.
2. **Formato de la diferencia en pesos.** La spec no lo fija. Propuesta: con signo delante, `+$ 200,00` / `-$ 200,00`, igual que la variación porcentual.
3. **Integrar la 001 en la rama de la 002.** La 002 necesita el código de la 001: el filtro por dueño, para CA08, y "Mis gastos", para el E2E CA12. Propuesta:
   - Mergear `spec/001-gastos` en `spec/002-resumen-mensual` ahora.
   - Cuando el PR #5 entre en `main`, mergear `main` otra vez.
   - Hasta entonces, el PR #6 muestra también los commits de la 001.

   La alternativa es esperar a que se mergee el PR #5 antes de seguir.
4. **¿El menú "Resumen" va al lado de "Mis gastos"?** Propuesta: sí, para todos los usuarios con sesión.

## 7. Riesgos

- **`YearMonth` en JSON:** se serializa como `"2026-10"` solo si Jackson no escribe las fechas como timestamps. JHipster lo configura así, y lo verifica `ResumenResourceIT` (CA01).
- **E2E CA12:** crea el usuario con `/api/register`. En dev y en el CI (`secret-samples`) el registro está habilitado (spec 001, regla 16), así que no hay problema.
