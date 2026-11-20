# Plan — fix/001-patch (hallazgos del verify de la 001 y del 000)

- Autor: agente coder (sesión `coder`, 2026-11-20)
- Estado: Aprobado (documentador, 2026-11-20), con un cambio en `.yo-resolve` (ver §4)
- Rama: `fix/001-patch`. Tests en rojo del agente testing: `0b11efe`.

## 1. Cambios

| # | Hallazgo | Archivo(s) | Cambio |
|---|---|---|---|
| 1 | **O1**: un PATCH con descripción de 141 caracteres llega a la base y da 500 | `service/GastoService.java` | `validar(…)` recibe también la descripción: más de 140 → `CampoInvalidoException("descripcion")` → 400 con `fieldErrors`. En PATCH se valida el valor final (el del DTO o el que ya tenía). POST y PUT ya lo validaba Bean Validation con `@Size`; ahora lo valida también el service, que es la autoridad (S4) |
| 2 | **R2**: el hash de S5 se aplica una sola vez | `config/liquibase/changelog/20261120000000_prod_usuarios_por_defecto.xml` | `runOnChange="true"` en el changeset. Liquibase sustituye `${adminPasswordHash}` antes de calcular el checksum: si cambia `TRACKER_ADMIN_PASSWORD_HASH`, el changeset vuelve a correr y la contraseña de `admin` se rota con la variable. Si la variable no cambia, no corre, aunque `admin` haya cambiado la contraseña desde la UI. La precondición `HALT` queda igual. **No se modifica el changeset ya aplicado en otras bases:** el checksum de esas bases cambia con este atributo y Liquibase lo vuelve a correr una vez, con el mismo hash, así que el efecto es nulo |
| 3 | **O6 del 000**: `spring.mail.host: localhost` fijo en prod | `config/application-prod.yml`, `.env.example`, `docs/catedra/12-factors.md` §2 | `host: ${SPRING_MAIL_HOST:localhost}` (el mail no es crítico: sin servidor, la app arranca igual). La variable va documentada en `.env.example` y en la tabla de §2 |
| 4 | **O2**: `*.log*` ignora archivos como `gastos.logica.ts` | `.gitignore` | `*.log*` pasa a `*.log` y `*.log.*` |
| 5 | Que una regeneración no pise lo propio | `.yo-resolve` (nuevo) | Una línea `<ruta> skip` por cada archivo de los ADR-0007 y ADR-0008 (lista en §2). Yeoman lee `.yo-resolve` y responde "omitir" sin preguntar |
| 6 | **001-CA17 (O3)**: no se puede borrar un usuario con gastos | `service/UserService.java`, `i18n/es/reglas.json` | `deleteUser`: si el usuario tiene gastos (`GastoSpecifications.delDueno(login)`) → `ReglaDeNegocioException("usuario", "usuariocongastos")` → 400 `error.usuariocongastos`. No borra nada. El mensaje en español va en `reglas.json`, así la pantalla de administración de usuarios lo muestra en el toast |

## 2. Contenido de `.yo-resolve`

**Solo territorio del desarrollador**, es decir, archivos generados que reescribimos:
- Los cinco del ADR-0007: `GastoService`, `GastoQueryService`, `GastoResource`, `CategoriaService` y `CategoriaResource`.
- `CategoriaRepository.java`, `GastoResourceIT.java` y `CategoriaResourceIT.java`.
- `header.spec.tsx`, `e2e/entity/gasto.pw.ts` y `e2e/entity/categoria.pw.ts`.

## 3. Dependencias nuevas

Ninguna.

## 4. Decisiones (resueltas por el documentador, 2026-11-20)

1. **`master.xml` no va en `.yo-resolve`.** Es territorio compartido: se respetan los needles, nuestros `include` van arriba y JHipster agrega los suyos debajo. Si se omitiera, JHipster dejaría de incluir en silencio los changelogs de entidades nuevas.
   - Por la misma regla tampoco van en `.yo-resolve` los `application-*.yml`, `header.tsx`, `routes.tsx`, `ExceptionTranslator`, `UserService` ni `ApplicationProperties`.
   - Ante un conflicto se responde `d` y se fusiona a mano.
2. **El ADR-0008 se actualiza en este PR:** `.yo-resolve` es el mecanismo concreto, y solo para territorio del desarrollador.

## 5. Riesgos

- `runOnChange` vuelve a desactivar a `user` en cada corrida, lo que es intencional. Y si alguien reactiva a `user` desde la UI, la próxima rotación lo vuelve a desactivar.
- `.yo-resolve` con `skip` hace que JHipster no aplique sus propias correcciones a esos archivos al actualizar de versión. Es el costo aceptado en los ADR-0007 y ADR-0008.
