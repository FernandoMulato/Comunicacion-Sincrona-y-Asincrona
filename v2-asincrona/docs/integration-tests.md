# Integration Tests - v2-asincrona

## Descripción

Este documento describe las pruebas de integración para la versión asíncrona (v2-asincrona) del Medical Services Network.

## Diferencia con v1-sincrona

| Aspecto | v1-sincrona | v2-asincrona |
|--------|-------------|--------------|
| **Comunicación** | REST Síncrono | RabbitMQ Asíncrono |
| **Validación** | HTTP directa | Via cola de mensajes |
| **Tests** | HttpClient directo | Requiere RabbitMQ |

---

## Pruebas Unitarias

Las pruebas unitarias verifican la lógica de negocio sin dependencias externas.

```bash
# Ejecutar pruebas unitarias
cd v2-asincrona/services/appointments-service
mvn test -Dtest=AppointmentServiceTest
```

**Resultado**: 9 tests pasando ✅

### Tests Incluidos

1. `shouldCreateAppointment_whenValidData` - Verifica creación de cita exitosa
2. `shouldThrowException_whenPatientNotFound` - Verifica rechazo de paciente no existente
3. `shouldThrowException_whenValidationServiceTimeout` - Verifica timeout de validación
4. `shouldUsePatientNameFromValidationResponse_whenNotProvided` - Verifica que el nombre del paciente viene de la validación
5. `shouldThrowException_whenTimeSlotNotAvailable` - Verifica conflicto de horario
6. `shouldThrowException_whenDateInPast` - Verifica rechazo de fecha pasada
7. `shouldCancelAppointment_whenExistsAndScheduled` - Verifica cancelación
8. `shouldThrowException_whenCancelAlreadyCancelled` - Verifica rechado de doble cancelación
9. `shouldReturnAllAppointments` - Verifica listado de citas

---

## Pruebas de Integración

### Requisitos

Para ejecutar las pruebas de integración:

1. **Docker** instalado y corriendo
2. **RabbitMQ** disponible (vía docker-compose)
3. **PostgreSQL** para ambas bases de datos
4. Servicios iniciados manualmente

### Iniciar Entorno

```bash
# 1. Levantar infraestructura (RabbitMQ + PostgreSQL)
cd v2-asincrona
docker-compose up -d

# 2. Compilar servicios
cd services/users-service && mvn package -DskipTests
cd services/appointments-service && mvn package -DskipTests

# 3. Iniciar servicios (en terminals separados)
# Terminal 1: users-service
java -jar services/users-service/target/users-service-1.0.0-SNAPSHOT.jar

# Terminal 2: appointments-service
java -jar services/appointments-service/target/appointments-service-1.0.0-SNAPSHOT.jar
```

### Ejecutar Tests

```bash
cd v2-asincrona/services/appointments-service
mvn test -Dtest=AppointmentsServiceIntegrationTest
```

### Tests de Integración Incluidos

1. `shouldReturnServiceHealth` - Verifica health endpoint
2. `shouldCreateAppointment_whenPatientExists` - Verifica flujo completo async
3. `shouldRejectAppointment_whenPatientNotFound` - Verifica rechazo async
4. `shouldRejectAppointment_whenDateInPast` - Verifica validación de fecha
5. `shouldGetAllAppointments` - Verifica listado

---

## Flujo de Validación Asíncrona

### Diagrama del Flujo

```
┌─────────────────────────────────────────────────────────────────┐
│                    APPOINTMENTS-SERVICE                          │
│  POST /api/appointments                                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌───────────────────────┐
                    │ PatientValidationClient │
                    │ publish → queue      │
                    └───────────────────────┘
                              │
                              ▼ RabbitMQ
                    ┌───────────────────────┐
                    │  patient.validation  │
                    │     .requests        │
                    └───────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      USERS-SERVICE                              │
│  @RabbitListener                                                │
│  ┌───────────────────────┐                                       │
│  │ PatientValidation    │                                        │
│  │ Listener            │                                        │
│  └───────────────────────┘                                       │
│         │                                                       │
│         ▼                                                       │
│  ┌───────────────────────┐                                       │
│  │ Valida en PostgreSQL│                                        │
│  └───────────────────────┘                                       │
│         │                                                       │
│         ▼                                                       │
│  ┌───────────────────────┐                                       │
│  │ PatientEventPublisher│                                        │
│  │ publish → response  │                                        │
│  └───────────────────────┘                                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼ RabbitMQ
                    ┌───────────────────────┐
                    │  patient.validation  │
                    │     .responses       │
                    └───────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    APPOINTMENTS-SERVICE                          │
│  @RabbitListener                                                │
│  ┌───────────────────────┐                                       │
│  │ ValidationResponse    │ → correlationId match               │
│  │ Listener             │                                       │
│  └───────────────────────┘                                       │
│         │                                                       │
│         ▼                                                       │
│  Crear o rechazar appointment                                  │
└──────────────────────────────���──────────────────────────────────┘
```

### Tiempos Esperados

| Escenario | Tiempo Máximo |
|-----------|---------------|
| Paciente válido | ~500ms |
| Paciente no encontrado | ~500ms |
| Timeout | 5 segundos |

---

## Verificación Manual

Si los tests automatizados fallan, puedes verificar manualmente:

```bash
# 1. Verificar servicios
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health

# 2. Verificar colas RabbitMQ
curl -u guest:guest http://localhost:15672/api/queues

# 3. Crear paciente
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@test.com","password":"Pass123!","role":"PATIENT","documentNumber":"99999999","firstName":"Test","lastName":"User"}'

# 4. Crear cita (validación async)
curl -X POST http://localhost:8082/api/appointments \
  -H "Content-Type: application/json" \
  -d '{"patientDocument":"99999999","date":"2026-06-20","time":"10:00","reason":"Test"}'
```

---

## Troubleshooting

### tests Timeout

**Problema**: Los tests fallan por timeout esperando respuesta de RabbitMQ

**Causa**: users-service no está corriendo o no puede conectar a RabbitMQ

**Solución**:
1. Verificar que users-service está corriendo en puerto 8081
2. Verificar que RabbitMQ está disponible en puerto 5672
3. Revisar logs de ambos servicios

###的患者NoEncontrado

**Problema**: Always returns "Patient not found"

**Causa**: El paciente no existe en la base de datos users_db

**Solución**:
1. Verificar que el documento existe en la tabla patients
2. Crear usuario con rol PATIENT primero via POST /api/users

### Error 500

**Problema**: Error 500 Internal Server Error

**Causa**: Revisar logs del servicio con `tail -f /tmp/appointments.log`

---

## Notas de Implementación

- **Timeout**: 5 segundos configurado en PatientValidationClient
- **Correlation ID**: UUID generado para cada request de validación
- **Colas**: Durables, con TTL de 30 segundos
- **Exchange**: Topic tipo medical.exchange