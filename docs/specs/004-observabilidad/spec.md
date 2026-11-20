# Spec 004 — Observabilidad (logs, dashboard y alerta)

- Estado: Aprobada para planificar
- Rama: `spec/004-observabilidad`
- Depende de: 001, 002

## 1. Contexto y objetivo
Hoy, si algo falla en el servidor o alguien intenta adivinar contraseñas, nadie se entera. El objetivo es que el equipo pueda **reconstruir qué pasó**, **ver la salud del sistema de un vistazo** y **enterarse solo** cuando hay un patrón de ataque a las cuentas. Todo esto sin que los registros se conviertan en una filtración de datos personales o financieros.

## 2. Definiciones
| Término | Significado |
|---|---|
| **Evento** | Algo que pasó en el sistema y queda registrado con fecha y hora. |
| **Registro (log)** | La línea estructurada que describe un evento. |
| **Identificador de traza** | Valor que comparten todos los registros generados por una misma petición, para poder seguirla de punta a punta. |
| **Dashboard de salud** | Pantalla que muestra el estado del sistema con gráficos que se actualizan solos. |
| **Alerta** | Aviso automático que se dispara cuando se cumple una condición sobre los registros. |
| **Dato sensible** | Contraseñas, tokens, montos y descripciones de gastos. |

## 3. Comportamiento esperado

### Qué se registra
1. Todo registro incluye: fecha y hora, nivel (`INFO`, `WARN` o `ERROR`), aplicación, identificador de traza, usuario (si hay sesión), un código de evento y un mensaje legible.
2. Se registran estos eventos:

| Código de evento | Nivel | Cuándo | Datos además de los comunes |
|---|---|---|---|
| `LOGIN_OK` | INFO | Inicio de sesión exitoso | login |
| `LOGIN_FALLIDO` | WARN | Credenciales incorrectas | login intentado, IP de origen |
| `ACCESO_DENEGADO` | WARN | Alguien intenta una operación sin permiso (ej.: un USER crea una categoría) | operación |
| `GASTO_AJENO` | WARN | Alguien pide, edita o borra un gasto que no es suyo | id del gasto, operación |
| `GASTO_CREADO` / `GASTO_EDITADO` / `GASTO_BORRADO` | INFO | Operación exitosa sobre un gasto | id del gasto |
| `CATEGORIA_CREADA` / `CATEGORIA_EDITADA` / `CATEGORIA_BORRADA` | INFO | Operación exitosa sobre una categoría | id de la categoría |
| `PETICION_HTTP` | INFO | Cada petición a `/api/**` al terminar | método, ruta, código de estado, duración en ms |
| `ERROR_INESPERADO` | ERROR | Excepción no controlada | tipo de error (el detalle técnico solo en el registro, nunca en la respuesta al cliente) |

3. **Nunca** se registran datos sensibles: ni contraseñas (tampoco las incorrectas), ni tokens, ni montos ni descripciones de gastos.
4. Los registros se centralizan en un único lugar donde se pueden buscar y filtrar por cualquiera de sus campos.

### Qué se puede ver
5. Se pueden buscar todos los registros de una petición a partir de su identificador de traza.
6. Hay un **dashboard de salud** con, como mínimo:
   - Peticiones por minuto.
   - Porcentaje de respuestas con error (códigos 5xx).
   - Duración de las peticiones (promedio y percentil 95) por ruta.
   - Logins fallidos en el tiempo.
   - Cantidad de registros por nivel en el tiempo.
7. Hay una **alerta** que se dispara cuando hay **5 o más `LOGIN_FALLIDO` en 5 minutos**, y que deja constancia de que se disparó (en el panel de alertas).

## 4. Casos borde
- Un login fallido con un usuario que no existe → se registra igual que con uno que existe (mismo nivel y mismo formato), para no revelar qué usuarios existen.
- El sistema de registros centralizado está caído → la app sigue funcionando y sigue mostrando los registros en su consola.
- 4 logins fallidos en 5 minutos → no hay alerta. Al 5.º → sí.
- Error inesperado → el cliente recibe un mensaje genérico (sin nombres de clases, rutas ni SQL) y el registro tiene el detalle.
- Petición sin sesión → el registro no tiene usuario, pero sí traza.

## 5. Criterios de aceptación

| ID | Escenario | Entrada | Resultado esperado |
|---|---|---|---|
| 004-CA01 | Login fallido registrado | Login de `ana` con contraseña incorrecta | Registro `LOGIN_FALLIDO`, nivel WARN, con login e IP; sin la contraseña |
| 004-CA02 | Sin datos sensibles | Crear un gasto de monto 1234,56 con descripción "farmacia X" | Hay un registro `GASTO_CREADO` con el id y ninguno contiene "1234" ni "farmacia" |
| 004-CA03 | Gasto ajeno registrado | `ana` pide un gasto de `beto` | Registro `GASTO_AJENO` WARN con el usuario `ana` y el id del gasto |
| 004-CA04 | Traza | Una petición de creación de gasto | Todos sus registros (`PETICION_HTTP`, `GASTO_CREADO`) comparten el mismo identificador de traza y se encuentran buscándolo |
| 004-CA05 | Error sin filtración | Forzar un error inesperado | La respuesta no tiene stack trace ni nombres internos; hay un registro `ERROR_INESPERADO` con el detalle |
| 004-CA06 | Centralización | Con el sistema de registros levantado, usar la app | Los registros aparecen en la búsqueda centralizada en menos de 1 minuto |
| 004-CA07 | Dashboard | Abrir el dashboard de salud con tráfico reciente | Se ven los 5 paneles del punto 6 con datos |
| 004-CA08 | Alerta | 5 logins fallidos en menos de 5 minutos | La alerta se dispara y queda registrada |
| 004-CA09 | Sin alerta por debajo del umbral | 4 logins fallidos en 5 minutos | La alerta no se dispara |
| 004-CA10 | Tolerancia a caída | Sistema de registros apagado, usar la app | La app responde normalmente; los registros salen por consola |

## 6. Fuera de alcance
- Política de retención y borrado de registros.
- Notificaciones por correo o chat (alcanza con que la alerta quede registrada en el panel).
- Trazas distribuidas entre servicios (hay un solo servicio) y métricas de infraestructura (CPU, memoria).
- Seguridad del propio sistema de registros (cifrado, usuarios): es un entorno local o de demo.
- Registros del frontend.

## 7. Referencias
- [ADR-0004 — Appender de Logstash](../../adr/ADR-0004-logs-appender-logstash.md)
- [Guía de logs y ELK](../../catedra/logs-elk.md)
- [Guía de seguridad](../../catedra/seguridad.md) (A09 Fallas de registro y monitoreo, A10 Condiciones excepcionales)
