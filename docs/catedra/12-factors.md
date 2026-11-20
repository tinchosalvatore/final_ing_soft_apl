# Guía: 12 factors (+3)

Responsable: el implementador de cada spec (es transversal), y el verificador lo controla en `verify.md`. La mayoría se resuelve en la generación base (000) y en la spec 004.

## 1. Qué se aplica y cómo

| Factor | Qué tiene que hacer el agente | Cómo se verifica |
|---|---|---|
| **I. Código base** | Un solo repo. Toda la configuración por entorno sale del mismo código; nada se copia a otro repo | Revisión |
| **II. Dependencias** | Versiones exactas en `pom.xml` y `package.json` (nada de `latest`, `*` ni rangos en las dependencias que agreguemos). Se usan `./mvnw` y `npm ci` (lockfile) | Trivy en el CI; revisión del diff |
| **III. Configuración** | Todo valor que cambia entre entornos se lee de una **variable de entorno** con un default seguro para dev. Lista mínima en la sección 2. `.env.example` versionado (sin valores reales), `.env` en `.gitignore` | Gitleaks; `grep` de URLs, IPs o claves en `src/main/resources/config/application-prod.yml` |
| **IV. Servicios de respaldo** | Postgres y Logstash se conectan solo por URL/host/puerto de configuración. Cambiar de base es cambiar una variable | Levantar la app apuntando a otro Postgres sin recompilar |
| **V. Build, release, run** | El CI hace el build. La misma imagen (`./mvnw -Pprod jib:dockerBuild`) corre en cualquier entorno, con su configuración inyectada al ejecutarse | `src/main/docker/app.yml` usa variables, no otra imagen |
| **VI. Procesos sin estado** | JWT stateless: no hay sesión HTTP en memoria. No guardar estado de usuario en campos estáticos ni en memoria entre peticiones | Revisión; reiniciar la app no desloguea a nadie mientras el JWT siga vigente |
| **VII. Asignación de puertos** | Servidor embebido. Puerto por `SERVER_PORT` (default 8080) | Levantar con `SERVER_PORT=9090` |
| **IX. Desechabilidad** | `server.shutdown: graceful` y un timeout de apagado (`spring.lifecycle.timeout-per-shutdown-phase: 20s`) en el perfil prod. Sin trabajos largos en memoria | Detener el contenedor durante una petición: termina antes de cerrar |
| **X. Paridad dev/prod** | Postgres en todos los entornos (ADR-0002), Testcontainers en los tests, ELK local con el mismo compose | `tracker.jh`: `devDatabaseType postgresql` |
| **XI. Logs** | La app escribe eventos estructurados y no gestiona archivos de log. Concesión documentada: el envío a Logstash lo hace el appender (ADR-0004) | Spec 004 |
| **XII. Procesos de administración** | Todo cambio de esquema o de datos base (categorías iniciales) es un **changelog de Liquibase** versionado. Nada a mano en la DB | `config/liquibase/changelog/` |
| **14. Telemetría** | Ver spec 004 y `logs-elk.md` | Spec 004 |
| **15. Autenticación y autorización** | Ver spec 001 y `seguridad.md`. El secreto JWT es configuración (III) | Spec 001 |

## 2. Variables de entorno mínimas (perfil prod)

| Variable | Para qué | Dónde se define |
|---|---|---|
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Conexión a Postgres | `.env` / `app.yml` |
| `JHIPSTER_SECURITY_AUTHENTICATION_JWT_BASE64_SECRET` | Firma de los JWT. **Distinto por entorno**; en prod nunca se usa el valor generado que trae el yml | `.env` |
| `JHIPSTER_LOGGING_LOGSTASH_ENABLED`, `JHIPSTER_LOGGING_LOGSTASH_HOST`, `JHIPSTER_LOGGING_LOGSTASH_PORT` | Envío de logs | `.env` / `app.yml` |
| `SERVER_PORT` | Puerto HTTP | opcional |
| `SPRING_PROFILES_ACTIVE` | `prod` / `dev` | `app.yml` |

El agente que agregue una variable la suma a esta tabla y a `.env.example` en el mismo commit.

## 3. Qué queda afuera y por qué
| Factor | Motivo |
|---|---|
| **VIII. Concurrencia** (workers y planificadores) | Monolito sin colas ni tareas en segundo plano: no hay procesos de otro tipo que escalar |
| Patrones **Saga / Outbox** | No hay transacciones distribuidas: una sola base y un solo servicio |
| **13. API first** | JHipster genera la API a partir del modelo (*code-first*). Se reconoce como no aplicado; el contrato queda documentado por el OpenAPI que genera JHipster |
| Kubernetes, ConfigMaps, Secrets de plataforma | Fuera del alcance de la materia; el equivalente local es `.env` + docker compose |

## 4. Referencias
ADR-0002, ADR-0004, `seguridad.md`, `logs-elk.md`, spec 004.
