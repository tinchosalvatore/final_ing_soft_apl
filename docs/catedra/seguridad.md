# Guía: seguridad (OWASP)

Responsable: el agente de cada tema implementa; el agente **seguridad** (verificador) revisa la sección 2 en cada PR y es el interlocutor para estudiar el tema. La sección 4 la usa el humano para la defensa.

## 1. Enfoque
Hay dos capas, y el proyecto tiene que mostrar las dos:
1. **Defensas escritas en el código**: lugares concretos donde una regla del sistema neutraliza un ataque conocido, con un test que lo demuestra. Son las del punto 2.
2. **Controles automáticos en el CI**: herramientas que buscan problemas conocidos en cada cambio (Gitleaks, Semgrep, Trivy). Ver `ci.md`.

## 2. Defensas que se implementan en el código

| ID | Defensa | Contra qué ataque (OWASP 2025) | Dónde | Spec / test |
|---|---|---|---|---|
| **S1** | **Propiedad del gasto verificada en el backend.** Toda lectura, edición o borrado busca el gasto por `id` **y** dueño = usuario autenticado. Si no coincide, responde 404 (igual que si no existiera) | **A01 Pérdida de control de acceso / IDOR** (cambiar el id en la URL para ver datos de otro) | `GastoService` (buscar por id y login del usuario actual, vía `SecurityUtils.getCurrentUserLogin()`) | 001-CA07, CA08 |
| **S2** | **El filtro siempre agrega "dueño = yo".** Cualquier criterio de filtrado que mande el cliente se combina con el usuario actual; el criterio de usuario que venga del cliente se pisa | **A01** (pedir `?userId.equals=<otro>` en el endpoint de filtros que genera JHipster) | `GastoQueryService` | 001-CA06 |
| **S3** | **El dueño lo asigna el servidor.** Al crear o editar se ignora el `user` del DTO recibido | **Manipulación (STRIDE) / asignación masiva**: mandar en el JSON un campo que el usuario no debería controlar | `GastoService.save/update` | 001-CA04 |
| **S4** | **Validación de entrada en el backend** con Bean Validation y reglas de negocio en el service (monto, fecha, largos, mes del resumen). El front valida para la experiencia de uso, pero la autoridad es el backend | **Principio 1 de codificación segura** y **A06 Diseño inseguro**: límites de negocio definidos (monto máximo, fechas válidas) | DTOs + `GastoService` + servicio de resumen | 001-CA02, CA03; 002-CA10 |
| **S5** | **Sin contraseñas por defecto en prod.** JHipster carga siempre los usuarios `admin/admin` y `user/user` desde los CSV del changelog inicial. En prod, `user` queda desactivado y la contraseña de `admin` se reemplaza por una que viene de una variable de entorno. El mecanismo concreto (por ejemplo, un changeset de Liquibase con contexto `prod`) lo decide el `plan.md` del paso 000 | **A02 Configuración de seguridad incorrecta** | Liquibase (contextos) + configuración prod | Revisión + `verify.md` de 000 |
| **S6** | **Secreto JWT fuera del código**, distinto por entorno | **A04 Fallas criptográficas** / gestión de secretos | `application-prod.yml` → variable de entorno | Gitleaks |
| **S7** | **Errores sin filtrar información interna.** El manejador global de JHipster (`ExceptionTranslator`) devuelve mensajes genéricos (sin stack, clases ni SQL); el detalle va solo al log | **A10 Manejo inadecuado de condiciones excepcionales** y principio 7 (errores y logging) | `ExceptionTranslator` (perfil prod) | 004-CA05 |
| **S8** | **Registro de eventos de seguridad sin datos sensibles** (`LOGIN_FALLIDO`, `GASTO_AJENO`, `ACCESO_DENEGADO`) y alerta por intentos de login fallidos | **A09 Fallas en el registro y monitoreo** y **A07 Fallas de autenticación** (detectar fuerza bruta) | Spec 004 | 004-CA01, CA03, CA08 |
| **S9** | **Limpieza de datos locales al cerrar sesión** (caché de la API e IndexedDB) | **Exposición de datos sensibles** en dispositivos compartidos | Módulo offline (`app/shared/offline/`) | 003-CA09, CA10 |
| **S10** | **Categorías: solo ADMIN escribe** (`@PreAuthorize`) | **A01** (escalada de privilegios) | `CategoriaResource` | 001-CA11 |

S1, S2 y S3 son **las defensas en código para mostrar en la defensa oral**: ahí el código generado por JHipster era vulnerable y se corrigió a propósito.

Lo que ya trae JHipster y **no hay que romper**: contraseñas con **BCrypt** (A04), consultas con JPA/Criteria sin concatenar SQL (A05 Inyección), headers de seguridad y CSP (A02), escape de salida de React (XSS, principio 2).

## 3. Modelo de amenazas (STRIDE) del tracker

| Amenaza | Ejemplo en esta app | Mitigación |
|---|---|---|
| **S**uplantación | Robar o adivinar credenciales | BCrypt, JWT con vencimiento, alerta por logins fallidos (S8) |
| **T** Manipulación | Mandar otro `user` u otro `id` en la petición | S1, S2, S3 |
| **R**epudio | "Yo no borré ese gasto" | `GASTO_BORRADO` con usuario y traza (spec 004) |
| **I** Filtración de información | Stack trace en un error; gastos de otro en caché | S7, S9, logs sin datos sensibles |
| **D**enegación de servicio | Inundar de peticiones | **Fuera de alcance** (sin rate limiting; se reconoce) |
| **E**levación de privilegios | Un USER que administra categorías | S10 |

## 4. Mapa OWASP Top 10 2025 → proyecto (para la defensa)

| OWASP 2025 | Cómo se cubre |
|---|---|
| A01 Pérdida de control de acceso | S1, S2, S3, S10 + tests |
| A02 Configuración incorrecta | S5, perfil prod, headers de JHipster |
| A03 Cadena de suministro | Trivy (SCA) en el CI, versiones fijas, acciones de GitHub fijadas a un SHA |
| A04 Fallas criptográficas | BCrypt (JHipster), S6, HTTPS en un despliegue real |
| A05 Inyección | JPA/Criteria (sin SQL concatenado) + Semgrep en el CI |
| A06 Diseño inseguro | S4: límites de negocio explícitos en las specs |
| A07 Fallas de autenticación | S8 (detección). MFA y bloqueo por reintentos: fuera de alcance |
| A08 Integridad de software y datos | Lockfiles, `npm ci`, acciones fijadas a un SHA |
| A09 Registro y monitoreo | S8, spec 004 |
| A10 Condiciones excepcionales | S7; operaciones de escritura dentro de transacciones (`@Transactional`) |

## 5. Qué queda afuera
- OWASP Top 10 para aplicaciones con LLM (la app no usa LLMs).
- MFA, bloqueo de cuenta o retardo entre reintentos de login, chequeo de contraseñas filtradas.
- Rate limiting (denegación de servicio).
- DAST (OWASP ZAP) y escaneo de imágenes de contenedor.
- Cifrado de datos en reposo (más allá de las contraseñas hasheadas).
- Principios 10 a 12 de codificación segura (configuración del sistema, seguridad de la DB, archivos).

## 6. Referencias
ADR-0007, specs 001, 003 y 004, `ci.md`, `logs-elk.md`.
