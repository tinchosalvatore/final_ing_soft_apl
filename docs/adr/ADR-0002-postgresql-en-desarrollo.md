# ADR-0002: PostgreSQL también en desarrollo (no H2)

- Estado: Aceptado
- Fecha: 2026-11-20

## Contexto
JHipster sugiere H2 en desarrollo y PostgreSQL en producción. El factor X de 12 factors (paridad dev/prod) pone justamente ese caso como mal ejemplo: un motor distinto en cada entorno esconde errores (tipos, SQL específico, migraciones) que aparecen recién en producción.

## Decisión
`devDatabaseType postgresql` y `prodDatabaseType postgresql` en `tracker.jh`. En desarrollo, Postgres corre en un contenedor (`src/main/docker/postgresql.yml`, que la app levanta sola con el soporte de docker compose de Spring Boot). Los tests de integración usan Testcontainers con Postgres.

## Consecuencias
- Los mismos changelogs de Liquibase se prueban siempre contra el mismo motor.
- Desarrollar requiere Docker (también lo requieren ELK y los tests, así que no es un costo nuevo).
- El arranque en dev es unos segundos más lento que con H2.

## Alternativas
- **H2 en dev:** más rápido, pero rompe la paridad. Descartado.
- **Postgres instalado en la máquina:** depende de la configuración de cada uno y no es reproducible. Descartado.
