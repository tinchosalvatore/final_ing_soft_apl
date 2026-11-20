# ADR-0008: Interfaz propia de "Mis gastos" y archivos generados que se modifican a mano

- Estado: Aceptado
- Fecha: 2026-11-20

## Contexto
- La spec 001 fija textos exactos de la interfaz (§5.1), y los E2E los buscan. Por ejemplo, "Mis gastos", "Nuevo gasto", "Borrar", "Confirmar" y el formato de montos `$ 1.500,50`.
- La spec 001 también pide filtros por fecha y categoría, y errores junto al campo (regla 15).
- El CRUD que genera JHipster en `src/main/webapp/app/entities/gasto/` tiene otros textos ("Crear nuevo Gasto", "Eliminar"), ordena por columna y muestra el dueño. Además, se regenera desde `tracker.jh` y pisaría cualquier cambio.
- Para implementar la spec 001 sin agregar métodos a `GastoRepository` alcanza con `Specification`. La consulta de nombre de categoría sin distinguir mayúsculas ni espacios (regla 11) no se puede derivar del nombre de un método: necesita JPQL.
- El menú "Entidades" que genera JHipster se muestra a cualquier usuario con sesión.

## Decisión
- **"Mis gastos" es un módulo propio** en `src/main/webapp/app/modules/gastos/`, territorio del desarrollador. Lo forman el listado con filtros, el formulario de alta y edición, y el diálogo de borrado. Sus textos están en `textos.ts`, que es la única fuente de los textos de la spec 001 §5.1. Habla con `/api/gastos` y `/api/categorias` directamente, sin el reducer generado.
- **El CRUD generado no se modifica.** El menú "Entidades" queda **solo para ADMIN**, igual que el ítem "Categorías", que abre el CRUD generado de categorías.
- **Archivos generados o compartidos que se modifican a mano.** Al regenerar, se responde así:

| Archivo | Cambio | Al regenerar |
|---|---|---|
| `app/shared/layout/header/header.tsx` | Ítems "Mis gastos" y "Categorías"; "Entidades" solo ADMIN | `n` y reaplicar |
| `app/routes.tsx` | Ruta `/mis-gastos/*` | `n` y reaplicar |
| `app/shared/layout/header/header.spec.tsx` | Un USER ve "Mis gastos" y no ve "Entidades" | `n` |
| `repository/CategoriaRepository.java` | `existeNombreNormalizado` (JPQL `lower(trim(...))`) | `i` (ignorar siempre) |
| `GastoResourceIT.java`, `CategoriaResourceIT.java` | Ajustados a las reglas de la 001 (ver `docs/specs/001-gastos/tasks.md`) | `i` |
| `web/rest/errors/ExceptionTranslator.java`, `service/UserService.java`, `config/ApplicationProperties.java` | Traducción de las excepciones de negocio, registro deshabilitado (regla 16), propiedad `application.registro.habilitado` | `n` y reaplicar |

  Los archivos de ADR-0007 (`GastoService`, `GastoQueryService`, `GastoResource`, `CategoriaService`, `CategoriaResource`) siguen esa misma regla: `n` o `i`.
- **Mensajes de error de negocio** (`error.nombreduplicado`, `error.categoriaenuso`): van en `src/main/webapp/i18n/es/reglas.json`. Es un archivo propio, que el `deepmerge` de `i18n/es/es.js` junta con los generados.

## Consecuencias
- Los textos de la UI que prueban los E2E están en un solo lugar y una regeneración no los pisa.
- Un USER puede abrir igual el CRUD generado escribiendo `/gasto` en la URL. No es un problema de seguridad: el backend aplica las mismas reglas (S1 a S4). Solo es una pantalla menos cuidada.
- Regenerar exige revisar la tabla de arriba. El paso "revisar el diff" de `AGENTS.md` §4.3 lo cubre.
- El módulo no usa `Translate`: si algún día la app tuviera otro idioma, `textos.ts` se pasaría a i18n.

## Alternativas
- **Editar el CRUD generado de `entities/gasto/`:** cada regeneración pisa los cambios o los convierte en conflictos grandes. Descartado.
- **Agregar `findOneByIdAndUserLogin` y `existsByCategoriaId` a `GastoRepository`:** obliga a ignorar ese archivo al regenerar. `Specification` resuelve lo mismo desde el service (`GastoSpecifications`). Descartado.
- **Textos en un JSON de i18n:** sería la convención de JHipster, pero la app es solo en español, los tests unitarios tendrían que cargar las traducciones, y la spec fija los textos literales. Descartado por ahora.
