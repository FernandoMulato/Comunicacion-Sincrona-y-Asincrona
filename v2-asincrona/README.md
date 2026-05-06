# Medical Services Network - v2-asincrona

**Versión:** 2.0.0  
**Tipo de Comunicación:** Asíncrona (RabbitMQ)  
**Stack:** Java 21, Spring Boot, PostgreSQL, RabbitMQ, Maven

---

## Descripción

Sistema de gestión de servicios médicos implementado como arquitectura de microservicios con comunicación **asíncrona via RabbitMQ**. Esta es la versión que reemplaza la comunicación síncrona REST de v1-sincrona.

### Diferencia Principal con v1-sincrona

| Característica | v1-sincrona | v2-asincrona |
|---------------|-------------|--------------|
| Comunicación | REST Síncrono | RabbitMQ Asíncrono |
| Acoplamiento | Tight coupling | Loose coupling |
| Tolerancia a fallos | Si users-service falla,appointments falla | Timeout 5s con error claro |
| Validación paciente | Llamada HTTP directa | Via cola de mensajes |

### Características Principales

- **users-service** (puerto 8081): Gestión de usuarios, pacientes y profesionales
- **appointments-service** (puerto 8082): Gestión de citas médicas con validación asíncrona
- **Comunicación Asíncrona**: Validación de pacientes via RabbitMQ (no REST)
- **Timeout**: 5 segundos para validación de paciente

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                     CLIENTE                                     │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                 APIGATEWAY (futuro - puerto 8080)                 │
└─────────────────────────────────────────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            ▼                           ▼
┌───────────────────────┐   ┌───────────────────────────┐
│   USERS-SERVICE       │   │   APPOINTMENTS-SERVICE    │
│   Puerto 8081        │   │   Puerto 8082            │
└───────────────────────┘   └───────────────────────────┘
            │                           │
            │   ┌─────────────────────┘
            │   │  RabbitMQ: patient.validation.requests
            ▼   ▼
┌─────────────────────────────────────────────┐
│              RABBITMQ                       │
│         medical.exchange (topic)            │
│    patient.validation.requests ─────────► │
│    ◄────────── patient.validation.responses │
└─────────────────────────────────────────────┘
            │
            ▼
┌───────────────────────────┐
│      POSTGRESQL           │
│      Puerto 5432/5433     │
└───────────────────────────┘
```

---

## Estructura del Proyecto

```
v2-asincrona/
├── docker-compose.yml         # RabbitMQ + PostgreSQL
├── services/
│   ├── users-service/         # Servicio de usuarios (puerto 8081)
│   │   └── src/
│   │       ├── main/java/com/medical/
│   │       │   ├── controller/     # REST endpoints
│   │       │   ├── service/       # Lógica de negocio
│   │       │   ├── repository/    # Acceso a datos
│   │       │   ├── entity/       # Entidades JPA
│   │       │   ├── dto/          # Objetos de transferencia
│   │       │   ├── config/       # RabbitMQ config
│   │       │   └── messenger/     # RabbitMQ listeners
│   │       └── test/java/        # Pruebas
│   │
│   └── appointments-service/    # Servicio de citas (puerto 8082)
│       └── src/
│           ├── main/java/com/medical/
│           │   ├── controller/   # REST endpoints
│           │   ├── service/    # Lógica de negocio
│           │   ├── repository/ # Acceso a datos
│           │   ├── entity/     # Entidades JPA
│           │   ├── dto/       # Objetos de transferencia
│           │   ├── config/     # RabbitMQ config
│           │   └── messenger/  # RabbitMQ clients
│           └── test/java/    # Pruebas unitarias y de integración
│
├── scripts/                    # Scripts de gestión
├── db/                        # Scripts de base de datos
└── docs/                      # Documentación
```

---

## Servicios Implementados

### users-service (Puerto 8081)

**Entidades:**

- `User`: Usuario del sistema (id, username, email, password, role, active)
- `Patient`: Paciente médico (id, documentNumber, firstName, lastName, birthDate, phone)
- `Professional`: Profesional de salud (id, documentNumber, firstName, lastName, specialty, licenseNumber)
- `UserRole`: Roles disponibles (ADMIN, PATIENT, PROFESSIONAL)

**Endpoints REST:**

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/users` | Listar todos los usuarios |
| POST | `/api/users` | Crear nuevo usuario |
| GET | `/api/users/{id}` | Obtener usuario por ID |
| PUT | `/api/users/{id}` | Actualizar usuario |
| PATCH | `/api/users/{id}/deactivate` | Desactivar usuario |
| GET | `/api/users/patients/validate/{document}` | Validar paciente (REST - fallback) |

