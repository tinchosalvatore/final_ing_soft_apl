# Verificación — Spec 002 (resumen mensual)

- Verificador: agente seguridad (no participó en la implementación ni en los tests de la 002)
- Rama / PR: [PR #6](https://github.com/tinchosalvatore/final_ing_soft_apl/pull/6) (`spec/002-resumen-mensual`), verificado sobre `688a4fb`, que es el código que entró a `main` en `3677ac6`. Cuando arranqué, el PR ya estaba mergeado, así que este verify entra por un PR aparte (`verify/002-resumen-mensual`)
- Fecha: 2026-11-20
- Resultado: **Cumple con observaciones**

<!-- No alcanza con que compile. Se contrasta contra la spec: ¿cada criterio tiene un test que lo representa de verdad? -->

**Resumen.**
- Los 12 criterios tienen un test que los representa, y el CI y mis corridas locales están en verde.
- Aislamiento (002-CA08): ningún parámetro de `GET /api/resumen` deja ver gastos de otro usuario. El servicio reusa `GastoQueryService.findByCriteria`, que agrega siempre "dueño = yo" (S2).
- Mes inválido (002-CA10): da 400 `error.mesinvalido`, sin detalles internos.
- Logs: sin montos.

Hay dos observaciones **bajas** sobre reglas que la spec deja abiertas y los tests no ejercitan:
- **O1:** el desempate alfabético no ordena bien acentos ni minúsculas.
- **O2:** el redondeo de una variación negativa "con la mitad hacia arriba" es ambiguo.

## 1. Criterios de aceptación

| ID | Test que lo cubre (archivo:nombre) | ¿El test representa el criterio? | Resultado |
|---|---|---|---|
| 002-CA01 | `ResumenMensualCalculadoraTest`: `002-CA01 total y detalle por categoría`; `ResumenResourceIT`: `002-CA01 GET /api/resumen?mes devuelve total, cantidad y detalle con la forma del contrato` | sí: los valores, el orden y la forma JSON de §5.1 (`mes` como `"2026-10"`, escala 0-100) | ✅ |
| 002-CA02 | `ResumenMensualCalculadoraTest`: `002-CA02 suma exacta sin errores de redondeo` | sí: 0,10 + 0,20 = 0,30 con `BigDecimal`, en el total y en el detalle | ✅ |
| 002-CA03 | `ResumenMensualCalculadoraTest`: `002-CA03 redondeo de participación a 1 decimal (33,3 %)` y `…la mitad hacia arriba (12,25 → 12,3)` | sí, incluido el caso exacto de la mitad que da la spec | ✅ |
| 002-CA04 | `ResumenMensualCalculadoraTest`: `002-CA04 variación positiva…` | sí: diferencia +200 y +20,0 | ✅ |
| 002-CA05 | `ResumenMensualCalculadoraTest`: `002-CA05 mes anterior en 0…`; `ResumenResourceIT` (CA01: `variacionPorcentual` no existe en el JSON) | sí. Ver O3 sobre "`null`" contra "campo ausente" | ✅ |
| 002-CA06 | `ResumenMensualCalculadoraTest`: `002-CA06 cambio de año…`; `MesResumenTest`: `002-CA11 el mes actual depende de hoy (enero)` | sí | ✅ |
| 002-CA07 | `ResumenMensualCalculadoraTest`: `002-CA07 límites del mes…` (28/02 contra 01/03, y día 1 contra el último del mes anterior); `ResumenMensualServiceIT`: `002-CA07 límites del mes contra la base` | sí: contra la base y contra la calculadora | ✅ |
| 002-CA08 | `ResumenMensualServiceIT`: `002-CA08 aislamiento…` | sí: `beto` tiene gastos en el mes y en el mes anterior, y ninguno afecta ni al total ni a la variación de `ana`. Lo confirmé como atacante por la API (§4, A1) | ✅ |
| 002-CA09 | `ResumenMensualCalculadoraTest`: `002-CA09 empate de categorías…` | sí para Ocio y Salud. **No** cubre acentos ni minúsculas (O1) | ✅ con O1 |
| 002-CA10 | `MesResumenTest`: `002-CA10 mes inválido`, `…el mes siguiente al actual es inválido`, `…los límites 2000-01 y el mes actual son válidos`; `ResumenResourceIT`: `002-CA10 mes inválido por API → 400 error.mesinvalido` (los 4 valores de la spec) | sí | ✅ |
| 002-CA11 | `MesResumenTest`: `002-CA11 sin mes…`; `ResumenMensualServiceIT` y `ResumenResourceIT`: `002-CA11…` | sí | ✅ |
| 002-CA12 | `e2e/negocio/002-resumen-mensual.pw.ts`: `002-CA12 flujo E2E…` | sí: crea un usuario nuevo por API, carga los dos gastos por la UI y comprueba `$ 1.500,00`, `+$ 1.500,00`, "Sin datos del mes anterior", `66,7 %` y `33,3 %`. **Pendiente:** está escrito a mano y falta regrabarlo con codegen. No bloquea | ⚠️ pendiente codegen |

Bordes del §4 cubiertos además por la calculadora:
- Mes sin gastos con el anterior con gastos → −100 %.
- Los dos meses sin gastos → variación `null`.
- Variación negativa → −33,3.

El caso "un gasto se edita y pasa a otro mes o categoría" no tiene test, pero el resumen se calcula siempre desde la base, así que no hay caché que pueda quedar desactualizada.

## 2. Casos borde no contemplados

Son propuestas. No corregí nada.

### O1 (baja): el desempate "alfabético" no es alfabético con acentos ni minúsculas

- **Qué pasa:** `ResumenMensualCalculadora` desempata con `thenComparing(CategoriaResumenDTO::getNombre)`, que compara por código Unicode. Las mayúsculas van antes que las minúsculas, y las letras acentuadas van después de la `z`. Por API, con tres categorías empatadas en 10:
  ```
  resultado: "Peluquería", "almacén", "Óptica"
  esperado (alfabético en español): "almacén", "Óptica", "Peluquería"
  ```
  (§4, A6). Un ADMIN puede crear esos nombres: la regla 11 de la 001 respeta las mayúsculas que escriba.
- **Propuesta:** comparar con un `java.text.Collator` para `es-AR` (fuerza PRIMARY o SECONDARY), y que el agente testing sume un caso con acento y minúscula a CA09. Decide el documentador si va en un fix chico.

### O2 (baja, ambigüedad de la spec): el redondeo de una variación negativa

- **Qué pasa:** la spec dice "1 decimal, con la mitad hacia arriba (12,25 → 12,3)". El código usa `RoundingMode.HALF_UP`, que **aleja la mitad del cero**. Para una variación de −12,25 % da **−12,3** (§4, A5). Si "hacia arriba" se lee literalmente (hacia +∞), daría −12,2.
- **Por qué importa:** los tests solo ejercitan la mitad en valores positivos (participación), donde las dos lecturas coinciden.
- **Propuesta:** que el documentador aclare la spec ("la mitad se aleja del cero", que es lo implementado y lo habitual) y, si quiere, que el agente testing sume el caso −12,25 → −12,3.

### O3 (baja, contrato): `variacionPorcentual` no informada sale como campo **ausente**, no como `null`

§5.1 dice "`null` = no informada", pero la configuración de Jackson de JHipster omite los `null`, así que el campo no aparece en el JSON. El test (`doesNotExist()`) y la UI ya lo tratan así, y para un cliente JavaScript `undefined` y `null` se manejan igual. **Propuesta:** que el documentador ajuste §5.1 a "ausente (o `null`)". No hace falta cambiar código.

### O4 (baja): `?mes=` vacío da 400

La regla 3.1 dice "si no indica cuál, se usa el mes actual". `GET /api/resumen` sin el parámetro devuelve el mes actual (CA11), pero `?mes=` vacío responde 400 `error.mesinvalido`. Es defendible, pero la spec no lo dice. **Propuesta:** dejarlo como está y anotarlo en §5.1, o tratar el vacío como ausente. Decide el documentador.

### O5 (para la UX, sin bloqueo)

- "Mes anterior" no se deshabilita en `2000-01`. Al pasar a `1999-12`, la pantalla muestra la alerta de error genérica en lugar de impedirlo. La spec solo pide deshabilitar "Mes siguiente".
- Una categoría con gastos muy chicos frente al total muestra `0,0 %`, por ejemplo 0,30 sobre 600,30 (§4, A4). Es correcto por el redondeo, pero puede confundir.

## 3. Revisión del diff (`3677ac6^1..3677ac6`)

| Tema | Resultado |
|---|---|
| **Archivos fuera del plan** | Ninguno relevante. `resumen.reglas.ts` reemplaza al `resumen.logica.ts` del plan (por la trampa de `*.log*` del `.gitignore`, O2 de la 001). `menu-gastos.tsx` suma `MenuResumen` (el plan decía `header.tsx`, que solo lo usa) |
| **Territorio del generador** | No se tocó. `routes.tsx`, `header.tsx` y `header.spec.tsx` son archivos compartidos o generados ya cubiertos por el ADR-0008, con el mismo patrón de la 001 |
| **Tests del agente testing** | Los commits del coder (`e8109f4`, `754ca56`, `688a4fb`) no tocan `src/test`. Los 3 commits que tocan `src/test` (`c083e63`, `e0eecec`, `10921fc`) salen de la sesión del agente testing, según el `Claude-Session` de cada commit, la misma de toda la fase roja |
| **Ajuste `10921fc` al E2E** | Es del agente testing (misma sesión que `e0eecec`). Hace la aserción del total exacta (`$ 1.500,00` también aparecía dentro de `+$ 1.500,00`) y **suma** aserciones (diferencia y "Sin datos del mes anterior"). No relaja nada |
| **Stubs en `src/main` hechos por testing (`c083e63`)** | Solo firmas, getters y setters, y `throw new UnsupportedOperationException(...)`. Están documentados en `tasks.md` (T2) y el coder los implementó sin cambiar firmas (plan §1). No hay lógica escrita por el agente testing |
| **`GastoService.hoy()` pasa a `public`** | Una sola definición de "hoy" para las dos specs. Sin otro cambio en `GastoService` |
| **Aislamiento** | `ResumenMensualService` arma un `GastoCriteria` propio con solo el rango de fechas, y no recibe ningún criterio del cliente. `GastoQueryService.createSpecification` agrega `delDueno(loginActual())` siempre. El único parámetro del endpoint es `mes` |
| **Logs (regla 6)** | `ResumenMensualService`: un DEBUG con el mes. `ResumenResource`: ningún log. Montos del escenario (`7000`, `1500.00`, `600.30`, `351`) en el log de la corrida: 0 apariciones |
| **Errores** | `MesInvalidoException` extiende `ReglaDeNegocioException`: 400 con `message = error.mesinvalido` y un `detail` en español sin clases ni stack (A3) |
| **Secretos y dependencias** | Ninguno |

## 4. Evidencia

### CI

Run [44787953854](https://github.com/tinchosalvatore/final_ing_soft_apl/actions/runs/44787953854) (`688a4fb`): **verde**.
- Backend: 216 tests de integración.
- E2E: 58 pasan y 1 se omite (`administration › /docs`, el mismo de antes). Entre los que pasan está `002-CA12 flujo E2E` (2.7 s).

### Tests en local (worktree `spec/002-resumen-mensual`, `688a4fb`, sin archivos ignorados)

- `./mvnw -ntp -Dskip.installnodenpm -Dskip.npm verify`: **BUILD SUCCESS**.
  - 79 tests unitarios: `MesResumenTest` 8/8, `ResumenMensualCalculadoraTest` 13/13 y ArchUnit.
  - 216 de integración: `ResumenMensualServiceIT` 3/3, `ResumenResourceIT` 7/7.
  - 0 fallas.
- `npm test`: 21 archivos, **187/187**.
- No corrí E2E en local (regla 13: hay otros agentes con E2E). La evidencia del E2E es la del CI.

### Ataques contra un jar de prod

- Jar `-Pprod` sin el front, perfil `e2e,secret-samples,prod`.
- Postgres aislado en el puerto 55432.
- Usuarios: `user` (atacante), `beto` (víctima) y `gamma` (casos de cálculo).
- Contenedor y jar borrados al terminar.

| # | Ataque o caso | Resultado |
|---|---|---|
| A1 | 002-CA08. `user` pide `2026-10` con cada uno de estos parámetros: `userId.equals=<beto>`, `userId.in=<beto>`, `user=beto`, `login=beto`, `user.login=beto`, `usuario=beto`, `fecha.greaterThanOrEqual=2000-01-01`, `categoriaId.equals=…`, `id.in=1,2,3`, `distinct=true` | En los 10 casos la respuesta es **byte a byte igual** a la que da sin parámetros: total 2000, sin los 7000 de `beto` en el mes ni los 999 del mes anterior. `beto` ve solo lo suyo (total 7000, variación 600,7 %) |
| A2 | 002-CA10 con valores raros: `mes` duplicado, `%00`, espacio inicial, `2026-1`, `02026-10`, `2026-00`, `+2026-10`, dígitos de ancho completo (codificados) y vacío | 400 `error.mesinvalido` en todos los casos |
| A3 | 002-CA10, cuerpo de la respuesta (`2099-01`, `1999-12`, `2026-13`, `abc`, `2026-12`) | 400, `message = error.mesinvalido`, `detail` en español ("El mes no existe", "…entre 2000-01 y el mes actual", "…formato AAAA-MM"), **sin clases ni stack**. Sin sesión → 401. Sin `mes` → `"mes":"2026-11"` |
| A4 | Cálculo por API: Ocio 300 y Salud 300 (empate), Transporte 0,10 + 0,20; mes anterior 1000 | Total `600.30`, Ocio antes que Salud, Transporte `0.30` con `0.0 %`, diferencia `-399.70`, variación `-40.0` |
| A5 | Variación negativa con la mitad exacta: 351 contra 400 (−12,25 %) | `-12.3` (HALF_UP aleja del cero; ver O2) |
| A6 | Desempate con `Óptica`, `Peluquería` y `almacén` (10 cada una) | Orden `Peluquería, almacén, Óptica` (O1) |
| A7 | Cambio de año: 2026-01 (150) contra 2025-12 (100) | diferencia 50, variación 50,0 |
| A8 | Mes sin gastos con el anterior con gastos | total 0, detalle vacío, variación −100,0 |
| A9 | Logs (regla 6): `grep` de los montos del escenario | 0 apariciones |
