#!/bin/bash
# Start All Microservices (v2-asincrona - RabbitMQ async version)
# IMPORTANT: Run 'docker-compose up -d' first to start RabbitMQ and PostgreSQL
# This script starts all microservices in the background

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "========================================="
echo "  Starting Medical Services Network"
echo "========================================="

# Start Users Service (8081)
echo "[1/2] Starting Users Service on port 8081..."
cd "$PROJECT_DIR/services/users-service"
nohup mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081" -DskipTests=true > /tmp/users-service.log 2>&1 &
USERS_PID=$!
echo "Users Service started (PID: $USERS_PID)"

# Wait a moment for the service to initialize
sleep 3

# Start Appointments Service (8082)
echo "[2/2] Starting Appointments Service on port 8082..."
cd "$PROJECT_DIR/services/appointments-service"
nohup mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082" -DskipTests=true > /tmp/appointments-service.log 2>&1 &
APPOINTMENTS_PID=$!
echo "Appointments Service started (PID: $APPOINTMENTS_PID)"

echo ""
echo "========================================="
echo "  All Services Started"
echo "========================================="
echo ""
echo "Services:"
echo "  - Users Service:      http://localhost:8081"
echo "  - Appointments Service: http://localhost:8082"
echo ""
echo "Logs:"
echo "  - Users Service:      /tmp/users-service.log"
echo "  - Appointments Service: /tmp/appointments-service.log"
echo ""
echo "To stop all services, run: ./scripts/stop-all-services.sh"
echo ""

# Save PIDs to file for stop script
echo "$USERS_PID $APPOINTMENTS_PID" > /tmp/medical-services-pids.txt