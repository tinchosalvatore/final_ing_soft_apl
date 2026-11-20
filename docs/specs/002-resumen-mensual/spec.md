# Spec 002 — Resumen mensual

- Estado: Aprobada para planificar
- Rama: `spec/002-resumen-mensual`
- Depende de: 001

## 1. Contexto y objetivo
Registrar gastos no sirve de mucho si no se puede ver a dónde va el dinero. El resumen mensual le muestra al usuario cuánto gastó en un mes, cómo se reparte entre categorías y si gastó más o menos que el mes anterior.

Es la lógica de negocio propia del proyecto (no la genera JHipster), y por eso es la que se desarrolla con **los tests escritos antes del código** por un agente distinto al que implementa.

## 2. Definiciones
| Término | Significado |
|---|---|
| **Mes** | Un año y un mes calendario, escrito `AAAA-MM` (ej.: `2026-11`). Incluye del día 1 al último día de ese mes. |
| **Mes actual** | El mes que contiene a "hoy" (zona `America/Argentina/Buenos_Aires`, igual que en la spec 001). |
| **Mes anterior** | El mes calendario inmediatamente anterior (el anterior a `2026-01` es `2025-12`). |
| **Total del mes** | Suma de los montos de los gastos **del usuario** cuya fecha cae en el mes. |
| **Participación** | Porcentaje del total del mes que representa una categoría. |
| **Variación** | Diferencia entre el total del mes y el total del mes anterior, en pesos y en porcentaje. |

## 3. Comportamiento esperado
1. Un usuario puede pedir el resumen de un mes. Si no indica cuál, se usa el mes actual.
2. El resumen contiene:
   - **Total del mes**.
   - **Cantidad de gastos** del mes.
   - **Detalle por categoría**, una fila por cada categoría con al menos un gasto en el mes: nombre, total, cantidad de gastos y participación.
   - **Variación contra el mes anterior**: diferencia en pesos (puede ser negativa) y diferencia porcentual.
3. Reglas de cálculo:
   - Los montos se suman **exactos**, sin errores de redondeo (0,10 + 0,20 = 0,30).
   - La participación se redondea a **1 decimal, con la mitad hacia arriba** (12,25 → 12,3). La suma de las participaciones puede no dar exactamente 100 por el redondeo, y eso se acepta.
   - Variación porcentual = (total del mes − total del mes anterior) / total del mes anterior × 100, redondeada a 1 decimal con la mitad hacia arriba.
   - Si el total del mes anterior es 0, la variación porcentual **no se informa** (se muestra "sin datos del mes anterior"), pero la diferencia en pesos sí.
4. El detalle por categoría se ordena por total de mayor a menor. Si dos categorías empatan, se ordenan por nombre alfabéticamente.
5. El resumen considera **únicamente los gastos del usuario que lo pide**.
6. Se puede pedir cualquier mes desde `2000-01` hasta el mes actual. Un mes posterior al actual, uno anterior a `2000-01` o un formato inválido se rechazan con error de validación.
7. En la interfaz hay una pantalla "Resumen" con un selector de mes (por defecto el actual, con botones de mes anterior y siguiente; el siguiente se deshabilita en el mes actual). Muestra el total, la variación (indicando claramente si es aumento o baja) y la tabla por categoría.

## 4. Casos borde
- Mes sin gastos → total 0, cantidad 0, detalle vacío y variación calculada contra el mes anterior (si el anterior tuvo gastos: −100 %).
- Mes actual y mes anterior ambos sin gastos → total 0, diferencia 0 y variación porcentual no informada.
- Una sola categoría → participación 100,0.
- Gasto con fecha el último día del mes (ej.: 2026-02-28) → cuenta en ese mes. Gasto del día 1 del mes siguiente → no cuenta.
- Enero: el mes anterior es diciembre del año anterior.
- Otro usuario tiene gastos en el mismo mes → no afectan en nada al resumen.
- Un gasto se edita y pasa a otra categoría o a otro mes → el resumen siguiente lo refleja.

## 5. Criterios de aceptación

| ID | Escenario | Entrada | Resultado esperado |
|---|---|---|---|
| 002-CA01 | Total y detalle | `ana` en 2026-10: Supermercado 1000 y 500; Transporte 500 | Total 2000; cantidad 3; Supermercado 1500 (2 gastos, 75,0 %); Transporte 500 (1 gasto, 25,0 %), en ese orden |
| 002-CA02 | Suma exacta | Gastos de 0,10 y 0,20 en el mes | Total exactamente 0,30 |
| 002-CA03 | Redondeo de participación | Total 3 con categorías de 1, 1 y 1 | Cada una 33,3 % |
| 002-CA04 | Variación positiva | Mes 2026-10 total 1200; 2026-09 total 1000 | Diferencia +200; variación +20,0 % |
| 002-CA05 | Variación con mes anterior en 0 | Mes 2026-10 total 500; 2026-09 sin gastos | Diferencia +500; variación porcentual no informada |
| 002-CA06 | Cambio de año | Resumen de 2026-01 con gastos en 2025-12 | La variación se calcula contra 2025-12 |
| 002-CA07 | Límites del mes | Gastos el 2026-02-28 y el 2026-03-01; resumen de 2026-02 | Solo cuenta el del 28 |
| 002-CA08 | Aislamiento | `beto` tiene gastos en 2026-10; `ana` pide 2026-10 | El resumen de `ana` no incluye nada de `beto` |
| 002-CA09 | Empate de categorías | Ocio 300 y Salud 300 | Ocio antes que Salud |
| 002-CA10 | Mes inválido | `2099-01`, `1999-12`, `2026-13`, `abc` | Error de validación en todos los casos |
| 002-CA11 | Mes por defecto | Pedido sin mes | Resumen del mes actual |
| 002-CA12 | Flujo E2E | `user` carga 2 gastos de categorías distintas y abre "Resumen" | Ve el total y las 2 categorías con su participación |

## 6. Fuera de alcance
- Presupuestos o límites por categoría y alertas por exceso.
- Gráficos (alcanza con la tabla; se pueden sumar si sobra tiempo, sin cambiar esta spec).
- Resúmenes anuales o por rango arbitrario de fechas.
- Comparar contra el promedio de varios meses.
- Exportar el resumen.

## 7. Referencias
- [Spec 001](../001-gastos/spec.md) (definición de gasto, dueño y "hoy")
- [ADR-0007 — Propiedad de los datos](../../adr/ADR-0007-propiedad-de-los-datos.md)
- [Guía de testing](../../catedra/testing.md) (flujo tests-primero)
