#!/usr/bin/env bash
# Spec 004, CA08 y CA09: hace N logins fallidos contra la app para probar la alerta "Logins fallidos (5 en 5 min)".
#   ./probar-alerta.sh 5   -> la alerta se dispara (CA08)
#   ./probar-alerta.sh 4   -> la alerta NO se dispara (CA09); esperar 5 minutos sin otros intentos antes de probar
# Variables opcionales: APP_URL (default http://localhost:8080), LOGIN (default "user").
set -euo pipefail

N="${1:-5}"
APP_URL="${APP_URL:-http://localhost:8080}"
LOGIN="${LOGIN:-user}"

for i in $(seq 1 "$N"); do
  estado=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$APP_URL/api/authenticate" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$LOGIN\",\"password\":\"incorrecta-$i\"}")
  echo "Login fallido $i/$N -> HTTP $estado"
done
echo "Listo. La regla se evalúa cada minuto: mirala en Kibana > Stack Management > Rules > \"Logins fallidos (5 en 5 min)\"."
