# Postman Collection - v2-asincrona

## Descripción

Colección de endpoints para 测试 la versión asíncrona (v2-asincrona) del Medical Services Network.

## Diferencia con v1-sincrona

| Aspecto | v1-sincrona | v2-asincrona |
|--------|-------------|--------------|
| **Comunicación** | REST Síncrono | RabbitMQ Asíncrono |
| Validación de paciente | HTTP directa | Via cola RabbitMQ |
| Timeout | No hay | 5 segundos |

---

## Endpoints

### users-service (Puerto 8081)

#### Crear Usuario (Patient)

```http
POST http://localhost:8081/api/users
Content-Type: application/json

{
  "username": "juanperez",
  "email": "juan@example.com",
  "password": "Password123!",
  "role": "PATIENT",
  "documentNumber": "12345678",
  "firstName": "Juan",
  "lastName": "Pérez",
  "phone": "+54 9 11 1234-5678"
}
```

**Respuesta**: 201 Created
```json
{
  "id": 1,
  "username": "juanperez",
  "email": "juan@example.com",
  "role": "PATIENT",
  "active": true
}
```

---

#### Listar Usuarios

```http
GET http://localhost:8081/api/users
```

**Respuesta**: 200 OK

---

#### Obtener Usuario por ID

```http
GET http://localhost:8081/api/users/{id}
```

**Respuesta**: 200 OK

---

#### Validar Paciente (REST - fallback para debug)

```http
GET http://localhost:8081/api/users/patients/validate/{documentNumber}
```

**Nota**: Este endpoint se mantiene para debug. En v2-asincrona, la validación ocurre via RabbitMQ.

**Respuesta**: 200 OK (true/false)

---

#### Desactivar Usuario

```http
PATCH http://localhost:8081/api/users/{id}/deactivate
```

**Respuesta**: 204 No Content

---

### appointments-service (Puerto 8082)

#### Crear Cita (Validación Asíncrona via RabbitMQ)

```http
POST http://localhost:8082/api/appointments
Content-Type: application/json

{
  "patientDocument": "12345678",
  "patientName": "Juan Pérez",
  "patientPhone": "+54 9 11 1234-5678",
  "professionalId": 1,
  "professionalName": "Dra. Smith",
  "date": "2026-06-15",
  "time": "10:00",
  "durationMinutes": 30,
  "reason": "Chequeo General"
}
```

**Flujo interno**:
1. appointments-service recibe request
2. Publica mensaje a `patient.validation.requests` en RabbitMQ
3. users-service consume, valida, publica respuesta
4. appointments-service recibe respuesta y crea cita

**Respuesta exitosa**: 201 Created
```json
{
  "id": 1,
  "patientDocument": "12345678",
  "patientName": "Juan Pérez",
  "date": "2026-06-15",
  "time": "10:00:00",
  "status": "SCHEDULED"
}
```

**Error - Paciente no encontrado**: 400 Bad Request
```json
"Patient not found with document: 12345678"
```

**Error - Timeout**: 503 Service Unavailable
```json
"Validation service timeout - please try again"
```

---

#### Listar Citas

```http
GET http://localhost:8082/api/appointments
```

**Respuesta**: 200 OK

---

#### Obtener Cita por ID

```http
GET http://localhost:8082/api/appointments/{id}
```

**Respuesta**: 200 OK

---

#### Listar Citas por Paciente

```http
GET http://localhost:8082/api/appointments/patient/{documentNumber}
```

**Respuesta**: 200 OK

---

#### Actualizar Cita (Reprogramar)

```http
PUT http://localhost:8082/api/appointments/{id}
Content-Type: application/json

{
  "date": "2026-06-20",
  "time": "14:00"
}
```

**Respuesta**: 200 OK

---

#### Cancelar Cita

```http
PATCH http://localhost:8082/api/appointments/{id}/cancel
```

**Respuesta**: 204 No Content

---

### Health Endpoints

```http
# users-service
GET http://localhost:8081/actuator/health

# appointments-service  
GET http://localhost:8082/actuator/health
```

**Respuesta**: 200 OK
```json
{"status": "UP"}
```

---

## Colección Postman

### Importar a Postman

1. Abrir Postman
2. Click "Import" → "Paste Raw Text"
3. Pegar el JSON de abajo

```json
{
  "info": {
    "name": "Medical Services Network v2-asincrona",
    "description": "API tests for async RabbitMQ version",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8081"
    },
    {
      "key": "appointmentsUrl", 
      "value": "http://localhost:8082"
    }
  ],
  "item": [
    {
      "name": "users-service",
      "item": [
        {
          "name": "Create Patient User",
          "request": {
            "method": "POST",
            "url": "{{baseUrl}}/api/users",
            "header": [
              {"key": "Content-Type", "value": "application/json"}
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"patient{{$randomInt}}\",\n  \"email\": \"patient{{$randomInt}}@test.com\",\n  \"password\": \"Password123!\",\n  \"role\": \"PATIENT\",\n  \"documentNumber\": \"55555555\",\n  \"firstName\": \"Test\",\n  \"lastName\": \"Patient\",\n  \"phone\": \"3005555555\"\n}"
            }
          }
        },
        {
          "name": "Get All Users",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/users"
          }
        },
        {
          "name": "Validate Patient (REST fallback)",
          "request": {
            "method": "GET",
            "url": "{{baseUrl}}/api/users/patients/validate/55555555"
          }
        }
      ]
    },
    {
      "name": "appointments-service",
      "item": [
        {
          "name": "Create Appointment (Async Validation)",
          "request": {
            "method": "POST",
            "url": "{{appointmentsUrl}}/api/appointments",
            "header": [
              {"key": "Content-Type", "value": "application/json"}
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"patientDocument\": \"55555555\",\n  \"patientName\": \"Test Patient\",\n  \"patientPhone\": \"3005555555\",\n  \"professionalId\": 1,\n  \"professionalName\": \"Dr. Smith\",\n  \"date\": \"2026-06-20\",\n  \"time\": \"10:00\",\n  \"durationMinutes\": 30,\n  \"reason\": \"Checkup\"\n}"
            }
          }
        },
        {
          "name": "Get All Appointments",
          "request": {
            "method": "GET",
            "url": "{{appointmentsUrl}}/api/appointments"
          }
        },
        {
          "name": "Cancel Appointment",
          "request": {
            "method": "PATCH",
            "url": "{{appointmentsUrl}}/api/appointments/1/cancel"
          }
        }
      ]
    }
  ]
}
```

---

## Casos de Prueba

### TC-001: Crear Cita con Paciente Válido

```
1. POST /api/users (crear paciente)
2. POST /api/appointments (crear cita)
3. Verificar: 201 Created
```

### TC-002: Crear Cita con Paciente Inexistente

```
1. POST /api/appointments (con documento no existente)
2. Verificar: 400 Bad Request
3. Mensaje contiene "Patient not found"
```

### TC-003: Timeout de Validación

```
1. Detener users-service
2. POST /api/appointments
3. Verificar: 503 Service Unavailable (after 5s timeout)
```

### TC-004: Fecha Pasada

```
1. POST /api/appointments (date = yesterday)
2. Verificar: 400 Bad Request
3. Mensaje contiene "past"
```

---

## Configuración RabbitMQ (para verificación)

### Management UI

- URL: http://localhost:15672
- Usuario: guest
- Contraseña: guest

### Verificar Colas

```bash
curl -u guest:guest http://localhost:15672/api/queues
```

Deberían verse:
- `patient.validation.requests`
- `patient.validation.responses`