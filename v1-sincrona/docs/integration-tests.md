# Pruebas de Integración

Este documento explica cómo ejecutar las pruebas de integración para los servicios.

## Requisitos previos

1. PostgreSQL corriendo en puerto 5432
2. Bases de datos inicializadas:
   - users_db
   - appointments_db

## Ejecutar pruebas

### Users Service

```bash
cd v1-sincrona/services/users-service

# Iniciar servicio en background
nohup java -jar target/users-service-1.0.0-SNAPSHOT.jar > /tmp/users.log 2>&1 &

# Ejecutar pruebas
mvn test -Dtest=UsersServiceIntegrationTest

# Detener servicio
./scripts/stop-all-services.sh
```

### Appointments Service

```bash
cd v1-sincrona/services/appointments-service

# Asegurarse que users-service esté corriendo en puerto 8081
# (necesario para validación síncrona)

# Iniciar servicio
nohup java -jar target/appointments-service-1.0.0-SNAPSHOT.jar > /tmp/appointments.log 2>&1 &

# Ejecutar pruebas
mvn test -Dtest=AppointmentsServiceIntegrationTest

# Detener servicio
./scripts/stop-all-services.sh
```

## Ejecutar todas las pruebas unitarias

```bash
# Users service
cd v1-sincrona/services/users-service
mvn test

# Appointments service
cd v1-sincrona/services/appointments-service
mvn test
```

## Tests disponibles

### Users Service
- `UserServiceTest` - Tests unitarios del servicio
- `UsersServiceIntegrationTest` - Tests de integración del API

### Appointments Service
- `AppointmentServiceTest` - Tests unitarios del servicio
- `AppointmentsServiceIntegrationTest` - Tests de integración del API

## Verificación manual (sin código)

Para verificación manual, ver: `docs/postman-endpoints.md`

```bash
# Users Service (puerto 8081)
curl http://localhost:8081/api/users

# Appointments Service (puerto 8082)
curl http://localhost:8082/api/appointments
```