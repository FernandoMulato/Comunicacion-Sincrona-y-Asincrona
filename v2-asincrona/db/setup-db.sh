#!/bin/bash
# Script para inicializar la base de datos
# Uso: ./setup-db.sh [postgres_password]

POSTGRES_PASSWORD=${1:-postgres}  # Por defecto usa 'postgres'

echo "========================================="
echo "  Inicializando Base de Datos"
echo "========================================="

# Ejecutar script SQL
PGPASSWORD="$POSTGRES_PASSWORD" psql -U postgres -h localhost -f init-databases.sql

echo ""
echo "========================================="
echo "  Base de datos inicializada"
echo "========================================="
echo ""
echo "Credenciales configuradas:"
echo "  - Usuario: medical_user"
echo "  - Contraseña: medical123"
echo "  - Base de datos: users_db, appointments_db"
echo ""
echo "Para actualizar la contraseña en los servicios:"
echo "  Editar services/users-service/src/main/resources/application.yml"
echo "  Editar services/appointments-service/src/main/resources/application.yml"
echo "  Cambiar: password: \${DB_PASSWORD:medical123}"