**NOTA**: El endpoint REST `/patients/validate/{document}` se mantiene como fallback para debug, pero **v2-asincrona usa RabbitMQ** para validar pacientes.

**Ejemplo de creación de usuario:**

```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "juanperez",
    "email": "juan@example.com",
    "password": "Password123!",
    "role": "PATIENT",
    "documentNumber": "12345678",
    "firstName": "Juan",
    "lastName": "Pérez",
    "phone": "+54 9 11 1234-5678"
  }'
```

---

### appointments-service (Puerto 8082)

**Entidades:**

- `Appointment`: Cita médica (id, patientDocument, appointmentDate, time, reason, status)
- `AppointmentStatus`: Estados (SCHEDULED, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW)

**Endpoints REST:**

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/appointments` | Listar todas las citas |
| POST | `/api/appointments` | **Crear nueva cita (via RabbitMQ)** |
| GET | `/api/appointments/{id}` | Obtener cita por ID |
| GET | `/api/appointments/patient/{document}` | Citas por documento |
| PUT | `/api/appointments/{id}` | Actualizar/cambiar fecha |
| PATCH | `/api/appointments/{id}/cancel` | Cancelar cita |

**Ejemplo de creación de cita (validación asíncrona):**

```bash
curl -X POST http://localhost:8082/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "patientDocument": "12345678",
    "appointmentDate": "2026-06-15",
    "time": "10:00",
    "durationMinutes": 30,
    "reason": "Chequeo General",
    "professionalId": 1,
    "professionalName": "Dra. Smith"
  }'
```

---

## Comunicación Asíncrona (RabbitMQ)

### Flujo de Validación de Paciente

```
1. POST /api/appointments (appointments-service)
          │
          ▼
2. PUBLISH → patient.validation.requests (RabbitMQ)
   {
     "correlationId": "uuid-1234",
     "documentNumber": "12345678",
     "timestamp": "2026-05-06T10:00:00Z"
   }
          │
          ▼
3. CONSUME (users-service)
   │ PatientValidationListener
          │
          ▼
4. Validar en PostgreSQL (users_db)
          │
          ▼
5. PUBLISH → patient.validation.responses (RabbitMQ)
   {
     "correlationId": "uuid-1234",
     "valid": true,
     "patientName": "Juan Pérez"
   }
          │
          ▼
6. CONSUME (appointments-service)
   │ PatientValidationClient
          │
          ▼
7. Crear cita o rechartar
```

### Configuración RabbitMQ

| Componente | Valor |
|-----------|-------|
| Exchange | `medical.exchange` (topic) |
| Queue Requests | `patient.validation.requests` |
| Queue Responses | `patient.validation.responses` |
| Routing Key (request) | `validation.request` |
| Routing Key (response) | `validation.response` |
| Timeout | 5 segundos |

### Manejo de Errores

| Escenario | Comportamiento |
|----------|--------------|
| Paciente válido | Crea cita con status SCHEDULED |
| Paciente no encontrado | Error 400 "Patient not found" |
| Timeout (5s) | Error 503 "Validation service timeout" |
| RabbitMQ caído | Error "Validation service unavailable" |

---

## Base de Datos

**Configuración:**

- **PostgreSQL users**: Puerto 5432, BD: `users_db`
- **PostgreSQL appointments**: Puerto 5433, BD: `appointments_db`
- **Usuario**: medical_user / medical123

### Docker Compose

```bash
cd v2-asincrona
docker-compose up -d
```

Esto levanta:
- `rabbitmq`: Puerto 5672 (AMQP), 15672 (Management UI)
- `postgres-users`: Puerto 5432
- `postgres-appointments`: Puerto 5433

---

## Prerrequisitos

1. **Java 21** o superior
2. **PostgreSQL 14+**
3. **RabbitMQ 3** (vía docker-compose)
4. **Maven 3.8+**

---

## Inicio Rápido

### 1. Levantar Infraestructura

```bash
cd v2-asincrona
docker-compose up -d

