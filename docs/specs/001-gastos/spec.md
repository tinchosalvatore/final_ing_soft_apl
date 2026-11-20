# Spec 001 — Gastos y categorías

- Estado: Aprobada para planificar
- Rama: `spec/001-gastos`
- Depende de: generación base (paso 000 en `docs/catedra/agentes-sdd.md`)

## 1. Contexto y objetivo
Una persona quiere registrar en qué gasta su dinero para después entender en qué se le va. Esta spec define el núcleo del producto: cargar, consultar, corregir y borrar gastos propios, clasificados en categorías comunes a todos.

Además del objetivo de producto hay un objetivo de seguridad: **los gastos son datos financieros personales y nadie puede ver ni tocar los gastos de otra persona**, aunque conozca su identificador.

## 2. Definiciones
| Término | Significado |
|---|---|
| **Usuario** | Persona registrada que inició sesión. Tiene rol `USER`, y algunos además `ADMIN`. |
| **Gasto** | Un egreso de dinero de un usuario: monto, fecha, categoría y una descripción opcional. |
| **Dueño** | El usuario que registró el gasto. Un gasto tiene un solo dueño y no cambia nunca. |
| **Categoría** | Rubro que agrupa gastos (ej.: Supermercado). Es la misma para todos los usuarios. |
| **Hoy** | La fecha actual en la zona horaria `America/Argentina/Buenos_Aires`. |
| **Monto** | Importe en pesos, positivo y con hasta 2 decimales. |

## 3. Comportamiento esperado

### Gastos
1. Un usuario puede **registrar** un gasto indicando monto, fecha, categoría y, si quiere, una descripción.
2. Reglas de un gasto válido:
   - El monto es **mayor a 0** y **menor o igual a 10.000.000**, con **como máximo 2 decimales**.
   - La fecha es obligatoria y **no puede ser posterior a hoy**.
   - La categoría es obligatoria y tiene que existir.
   - La descripción es opcional y tiene como máximo 140 caracteres.
3. El dueño del gasto es **siempre** el usuario que lo registra. Si la petición dice otro dueño, se ignora sin error.
4. Un usuario puede **listar** sus gastos, ordenados por fecha del más reciente al más antiguo (a igual fecha, el último registrado primero) y paginados.
5. Un usuario puede **filtrar** su listado por rango de fechas (desde/hasta, ambos inclusive) y por categoría. El filtro nunca devuelve gastos de otro usuario, sin importar qué criterios se envíen.
6. Un usuario puede **ver, editar y borrar** sus propios gastos. Al editar se aplican las mismas reglas del punto 2 y el dueño no cambia.
7. Si un usuario intenta ver, editar o borrar un gasto **que no es suyo**, el sistema responde exactamente igual que si ese gasto no existiera ("no encontrado").
8. Sin sesión iniciada no se puede hacer ninguna operación sobre gastos.

### Categorías
9. Cualquier usuario con sesión puede **listar** las categorías (para elegir una al cargar un gasto).
10. Solo un `ADMIN` puede **crear, renombrar y borrar** categorías. Un `USER` que lo intente recibe "prohibido".
11. El nombre de una categoría tiene entre 2 y 40 caracteres y es **único sin distinguir mayúsculas ni espacios al inicio o al final** ("Supermercado" y " supermercado " son el mismo nombre). El nombre se guarda **sin los espacios del inicio y del final**, respetando las mayúsculas que escribió el ADMIN.
12. **No se puede borrar una categoría que tiene gastos asociados.** El sistema lo rechaza con un mensaje que explica el motivo.
13. El sistema arranca con estas categorías cargadas: Supermercado, Transporte, Servicios, Salud, Ocio, Otros.

### Cuentas
16. **En producción no hay registro público.** Las cuentas las crea un `ADMIN` desde la administración de usuarios. Un intento de registrarse se rechaza y **no crea, modifica ni borra ningún usuario** (en particular, no reemplaza una cuenta desactivada). En los entornos de desarrollo y de pruebas (incluido el perfil de muestras que usan el CI y la demo) el registro sigue disponible. *(Decisión del humano a partir del verify del paso 000, riesgo R1 de `seguridad.md`.)*

### Interfaz
14. El menú principal tiene "Mis gastos" (listado con filtros y botón para nuevo gasto). La gestión de categorías aparece solo para `ADMIN`.
15. Los errores de validación se muestran junto al campo correspondiente, en español, y no se pierde lo que el usuario había escrito.

## 4. Casos borde
- Monto exactamente 0,01 → válido. Monto 0 → inválido. Monto 10.000.000 → válido. Monto 10.000.000,01 → inválido.
- Monto con 3 decimales (12,345) → inválido. No se redondea en silencio.
- Monto negativo → inválido (un gasto no es un ingreso).
- Fecha igual a hoy → válida. Fecha de mañana → inválida.
- Filtro con "desde" posterior a "hasta" → listado vacío, sin error.
- Pedir un gasto con un id que no existe y pedir uno de otro usuario dan **la misma** respuesta.
- Editar un gasto propio intentando cambiarle el dueño → se edita, pero el dueño sigue siendo el original.
- Dos categorías que difieren solo en mayúsculas → la segunda se rechaza.
- Borrar una categoría sin gastos → se permite.

## 5. Criterios de aceptación

