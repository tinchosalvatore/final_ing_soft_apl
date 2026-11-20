# Spec 003 — PWA y carga de gastos sin conexión

- Estado: Aprobada para planificar
- Rama: `spec/003-pwa-offline`
- Depende de: 001, 002 (y 005 andando)

## 1. Contexto y objetivo
Los gastos se hacen en la calle: en el colectivo, en un kiosco, en un lugar sin señal. Si para anotar un gasto hace falta conexión, el usuario se olvida de anotarlo y el registro deja de ser confiable.

Objetivo: que la app **se pueda instalar** en el celular o la computadora, que **abra y muestre lo último que se vio aunque no haya conexión**, y que **se puedan cargar gastos sin conexión** que se envían solos cuando vuelve la red.

## 2. Definiciones
| Término | Significado |
|---|---|
| **Sin conexión** | El dispositivo no llega al servidor (sin red o servidor inalcanzable). |
| **Gasto pendiente** | Gasto cargado sin conexión, guardado solo en el dispositivo, que todavía no llegó al servidor. |
| **Sincronizar** | Enviar los gastos pendientes al servidor. |
| **Gasto rechazado** | Gasto pendiente que el servidor no aceptó por no cumplir las reglas de la spec 001. |
| **Datos guardados** | La última versión de una pantalla (listado de gastos, categorías, resumen) que se vio con conexión. |

## 3. Comportamiento esperado

### Instalación y apertura
1. La app se puede **instalar** desde el navegador (escritorio y Android) y abre en su propia ventana, con nombre "Tracker de Gastos" e ícono propio.
2. Después de haberla abierto una vez con conexión, la app **abre sin conexión** (la interfaz carga, no aparece el error del navegador).
3. La interfaz se puede usar en un celular de 360 px de ancho sin scroll horizontal.

### Consulta sin conexión
4. Sin conexión, "Mis gastos", el resumen y la lista de categorías muestran los **datos guardados** de la última vez que se vieron con conexión, con un aviso visible: "Sin conexión: mostrando datos guardados".
5. Si una pantalla nunca se vio con conexión, sin conexión muestra "Esta información no está disponible sin conexión" en lugar de un error.
6. Con conexión, siempre se muestran los datos del servidor (no los guardados).

### Carga sin conexión
7. Sin conexión, el usuario puede **cargar un gasto nuevo** con el mismo formulario. Se aplican las mismas validaciones de la spec 001 que no necesitan al servidor (monto, fecha, largo de la descripción, categoría elegida de la lista guardada).
8. El gasto queda como **pendiente** y se ve en "Mis gastos" marcado como "Pendiente de sincronizar". Un indicador muestra cuántos pendientes hay.
9. Cuando vuelve la conexión, o cuando se abre la app con conexión, **con la sesión iniciada**, los pendientes se sincronizan solos, en el orden en que se cargaron.
10. Si un pendiente se acepta, deja de figurar como pendiente y aparece como un gasto normal.
11. Si un pendiente se **rechaza** (por ejemplo, porque la categoría fue borrada mientras tanto), queda marcado como "Rechazado" con el motivo, y el usuario puede **corregirlo y reintentar** o **descartarlo**.
12. Si la sesión venció, los pendientes **no se pierden**: esperan a que el usuario vuelva a iniciar sesión y se sincronizan después.
13. Sin conexión **no se puede editar ni borrar** gastos ya sincronizados: esas acciones aparecen deshabilitadas, con el motivo.

### Privacidad y sesión
14. Al **cerrar sesión** se borran del dispositivo todos los datos guardados y los pendientes. Si hay pendientes, antes de cerrar se avisa: "Tenés N gastos sin sincronizar. Si cerrás sesión se van a perder." y el usuario confirma o cancela.
15. Los datos guardados y los pendientes de un usuario nunca se muestran a otro usuario que inicie sesión en el mismo dispositivo.

### Actualizaciones
16. Cuando se publica una versión nueva de la app, se activa la próxima vez que el usuario la abre. No hace falta reinstalar.

## 4. Casos borde
- Se corta la conexión con el formulario a medio llenar → al guardar, el gasto queda pendiente (no se pierde lo escrito).
- Se cargan 3 pendientes y vuelve la conexión → se envían los 3, en orden; si el 2.º se rechaza, el 1.º y el 3.º igual se aceptan.
- Vuelve la conexión, pero la sesión venció → los pendientes siguen ahí; se sincronizan después del login.
- La conexión vuelve y se corta en medio de la sincronización → lo que no se confirmó sigue pendiente y se reintenta después. Cada pendiente se crea **una sola vez** en el servidor si se recibió la confirmación; si se cortó justo después de que el servidor lo guardara y antes de recibir la respuesta, puede duplicarse (limitación aceptada, ver "Fuera de alcance").
- Un pendiente con fecha de hoy que se sincroniza pasada la medianoche → sigue siendo válido (la fecha ya no es futura).
- Usuario A cierra sesión con pendientes y confirma → los pendientes se borran; el usuario B que entra después no ve nada de A.

