# Medical Services Network

**Proyecto:** Sistema de Gestión de Citas Médicas  
**Autor:** Henry Fernando Mulato Llanten  
**Stack:** Java 21, Spring Boot, PostgreSQL, RabbitMQ, Maven

---

## Descripción

Sistema de gestión de servicios médicos implementado como arquitectura de microservicios para el Centro de Salud Piedra Azul. Este proyecto demuestra la evolución de comunicación síncrona (REST) a comunicación asíncrona (RabbitMQ).

---

## Estructura del Proyecto

```
Comunicacion_Sincrona_y_Asincrona/
│
├── v1-sincrona/                    # Comunicación Síncrona (REST)
│   ├── README.md
│   ├── services/
│   │   ├── users-service/         # Puerto 8081
│   │   └── appointments-service/  # Puerto 8082
│   ├── scripts/
│   ├── db/
│   └── docs/
│
├── v2-asincrona/                  # Comunicación Asíncrona (RabbitMQ)
│   ├── README.md
│   ├── docker-compose.yml         # RabbitMQ + PostgreSQL
│   ├── services/
│   │   ├── users-service/       # Puerto 8081
│   │   └── appointments-service/ # Puerto 8082
│   ├── scripts/
│   ├── db/
│   └── docs/
│
├── ADRs/                          # Architecture Decision Records
├── README.md                      # Este archivo
└── PRD.md                         # Product Requirements Document
```

---

## Versiones

### v1-sincrona (Versión Actual - Comunicación Síncrona)

| Característica | Descripción |
|---------------|-------------|
| **Comunicación** | REST Síncrono entre servicios |
| **Puerto users-service** | 8081 |
| **Puerto appointments** | 8082 |
| **Base de datos** | PostgreSQL (puerto 5432) |
| **Infraestructura** | Solo PostgreSQL |

**Endpoints principales:**

```bash
# users-service (8081)
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"juan","email":"juan@test.com","password":"Pass123!","role":"PATIENT","documentNumber":"12345678","firstName":"Juan","lastName":"Pérez"}'

# appointments-service (8082)
curl -X POST http://localhost:8082/api/appointments \
  -H "Content-Type: application/json" \
  -d '{"patientDocument":"12345678","date":"2026-06-15","time":"10:00","reason":"Chequeo"}'
```

**Iniciar v1-sincrona:**

```bash
cd v1-sincrona/db
chmod +x setup-db.sh
PGPASSWORD=admin123 ./setup-db.sh admin123

cd v1-sincrona/scripts
./start-all-services.sh
```

**Documentación:** [v1-sincrona/README.md](v1-sincrona/README.md)

---

### v2-asincrona (Nueva Versión - Comunicación Asíncrona)

| Característica | Descripción |
|---------------|-------------|
| **Comunicación** | RabbitMQ Asíncrono |
| **Puerto users-service** | 8081 |
| **Puerto appointments** | 8082 |
| **Mensajería** | RabbitMQ (exchange: medical.exchange) |
| **Timeout** | 5 segundos para validación |
| **Base de datos** | PostgreSQL (puertos 5432, 5433) |

**Flujo de validación asíncrona:**

```
appointments-service → RabbitMQ (patient.validation.requests) 
    → users-service (valida en PostgreSQL) 
    → RabbitMQ (patient.validation.responses) 
    → appointments-service (crea o rechaza cita)
```

**Iniciar v2-asincrona:**

```bash
# Levantar infraestructura
cd v2-asincrona
docker-compose up -d

# Compilar servicios
cd services/users-service && mvn package -DskipTests
cd services/appointments-service && mvn package -DskipTests

# Iniciar servicios
cd scripts
./start-users-service.sh
./start-appointments-service.sh
```

**Documentación:** [v2-asincrona/README.md](v2-asincrona/README.md)

---

## Comparación de Arquitecturas

| Aspecto | v1-sincrona | v2-asincrona |
|---------|--------------|---------------|
| **Tipo de comunicación** | REST Síncrono | RabbitMQ Asíncrono |
| **Acoplamiento** | Tight coupling | Loose coupling |
| **Si users-service falla** | Crea citas inválidas | No permite crear citas |
| **Timeout** | Indefinido (bloquea) | 5 segundos |
| **Resiliencia** | Baja | Alta |
| **Infraestructura** | PostgreSQL | RabbitMQ + PostgreSQL |

---

## Documentación Adicional

### v1-sincrona

| Documento | Descripción |
|----------|-------------|
| [README.md](v1-sincrona/README.md) | Guía completa |
| [postman-endpoints.md](v1-sincrona/docs/postman-endpoints.md) | Colección de endpoints |
| [integration-tests.md](v1-sincrona/docs/integration-tests.md) | Tests de integración |

### v2-asincrona

| Documento | Descripción |
|----------|-------------|
| [README.md](v2-asincrona/README.md) | Guía completa |
| [postman-endpoints.md](v2-asincrona/docs/postman-endpoints.md) | Colección de endpoints |
| [integration-tests.md](v2-asincrona/docs/integration-tests.md) | Tests de integración |

---

## Tech Stack

| Componente | Tecnología | Versión |
|------------|------------|---------|
| Lenguaje | Java | 21 |
| Framework | Spring Boot | 3.2.x |
| Messaging | RabbitMQ | 3-management |
| Base de Datos | PostgreSQL | 14+ |
| Build Tool | Maven | 3.9+ |
| testing | JUnit 5 | 5.10.x |
| Contenedores | Docker | Latest |

---

## Próximos Pasos

1. [ ] Implementar professionals-service (manejo de horarios)
2. [ ] Completar funcionalidades de appointments (reprogramar, completar)
3. [ ] Implementar cliente JavaFX
4. [ ] Agregar autenticación con JWT
5. [ ] Implementar API Gateway
6. [ ] Migrar a arquitectura de eventos completa

---

## Licencia

Este proyecto es para fines educativos y de demostración.
