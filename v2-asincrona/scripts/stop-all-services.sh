#!/bin/bash
# Stop All Microservices (v2-asincrona)
# Run 'docker-compose down' to stop infrastructure

echo "========================================="
echo "  Stopping Medical Services Network"
echo "========================================="

# Try to read PIDs from file
if [ -f /tmp/medical-services-pids.txt ]; then
    read -r USERS_PID APPOINTMENTS_PID < /tmp/medical-services-pids.txt
    
    # Kill services by PID
    if [ -n "$USERS_PID" ]; then
        echo "Stopping Users Service (PID: $USERS_PID)..."
        kill $USERS_PID 2>/dev/null
    fi
    
    if [ -n "$APPOINTMENTS_PID" ]; then
        echo "Stopping Appointments Service (PID: $APPOINTMENTS_PID)..."
        kill $APPOINTMENTS_PID 2>/dev/null
    fi
    
    rm -f /tmp/medical-services-pids.txt
fi

# Also kill any remaining Java processes for these services
echo "Cleaning up remaining processes..."

# Find and kill Spring Boot processes for our services
pkill -f "users-service" 2>/dev/null
pkill -f "appointments-service" 2>/dev/null

# Wait a moment
sleep 2

echo ""
echo "========================================="
echo "  All Services Stopped"
echo "========================================="
echo ""