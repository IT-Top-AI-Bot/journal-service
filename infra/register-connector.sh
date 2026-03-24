#!/bin/sh
set -e

JSON=$(cat /connector.json)
JSON=$(echo "$JSON" | sed "s|\${DATABASE_USERNAME}|${DATABASE_USERNAME}|g")
JSON=$(echo "$JSON" | sed "s|\${DATABASE_PASSWORD}|${DATABASE_PASSWORD}|g")
JSON=$(echo "$JSON" | sed "s|\${DATABASE_NAME}|${DATABASE_NAME}|g")
JSON=$(echo "$JSON" | sed 's|\${routedByValue}|${routedByValue}|g')

echo "Deleting existing connector if present..."
curl -s -X DELETE http://debezium:8083/connectors/journal-outbox-connector || true

echo "Registering Debezium connector..."
curl -sf -X POST http://debezium:8083/connectors \
  -H "Content-Type: application/json" \
  -d "$JSON"

echo "Done."