# Verificar que RabbitMQ está corriendo
docker ps | grep rabbitmq
```

### 2. Inicializar Base de Datos

```bash
cd v2-asincrona/db
chmod +x setup-db.sh
PGPASSWORD=admin123 ./setup-db.sh admin123
```

### 3. Compilar los Servicios

```bash
# users-service
cd v2-asincrona/services/users-service
mvn clean package -DskipTests

# appointments-service
cd v2-asincrona/services/appointments-service
mvn clean package -DskipTests
```

### 4. Iniciar los Servicios

```bash
# Opción A: Iniciar todos los servicios
cd v2-asincrona/scripts
./start-all-services.sh

# Opción B: Iniciar individualmente
./start-users-service.sh    # Puerto 8081
./start-appointments-service.sh  # Puerto 8082
```

### 5. Verificar que los Servicios están Activos

```bash
# users-service
curl http://localhost:8081/actuator/health

# appointments-service
curl http://localhost:8082/actuator/health
```

---

## Pruebas

### Pruebas Unitarias

```bash
# users-service
cd v2-asincrona/services/users-service
mvn test

# appointments-service
cd v2-asincrona/services/appointments-service
mvn test
```

**Resultado**: 9 tests pasando en appointments-service

### Pruebas de Integración

Las pruebas de integración requieren:
- RabbitMQ corriendo (`docker-compose up -d`)
- PostgreSQL corriendo
- Ambos servicios ejecutándose

---

## Detener los Servicios

```bash
cd v2-asincrona/scripts
./stop-all-services.sh

# Detener infraestructura
cd v2-asincrona
docker-compose down
```

---

## Comparación: v1-sincrona vs v2-asincrona

| Aspecto | v1-sincrona | v2-asincrona |
|---------|-------------|--------------|
| **Comunicación** | REST (HTTP) | RabbitMQ (AMQP) |
| **Puerto users-service** | 8081 | 8081 |
| **Puerto appointments** | 8082 | 8082 |
| **Acoplamiento** | Tight | Loose |
| **Si users-service cae** | Crea citas inválidas | No permite crear citas |
| **Timeout** | N/A (bloquea) | 5 segundos |
| **Debug endpoint** | `/patients/validate/` | `/patients/validate/` + RabbitMQ UI |

### Cuándo Usar Cuál

- **v1-sincrona**: Desarrollo local simple, sin Docker
- **v2-asincrona**: Producción, necesita resiliencia

---

## Tech Stack

| Componente | Tecnología | Versión |
|------------|------------|--------|
| Lenguaje | Java | 21 |
| Framework | Spring Boot | 3.2.x |
| Messaging | RabbitMQ | 3-management |
| Build Tool | Maven | 3.9+ |
| Base de Datos | PostgreSQL | 14+ |
| testing | JUnit 5 | 5.10.x |
| Mocking | Mockito | 5.x |

---

## Próximos Pasos (Mejoras)

1. [ ] FR-005: Publicar evento cuando se crea paciente (para cache en appointments)
2. [ ] Retry logic en PatientValidationClient
3. [ ] Dead letter queue para mensajes fallidos
4. [ ] API Gateway con autenticación

---

## Licencia

Este proyecto es para fines educativos y de demostración.

---

## Contribuir

1. Crear un branch para tu feature: `git checkout -b feature/nueva-funcionalidad`
2. Hacer commit de los cambios: `git commit -m 'feat: descripción'`
3. Push al branch: `git push origin feature/nueva-funcionalidad`
4. Crear un Pull Request