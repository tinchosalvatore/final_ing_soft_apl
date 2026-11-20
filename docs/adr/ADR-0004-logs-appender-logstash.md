# ADR-0004: Logs enviados con el appender de Logstash de JHipster

- Estado: Aceptado
- Fecha: 2026-11-20

## Contexto
- Hay que centralizar los logs en ELK, de la forma más simple posible (es la parte con menos experiencia previa y hay poco tiempo).
- JHipster ya trae un appender que manda los logs en JSON por TCP a Logstash (`jhipster.logging.logstash.enabled/host/port`, puerto 5000).
- El factor XI dice que la app solo debería escribir sus logs a `stdout` y que el entorno se encargue de recolectarlos (por ejemplo, Filebeat leyendo los logs de los contenedores).

## Decisión
- Usar el appender de Logstash de JHipster, activado y configurado **por variables de entorno** (host y puerto no van fijos en el código).
- La app **también** sigue escribiendo a consola (`stdout`), así que si ELK no está, los logs no se pierden de vista.
- ELK corre con un `docker compose` propio: Elasticsearch, Logstash (entrada TCP 5000 en JSON) y Kibana, sin seguridad de Elastic activada (solo entorno local o de demo).

## Consecuencias
- Cero piezas extra (no hay Filebeat ni OpenTelemetry Collector) y la configuración casi ya viene hecha.
- **Se cede parcialmente el factor XI:** la app conoce el destino de sus logs. Se mitiga con la configuración por entorno y la salida paralela a `stdout`. Es una concesión consciente y defendible.
- Si Logstash está caído, el appender descarta logs una vez lleno su buffer en memoria (`ring-buffer-size`): no hay buffer persistente.
- No hay política de retención (ILM): queda fuera de alcance.

## Alternativas
- **JSON a `stdout` + Filebeat:** es el factor XI "puro", pero suma un componente más para configurar. Descartado por tiempo.
- **OpenTelemetry Collector:** es el estándar moderno de telemetría, pero excede el alcance. Descartado.
