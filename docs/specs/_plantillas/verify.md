# Verificación — Spec NNN

- Verificador: agente revisor (sesión nueva, sin participación previa)
- Rama / PR: <link>
- Resultado: Cumple | Cumple con observaciones | No cumple

<!-- No alcanza con que compile. Se contrasta contra la spec: ¿cada criterio tiene un test que lo representa de verdad? -->

## 1. Criterios de aceptación
| ID | Test que lo cubre (archivo:nombre) | ¿El test representa el criterio? | Resultado |
|---|---|---|---|
| NNN-CA01 | | sí / parcial / no | ✅ / ❌ |

## 2. Casos borde no contemplados
Lo que encontró el revisor y no estaba en la spec → proponer cambio a la spec, no al código.

## 3. Revisión del diff
Archivos fuera del plan, código tocado en territorio del generador, secretos, logs con datos sensibles.

## 4. Evidencia
Link a la ejecución del CI, reportes y capturas.
