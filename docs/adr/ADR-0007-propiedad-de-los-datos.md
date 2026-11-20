# ADR-0007: Cada gasto pertenece a un usuario; las categorías son globales

- Estado: Aceptado
- Fecha: 2026-11-20

## Contexto
- Los gastos son datos financieros personales.
- El CRUD que genera JHipster deja que **cualquier usuario autenticado lea, modifique y borre cualquier registro**, y acepta el campo `user` que manda el cliente. Eso es exactamente una **Pérdida de control de acceso / IDOR** (OWASP A01).
- Las categorías son pocas y comunes a todos (Supermercado, Transporte...). Que cada usuario tenga las suyas duplicaría el problema de control de acceso sin aportar al alcance.

## Decisión
- **`Gasto`** tiene un dueño (`user`). En todas las operaciones (listar, filtrar, ver, editar, borrar, resumen):
  - El backend **asigna el dueño** a partir del usuario autenticado e ignora el `user` que mande el cliente.
  - El backend **solo opera sobre gastos del usuario autenticado**. Si el gasto existe pero es de otro, responde **404** (igual que si no existiera, para no revelar que existe).
  - El filtrado (`GastoQueryService`) **siempre** agrega la condición "dueño = usuario autenticado", sin importar los criterios que mande el cliente.
- **`Categoria`** es global: cualquier usuario autenticado la lee, y solo `ROLE_ADMIN` la crea, edita o borra.

## Consecuencias
- Se toca código generado en `GastoService`, `GastoQueryService` y `CategoriaResource` (con `@PreAuthorize`). Esos archivos pasan a territorio del desarrollador y, al regenerar, se responden con `n` o `i`.
- Es el ejemplo principal de seguridad "en código" del proyecto, con tests que lo prueban.
- Un ADMIN no ve los gastos de los demás por la API de gastos (privacidad por defecto).

## Alternativas
- **Responder 403 al acceder a un gasto ajeno:** confirma que ese id existe (filtración de información). Descartado a favor de 404.
- **Categorías por usuario:** más flexible, pero duplica el control de acceso. Fuera de alcance.
- **Validar la propiedad solo en el frontend:** no es control de acceso. Descartado.