## 5. Criterios de aceptación

| ID | Escenario | Entrada | Resultado esperado |
|---|---|---|---|
| 003-CA01 | Instalable | Abrir la app en Chromium | El navegador la reconoce como instalable (manifest válido y service worker activo) |
| 003-CA02 | Abre sin conexión | Abrir la app con conexión, después cortar la red y recargar | La interfaz carga; no aparece la página de error del navegador |
| 003-CA03 | Consulta sin conexión | Ver "Mis gastos" con conexión, cortar la red y volver a "Mis gastos" | Se ven los mismos gastos y el aviso "Sin conexión: mostrando datos guardados" |
| 003-CA04 | Carga sin conexión | Sin red, cargar un gasto válido | Aparece en el listado como "Pendiente de sincronizar"; el indicador muestra 1 |
| 003-CA05 | Sincronización | Con 2 pendientes, restablecer la red | Ambos llegan al servidor una vez cada uno y dejan de figurar como pendientes |
| 003-CA06 | Rechazo | Pendiente con una categoría que un ADMIN borró antes de sincronizar | Queda "Rechazado" con el motivo; se puede corregir y reenviar o descartar |
| 003-CA07 | Sesión vencida | Pendiente y sesión vencida al volver la red | El pendiente se mantiene; tras iniciar sesión se sincroniza |
| 003-CA08 | Validación offline | Sin red, monto 0 o fecha futura | Error de validación igual que con conexión; no se crea el pendiente |
| 003-CA09 | Logout limpia datos | Con datos guardados y 1 pendiente, cerrar sesión y confirmar | Aviso de pendientes; después no quedan datos de la API ni pendientes en el dispositivo |
| 003-CA10 | Aislamiento en dispositivo | `ana` cierra sesión y entra `beto` en el mismo navegador, sin red | `beto` no ve ningún dato de `ana` |
| 003-CA11 | Mobile | Viewport 360×740 | "Mis gastos", el formulario y el resumen se usan sin scroll horizontal |

## 5.1 Contrato observable (para escribir los tests)
Lo que un test puede observar desde afuera. Lo completó el documentador antes de la fase roja.

### Instalación y almacenamiento
| Elemento | Valor |
|---|---|
| Manifest | `/manifest.webmanifest`: `name` "Tracker de Gastos", `short_name` "Gastos", `display: standalone`, `start_url: /`, íconos de 192 y 512 px (uno `maskable`) |
| Service worker | Registrado con scope `/`, **solo en el build de producción**. Los E2E de esta spec corren contra el jar de prod (como el CI) |
| Caché de la API | Nombre `api-v1` en Cache Storage |
| Base offline | IndexedDB `tracker-offline`, versión 1, *object store* `pendientes` con índice `login` |

### Interfaz (textos que usan los E2E)
| Elemento | Texto |
|---|---|
| Aviso de datos guardados (punto 4) | "Sin conexión: mostrando datos guardados" |
| Pantalla nunca vista (punto 5) | "Esta información no está disponible sin conexión" |
| Marca de un pendiente (punto 8) | "Pendiente de sincronizar" |
| Indicador de pendientes | "1 pendiente" / "N pendientes" (no se muestra con 0) |
| Marca de un rechazado (punto 11) | "Rechazado: <motivo del servidor>" |
| Acciones sobre un rechazado | Botones "Reintentar" y "Descartar" |
| Editar o borrar sin conexión (punto 13) | Botones deshabilitados con el texto de ayuda "No disponible sin conexión" |
| Aviso al cerrar sesión (punto 14) | "Tenés N gastos sin sincronizar. Si cerrás sesión se van a perder.", con los botones "Cerrar sesión igual" y "Cancelar" |

## 6. Fuera de alcance
- Sincronizar con la app cerrada (sincronización en segundo plano). Ver ADR-0005.
- Editar o borrar gastos sin conexión.
- Garantía de "exactamente una vez" ante un corte justo después de que el servidor guardara el gasto (requeriría claves de idempotencia).
- Notificaciones push.
- Avisar "hay una versión nueva, ¿recargar?".
- Instalación en iOS y sus particularidades.

## 7. Referencias
- [ADR-0005 — PWA con Workbox e IndexedDB](../../adr/ADR-0005-pwa-workbox-indexeddb.md)
- [Guía PWA](../../catedra/pwa.md)
- [Spec 001](../001-gastos/spec.md) (reglas de validación de un gasto)
- [Guía de seguridad](../../catedra/seguridad.md) (datos locales al cerrar sesión)