| ID | Escenario | Entrada | Resultado esperado |
|---|---|---|---|
| 001-CA01 | Registrar gasto válido | Usuario `ana` carga monto 1500,50, fecha hoy, categoría Supermercado | Se crea; el dueño es `ana`; aparece primero en su listado |
| 001-CA02 | Monto inválido | Montos 0; -10; 12,345; 10.000.000,01 | Cada uno se rechaza con error de validación sobre el monto; no se crea nada |
| 001-CA03 | Fecha futura | Fecha = mañana | Se rechaza con error de validación sobre la fecha |
| 001-CA04 | Dueño forzado | `ana` crea un gasto indicando como dueño a `beto` | Se crea con dueño `ana` |
| 001-CA05 | Listado aislado | `ana` tiene 3 gastos y `beto` tiene 2; `ana` lista | `ana` ve exactamente sus 3 gastos |
| 001-CA06 | Filtro no escapa al dueño | `ana` filtra pidiendo explícitamente los gastos del usuario `beto` | Ve solo gastos propios (o ninguno), nunca los de `beto` |
| 001-CA07 | Ver gasto ajeno (IDOR) | `ana` pide el gasto con id de un gasto de `beto` | "No encontrado", idéntico a pedir un id inexistente |
| 001-CA08 | Editar/borrar gasto ajeno | `ana` intenta editar y borrar un gasto de `beto` | "No encontrado" en ambos; el gasto de `beto` queda intacto |
| 001-CA09 | Filtro por fecha y categoría | `ana` filtra del 1 al 31 del mes actual, categoría Transporte | Solo gastos de `ana`, de ese rango y de esa categoría, del más reciente al más antiguo |
| 001-CA10 | Sin sesión | Petición sin sesión a cualquier operación de gastos | "No autorizado" |
| 001-CA11 | USER gestiona categorías | `ana` (USER) intenta crear una categoría | "Prohibido" |
| 001-CA12 | Nombre duplicado | ADMIN crea " supermercado " cuando ya existe "Supermercado" | Se rechaza por nombre duplicado |
| 001-CA13 | Borrar categoría en uso | ADMIN borra Supermercado, que tiene gastos | Se rechaza con mensaje explicativo; la categoría sigue existiendo |
| 001-CA14 | Categorías iniciales | Sistema recién instalado | Existen las 6 categorías del punto 13 |
| 001-CA15 | Flujo E2E | En el navegador, `user` inicia sesión, carga un gasto, lo ve en "Mis gastos", lo edita y lo borra | Cada paso se refleja en pantalla |
| 001-CA16 | Registro deshabilitado | Con el registro deshabilitado (como en producción), alguien sin sesión intenta registrarse con el login de una cuenta desactivada existente | Se rechaza; la cuenta original sigue existiendo con los mismos datos y no se crea ninguna |

## 5.1 Contrato observable (para escribir los tests)
No describe *cómo* se implementa: fija lo que un test puede observar desde afuera (API y textos de la interfaz), para que el agente testing no tenga que inventarlo. Se agregó a pedido del agente testing durante la fase roja.

### API
| Situación | Respuesta |
|---|---|
| Validación de un campo (001-CA02, CA03) | **400**, *problem* de JHipster, con `fieldErrors[].field` = `monto` o `fecha` (aunque la regla se valide en el service) |
| Nombre de categoría duplicado (001-CA12) | **400**, *problem* con `message` = `error.nombreduplicado` |
| Borrar categoría en uso (001-CA13) | **400**, *problem* con `message` = `error.categoriaenuso` |
| Gasto ajeno o inexistente en GET, PUT, PATCH o DELETE (001-CA07, CA08) | **404** en todos los casos, también para un id inexistente. El cuerpo es igual en los dos casos, salvo los campos que contienen el id o la ruta (`instance`, `path`) |
| Sin sesión (001-CA10) | **401** |
| USER que escribe categorías (001-CA11) | **403** |
| Registro con el registro deshabilitado (001-CA16) | **401** |
| Conteo de gastos (`/api/gastos/count`, que genera JHipster) | Cuenta **solo los gastos del usuario autenticado**, con los mismos filtros. Si no, revela cuántos gastos tienen los demás (A01) |
| Respuesta de crear o editar un gasto | Incluye `user.id` y `user.login` del dueño (ningún otro dato del usuario: ni email ni nombre) |
| Orden del listado (punto 4) | El backend **impone siempre** fecha descendente y después id descendente. Si el cliente manda otro `sort`, se ignora |

### Interfaz (textos que usan los E2E)
| Elemento | Texto |
|---|---|
| Menú | "Mis gastos", "Resumen" (y "Categorías" solo para ADMIN) |
| Botón para crear | "Nuevo gasto" |
| Labels del formulario | "Monto", "Fecha", "Categoría", "Descripción" |
| Botones del formulario | "Guardar", "Cancelar" |
| Acciones por fila | "Editar", "Borrar" |
| Confirmación de borrado | Diálogo con el botón "Confirmar" |
| Formato de montos | `$ 1.500,50` (es-AR: punto de miles, coma decimal) |

## 6. Fuera de alcance
- Ingresos, saldos y cuentas bancarias.
- Múltiples monedas (todo es en pesos).
- Gastos compartidos entre usuarios.
- Gastos recurrentes o programados.
- Categorías propias de cada usuario (ver ADR-0007).
- Adjuntar comprobantes o fotos.
- Exportar a CSV o Excel.

## 7. Referencias
- Modelo: `tracker.jh` (`Gasto`, `Categoria`).
- [ADR-0007 — Propiedad de los datos](../../adr/ADR-0007-propiedad-de-los-datos.md)
- [Guía de seguridad](../../catedra/seguridad.md) (A01 Pérdida de control de acceso, validación de entradas)
- [Guía de testing](../../catedra/testing.md)
