#!/bin/bash
# Start Users Service (v2-asincrona)
# Requires: docker-compose up -d (RabbitMQ + PostgreSQL)
# Port: 8081

SERVICE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../services/users-service" && pwd)"
PORT=8081

echo "Starting Users Service on port $PORT..."

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

echo "Users Service stopped."