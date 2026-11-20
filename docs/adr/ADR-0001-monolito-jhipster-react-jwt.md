# ADR-0001: Monolito JHipster con React y JWT

- Estado: Aceptado
- Fecha: 2026-11-20

## Contexto
- El enunciado exige una aplicación generada con JHipster.
- El dominio es chico (gastos, categorías, usuarios), hay un solo desarrollador y menos de una semana.
- La PWA tiene que hacerse con **Workbox**, como pidió la cátedra. En JHipster 9.4, Angular trae su propio service worker (`@angular/service-worker`, no Workbox). React y Vue compilan con Vite, donde Workbox se integra directo.

## Decisión
- Aplicación **monolítica** generada con JHipster 9.4 a partir de `tracker.jh`.
- Frontend **React + TypeScript**.
- Autenticación **JWT** (la opción por defecto de JHipster).
- Arquitectura en capas oficial de JHipster: Resource → Service → Repository → Entity, con DTOs y MapStruct.

## Consecuencias
- Un solo artefacto desplegable: un solo pipeline y un solo `docker compose`.
- El escalado sería vertical, cosa que alcanza para este alcance.
- JWT stateless: el servidor no guarda sesiones en memoria (factor VI), pero el token vive en el navegador, así que la PWA tiene que limpiar los datos locales al cerrar sesión (spec 003).
- La lógica de negocio va en los `*Service` (territorio del desarrollador), nunca en DTOs ni mappers (territorio del generador).

## Alternativas
- **Microservicios / gateway:** mucha infraestructura para un dominio de dos entidades. Descartado.
- **Angular:** su service worker no es Workbox; habría que reemplazarlo. Descartado.
- **Vue:** sería igual de viable con Vite. Se eligió React por tener más material de referencia.
- **Sesión con cookies u OAuth2:** OAuth2 suma un proveedor de identidad (Keycloak) que no aporta al alcance. Descartado.
