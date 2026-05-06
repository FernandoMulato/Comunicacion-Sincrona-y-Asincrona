#!/bin/bash
# Start Appointments Service (v2-asincrona)
# Requires: docker-compose up -d (RabbitMQ + PostgreSQL)
# Port: 8082

SERVICE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../services/appointments-service" && pwd)"
PORT=8082

echo "Starting Appointments Service on port $PORT..."

cd "$SERVICE_DIR"

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven not found. Please install Maven."
    exit 1
fi

# Build and run
mvn spring-boot:run \
    -Dspring-boot.run.arguments="--server.port=$PORT" \
    -DskipTests=true

echo "Appointments Service stopped."