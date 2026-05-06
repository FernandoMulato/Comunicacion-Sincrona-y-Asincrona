# Medical Services Network - v1-sincrona

**Versión:** 1.0.0  
**Tipo de Comunicación:** Síncrona (REST)  
**Stack:** Java 21, Spring Boot, PostgreSQL, Maven

---

## Descripción

Sistema de gestión de servicios médicos implementado como arquitectura de microservicios con comunicación síncrona REST. Permite gestionar usuarios (pacientes y profesionales) y turnos/citas médicas para el Centro de Salud Piedra Azul.

### Características Principales

- **users-service** (puerto 8081): Gestión de usuarios, pacientes y profesionales
- **appointments-service** (puerto 8082): Gestión de citas médicas con validación de pacientes
- **Comunicación Síncrona**: appointments-service valida pacientes contra users-service mediante REST
- **Creación Automática**: Los pacientes se crean automáticamente al registrar un usuario con rol PATIENT

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                     CLIENTE (futuro)                   │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                 APIGATEWAY (futuro - puerto 8080)                  │
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
            │   │  GET /api/users/patients/validate/{document}
            ▼   ▼
┌───────────────────────────┐
│      POSTGRESQL           │
│      Puerto 5432           │
└───────────────────────────┘
```

---

## Estructura del Proyecto

```
v1-sincrona/
├── services/
│   ├── users-service/         # Servicio de usuarios (puerto 8081)
│   │   └── src/
│   │       ├── main/java/com/medical/
│   │       │   ├── controller/     # REST endpoints
│   │       │   ├── service/      # Lógica de negocio
│   │       │   ├── repository/   # Acceso a datos
│   │       │   ├── entity/       # Entidades JPA
│   │       │   ├── dto/          # Objetos de transferencia
│   │       │   └── config/        # Configuración
│   │       └── test/java/        # Pruebas unitarias
│   │
│   └── appointments-service/    # Servicio de citas (puerto 8082)
│       └── src/
│           ├── main/java/com/medical/
│           │   ├── controller/     # REST endpoints
│           │   ├── service/        # Lógica de negocio
│           │   ├── repository/    # Acceso a datos
│           │   ├── entity/       # Entidades JPA
│           │   ├── dto/          # Objetos de transferencia
│           │   └── client/       # Cliente REST a users-service
│           └── test/java/        # Pruebas unitarias
│
├── scripts/                    # Scripts de gestión
│   ├── start-all-services.sh  # Iniciar todos los servicios
│   ├── start-users-service.sh
│   ├── start-appointments-service.sh
│   └── stop-all-services.sh
│
├── db/                        # Scripts de base de datos
│   ├── init-databases.sql     # Crear bases de datos
│   ├── setup-db.sh          # Script de inicialización
│   └── grant-permissions.sql # Permisos de usuario
│
└── docs/                     # Documentación adicional
    ├── postman-endpoints.md   # Colección de endpoints
    └── integration-tests.md  # Documentación de pruebas
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
| PATCH | `/api/users/{id}/deactivate` | Desactivar usuario |
| GET | `/api/users/patients/validate/{document}` | Validar paciente (para appointments-service) |

**Ejemplo de creación de usuario:**

```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "juanperez",
    "email": "juan@example.com",
    "password": "password123",
    "role": "PATIENT",
    "documentNumber": "12345678",
    "firstName": "Juan",
    "lastName": "Pérez",
    "birthDate": "1990-05-15",
    "phone": "+54 9 11 1234-5678"
  }'
```

**Respuesta exitosa:**

```json
{
  "id": 1,
  "username": "juanperez",
  "email": "juan@example.com",
  "role": "PATIENT",
  "active": true,
  "patient": {
    "id": 1,
    "documentNumber": "12345678",
    "firstName": "Juan",
    "lastName": "Pérez",
    "birthDate": "1990-05-15",
    "phone": "+54 9 11 1234-5678"
  }
}
```

---

### appointments-service (Puerto 8082)

**Entidades:**

- `Appointment`: Cita médica (id, patientDocument, appointmentDate, time, reason, status)
- `AppointmentStatus`: Estados (SCHEDULED, CONFIRMED, COMPLETED, CANCELLED)

**Endpoints REST:**

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/appointments` | Listar todas las citas |
| POST | `/api/appointments` | Crear nueva cita |
| GET | `/api/appointments/patient/{document}` | Citas por documento |
| PATCH | `/api/appointments/{id}/cancel` | Cancelar cita |

**Ejemplo de creación de cita:**

```bash
curl -X POST http://localhost:8082/api/appointments \
  -H "Content-Type: application/json" \
  -d '{
    "patientDocument": "12345678",
    "appointmentDate": "2026-06-15",
    "time": "10:00",
    "reason": "Chequeo General"
  }'
