# ELK — logs centralizados (spec 004)

Elasticsearch, Logstash y Kibana **9.5.5**, para entorno local o de demo: sin seguridad de Elastic, sin retención (ILM). Decisión en [ADR-0004](../../../../docs/adr/ADR-0004-logs-appender-logstash.md).

```
app (appender Logstash, JSON por TCP 5000) → Logstash (sin filtros) → Elasticsearch (tracker-logs-AAAA.MM.DD) → Kibana
```

| Archivo                 | Qué es                                                                                                               |
| ----------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `../elk.yml`            | Los tres servicios                                                                                                   |
| `logstash.conf`         | Pipeline: entrada TCP 5000 `json_lines`, salida al índice `tracker-logs-%{+YYYY.MM.dd}`                              |
| `kibana/objetos.ndjson` | Data view `tracker-logs-*`, búsqueda "Eventos de seguridad" y dashboard "Salud – Tracker de Gastos" (5 paneles Lens) |
| `probar-alerta.sh`      | Hace N logins fallidos para probar la alerta                                                                         |

## 1. Levantar ELK

Todo desde la raíz del repo.

1. En `.env` (copiado de `.env.example`), completar `KIBANA_ENCRYPTION_KEY`. Kibana la necesita para las reglas de alerta:
   ```bash
   echo "KIBANA_ENCRYPTION_KEY=$(openssl rand -hex 32)" >> .env
   ```
2. Levantar los servicios. Kibana tarda entre 1 y 2 minutos:
   ```bash
   docker compose --env-file .env -f src/main/docker/elk.yml up -d
   curl -s localhost:5601/api/status | grep -o '"level":"[a-z]*"' | head -1   # "available"
   ```
3. Importar los objetos de Kibana: data view, búsqueda y dashboard.
   ```bash
   curl -s -X POST 'localhost:5601/api/saved_objects/_import?overwrite=true' \
     -H 'kbn-xsrf: true' --form file=@src/main/docker/elk/kibana/objetos.ndjson
   ```
   También se pueden importar a mano: _Stack Management → Saved Objects → Import_.
4. Crear la regla de alerta "Logins fallidos (5 en 5 min)". Es de tipo _Elasticsearch query_: cuenta los `LOGIN_FALLIDO` de los últimos 5 minutos, cada 1 minuto, y se dispara con 5 o más:
   ```bash
   curl -s -X POST 'localhost:5601/api/alerting/rule/logins-fallidos' \
     -H 'kbn-xsrf: true' -H 'Content-Type: application/json' -d '{
     "name": "Logins fallidos (5 en 5 min)",
     "rule_type_id": ".es-query",
     "consumer": "stackAlerts",
     "schedule": { "interval": "1m" },
     "tags": ["spec-004", "seguridad"],
     "params": {
       "searchType": "esQuery",
       "index": ["tracker-logs-*"],
       "timeField": "@timestamp",
       "esQuery": "{\"query\":{\"match_phrase\":{\"evento\":\"LOGIN_FALLIDO\"}}}",
       "threshold": [5],
       "thresholdComparator": ">=",
       "timeWindowSize": 5,
       "timeWindowUnit": "m",
       "size": 100
     },
     "actions": []
   }'
   ```
   La regla no tiene acciones: la constancia de que se disparó queda en el panel de alertas (spec 004, punto 7).

## 2. Levantar la app mandando los logs a Logstash

El appender se activa por variables de entorno (ADR-0004). La consola sigue andando igual.

```bash
JHIPSTER_LOGGING_LOGSTASH_ENABLED=true ./mvnw        # dev; host y puerto por defecto: localhost:5000
```

En prod se configuran `JHIPSTER_LOGGING_LOGSTASH_ENABLED`, `_HOST` y `_PORT` (ver `.env.example`).

## 3. Qué mostrar (defensa)

Abrir Kibana en <http://localhost:5601>. Antes, generar tráfico: login, ver y crear gastos, pedir un gasto de otro usuario, algún login fallido.

| Qué                                   | Dónde                                                                                                                                                                                                                    | Criterio |
| ------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------- |
| **Logs centralizados**                | _Discover_, data view `tracker-logs-*`. Aparecen en segundos                                                                                                                                                             | 004-CA06 |
| **Seguir una petición por `traceId`** | En Discover, abrir un registro `GASTO_CREADO`, copiar su `traceId` y buscar `traceId : "<valor>"`. Aparecen ese evento y su `PETICION_HTTP`                                                                              | 004-CA04 |
| **Eventos de seguridad**              | _Discover → Open → "Eventos de seguridad"_ (`LOGIN_FALLIDO`, `GASTO_AJENO`, `ACCESO_DENEGADO`)                                                                                                                           | S8       |
| **Dashboard de salud**                | _Dashboards → "Salud – Tracker de Gastos"_: peticiones por minuto, % de 5xx, duración promedio y p95 por ruta, logins fallidos, registros por nivel. Se actualiza cada 30 s                                              | 004-CA07 |
| **Alerta sin disparar**               | `./src/main/docker/elk/probar-alerta.sh 4` y esperar 1 o 2 minutos: en _Stack Management → Rules → "Logins fallidos (5 en 5 min)"_, la regla corre y no tiene alertas activas                                            | 004-CA09 |
| **Alerta disparada**                  | `./src/main/docker/elk/probar-alerta.sh 5` (o uno más después de los 4 anteriores, dentro de los 5 minutos) y esperar 1 o 2 minutos: la regla queda con una alerta _Active_, visible también en _Observability → Alerts_ | 004-CA08 |
| **Tolerancia a caída**                | `docker compose --env-file .env -f src/main/docker/elk.yml stop logstash`: la app sigue respondiendo y los logs siguen saliendo por consola                                                                              | 004-CA10 |

`probar-alerta.sh` usa por defecto `APP_URL=http://localhost:8080` y `LOGIN=user`. Para CA09, que no haya otros logins fallidos en los 5 minutos previos.

## 4. Apagar

```bash
docker compose --env-file .env -f src/main/docker/elk.yml down        # conserva los datos (volumen elk-datos)
docker compose --env-file .env -f src/main/docker/elk.yml down -v     # borra también los logs
```

## 5. Regenerar `objetos.ndjson`

Si se cambia el dashboard en Kibana, exportarlo de nuevo con _Stack Management → Saved Objects_: seleccionar el dashboard y la búsqueda → _Export_ → _Include related objects_. O por API:

```bash
curl -s -X POST localhost:5601/api/saved_objects/_export -H 'kbn-xsrf: true' -H 'Content-Type: application/json' \
  -d '{"objects":[{"type":"dashboard","id":"salud-tracker-gastos"},{"type":"search","id":"eventos-de-seguridad"}],"includeReferencesDeep":true,"excludeExportDetails":true}' \
  > src/main/docker/elk/kibana/objetos.ndjson
```
