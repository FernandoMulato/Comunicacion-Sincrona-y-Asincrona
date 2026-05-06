# Endpoints para probar en Postman

Este documento contiene los endpoints disponibles para probar los microservicios de Medical Services Network.

---

## Configuración Base

| Servicio | URL Base | Puerto |
|----------|----------|--------|
| users-service | http://localhost | 8081 |
| appointments-service | http://localhost | 8082 |

---

## Users Service (Puerto 8081)

### Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | /api/users | Listar todos los usuarios |
| GET | /api/users/{id} | Obtener usuario por ID |
| POST | /api/users | Crear nuevo usuario |
| PUT | /api/users/{id} | Actualizar usuario |
| PATCH | /api/users/{id}/deactivate | Desactivar usuario |

### Ejemplos

#### GET - Listar usuarios
```bash
curl http://localhost:8081/api/users
```

#### POST - Crear paciente
```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"juan.perez","password":"Password1!","email":"juan@test.com","role":"PATIENT"}'
```

---

## Appointments Service (Puerto 8082)

### Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | /api/appointments | Listar todas las citas |
| GET | /api/appointments/{id} | Obtener cita por ID |
| GET | /api/appointments/patient/{document} | Citas por documento |
| POST | /api/appointments | Crear nueva cita |
| PUT | /api/appointments/{id} | Reprogramar cita |
| PATCH | /api/appointments/{id}/cancel | Cancelar cita |

### Ejemplos

#### GET - Listar citas
```bash
curl http://localhost:8082/api/appointments
```

#### POST - Crear cita
```bash
curl -X POST http://localhost:8082/api/appointments \
  -H "Content-Type: application/json" \
  -d '{"patientDocument":"87654321","patientName":"Maria Garcia","professionalId":1,"professionalName":"Dr. Smith","date":"2026-05-15","time":"10:00:00","reason":"Chequeo general"}'
```

---

## Resultados de Pruebas

### Users Service (8081) - ✅ VERIFICADO
- GET /api/users: Lista 4 usuarios
- POST /api/users: Crea usuario exitosamente

### Appointments Service (8082) - ✅ VERIFICADO
- GET /api/appointments: Lista 2 citas
- POST /api/appointments: Crea nueva cita

---

## Scripts de Base de Datos

| Script | Descripción |
|--------|-------------|
| db/init-databases.sql | Crea tablas y datos de ejemplo |
| db/setup-db.sh | Script bash para ejecutar SQL |
| db/grant-permissions.sql | Otorga permisos a medical_user |

---

## Notas

- Puerto PostgreSQL: 5432
- Usuario: medical_user, Contraseña: medical123
- Bases de datos: users_db, appointments_db