```

**Validación de paciente:** Al crear una cita, appointments-service llama automáticamente a users-service para validar que el paciente existe:

```
GET http://localhost:8081/api/users/patients/validate/12345678
```

- **Paciente existe**: La cita se crea exitosamente
- **Paciente no existe**: Error 404 "Patient not found"

---

## Base de Datos

**Configuración:**

- **Motor:** PostgreSQL
- **Puerto:** 5432
- **Usuario:** medical_user
- **Contraseña:** medical123

**Bases de datos:**

- `users_db` (para users-service)
- `appointments_db` (para appointments-service)

**Credenciales de administrador:**

- Usuario: postgres
- Contraseña: admin123

---

## Prerrequisitos

1. **Java 21** o superior
2. **PostgreSQL 14+**
3. **Maven 3.8+**
4. **Docker** (opcional, para levantar PostgreSQL)

### Instalar PostgreSQL con Docker

```bash
docker run -d \
  --name medical-postgres \
  -e POSTGRES_PASSWORD=admin123 \
  -e POSTGRES_USER=postgres \
  -p 5432:5432 \
  postgres:14
```

---

## Inicio Rápido

### 1. Inicializar Base de Datos

```bash
cd v1-sincrona/db
chmod +x setup-db.sh
PGPASSWORD=admin123 ./setup-db.sh admin123
```

### 2. Compilar los Servicios

```bash
# Compilar users-service
cd v1-sincrona/services/users-service
mvn clean package -DskipTests

# Compilar appointments-service
cd v1-sincrona/services/appointments-service
mvn clean package -DskipTests
```

### 3. Iniciar los Servicios

```bash
# Opción A: Iniciar todos los servicios
cd v1-sincrona/scripts
./start-all-services.sh

# Opción B: Iniciar individualmente
./start-users-service.sh    # Puerto 8081
./start-appointments-service.sh  # Puerto 8082
```

### 4. Verificar que los Servicios están Activos

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
cd v1-sincrona/services/users-service
mvn test

# appointments-service
cd v1-sincrona/services/appointments-service
mvn test
```

### Pruebas de Integración

Las pruebas de integración requieren que los servicios estén ejecutándose. Ver `docs/integration-tests.md` para más detalles.

---

## Detener los Servicios

```bash
cd v1-sincrona/scripts
./stop-all-services.sh
```

O detener manualmente:

```bash
pkill -f "users-service"
pkill -f "appointments-service"
```

---

## Colección Postman

Ver `docs/postman-endpoints.md` para una colección completa de requests con ejemplos.

---

## Comunicación Síncrona vs Asíncrona

### v1-sincrona (Actual)

- **Tipo:** REST síncrono (bloqueante)
- **Comportamiento:** appointments-service espera respuesta inmediata de users-service
- **Ventajas:** Simplicidad, consistencia inmediata
- **Desventajas:** Acoplamiento temporal, menor tolerancia a fallos

### v2-asincrona (Futuro)

- **Tipo:** Mensajería con RabbitMQ/Kafka
- **Comportamiento:** Comunicación no bloqueante via eventos
- **Ventajas:** Desacoplamiento, mayor escalabilidad, tolerancia a fallos
- **Desventajas:** Mayor complejidad, consistencia eventual

---

## Próximos Pasos

1. [ ] Implementar professionals-service (manejo de horarios de profesionales)
2. [ ] Completar funcionalidades de appointments (reprogramar, marcar como completada)
3. [ ] Implementar cliente JavaFX para consumir los servicios
4. [ ] Crear API Gateway para routing centralizado
5. [ ] Migrar a v2-asincrona con RabbitMQ/Kafka

---

## Tech Stack

| Componente | Tecnología | Versión |
|------------|------------|--------|
| Lenguaje | Java | 21 |
| Framework | Spring Boot | 3.2.x |
| Build Tool | Maven | 3.9+ |
| Base de Datos | PostgreSQL | 14+ |
| testing | JUnit 5 | 5.10.x |
| Mocking | Mockito | 5.x |

---

## Licencia

Este proyecto es para fines educativos y de demostración.

---

## Contribuir

1. Crear un branch para tu feature: `git checkout -b feature/nueva-funcionalidad`
2. Hacer commit de los cambios: `git commit -m 'feat: descripción'`
3. Push al branch: `git push origin feature/nueva-funcionalidad`
4. Crear un Pull Request