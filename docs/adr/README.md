# Architecture Decision Records

Decisiones que condicionan decisiones futuras. Si algo del proyecto no es convencional, acá se vuelve "convencional para este proyecto".

## Índice

| ADR | Decisión | Estado |
|---|---|---|
| [0001](ADR-0001-monolito-jhipster-react-jwt.md) | Monolito JHipster con React y JWT | Aceptado |
| [0002](ADR-0002-postgresql-en-desarrollo.md) | PostgreSQL también en desarrollo (no H2) | Aceptado |
| [0003](ADR-0003-playwright-e2e.md) | Playwright como único framework E2E, grabado con codegen | Aceptado |
| [0004](ADR-0004-logs-appender-logstash.md) | Logs enviados con el appender de Logstash de JHipster | Aceptado |
| [0005](ADR-0005-pwa-workbox-indexeddb.md) | PWA con Workbox y gastos pendientes en IndexedDB | Aceptado |
| [0006](ADR-0006-pipeline-ci.md) | Pipeline de CI basado en el de la cátedra, no en `jhipster ci-cd` | Aceptado |
| [0007](ADR-0007-propiedad-de-los-datos.md) | Cada gasto pertenece a un usuario; las categorías son globales | Aceptado |

## Plantilla

Nombre del archivo: `ADR-NNNN-titulo-en-kebab-case.md`. El número es correlativo y no se reutiliza. Si una decisión cambia, se escribe un ADR nuevo y el viejo pasa a estado "Reemplazado por ADR-XXXX".

```markdown
# ADR-NNNN: <decisión en una línea>

- Estado: Propuesto | Aceptado | Reemplazado por ADR-XXXX
- Fecha: AAAA-MM-DD

## Contexto
Qué problema o fuerza obliga a decidir. Hechos, no opiniones.

## Decisión
Qué se decidió, en una o dos frases que se puedan cumplir o incumplir.

## Consecuencias
Qué se gana, qué se pierde y qué obliga a hacer de ahora en más.

## Alternativas
Qué otras opciones se consideraron y por qué se descartaron.
```
