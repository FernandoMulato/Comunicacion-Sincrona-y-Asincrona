## Change Proposal: Migrar comunicación síncrona REST a asíncrona con RabbitMQ

**What**: Migrar la comunicación síncrona REST entre appointments-service y users-service para la validación de pacientes a una comunicación asíncrona utilizando RabbitMQ.

**Why**: La actual comunicación síncrona genera un alto acoplamiento y fallas en cascada cuando users-service no está disponible, afectando la creación de citas en appointments-service. Además, no aprovecha los beneficios de arquitecturas desacopladas y resilientes.

**Scope**:
- **IN**: 
  - Creación de usuarios con creación automática de pacientes (users-service)
  - Creación de citas con validación asíncrona de pacientes (appointments-service)
  - Configuración de RabbitMQ en ambos servicios (conexión, exchanges, queues)
  - DTOs para mensajes de solicitud y respuesta de validación de pacientes
  - Manejo de timeouts y correlación de mensajes
- **OUT**:
  - Otros servicios (professionals-service, clinical-records-service, reports-service, audits-service)
  - Interfaz de usuario JavaFX (seguirá consumiendo los mismos endpoints REST de los servicios)
  - API Gateway (mantendrá su configuración actual de enrutamiento)
  - Base de datos PostgreSQL (no se modificarán esquemas existentes)

**Approach**: Opción C - Async Request with Timeout
- **Selected Approach**: appointments-service envía solicitud de validación a una cola de requests, espera respuesta en una cola de replies con timeout configurable (5s). Si timeout, la creación de cita falla de manera controlada.
- **Rationale**: 
  - Mantiene el mismo comportamiento de usuario que la versión síncrona (bloqueo hasta validación) pero con desacoplamiento y resiliencia.
  - Más simple que implementar Request-Response completo (Opción A) evitando complejidades de correlación avanzada.
  - Evita problemas de consistencia de caché (Opción B) mientras proporciona tolerancia a fallas de servicio.
  - Aprovecha colas para bufferizar picos de carga y permitir recuperación tras caídas de users-service.

**Artifacts to Create/Modify**:
1. **users-service** (v2-asincrona/services/users-service/):
   - src/main/java/com/medical/users/config/RabbitMQConfig.java (configuración de connection factory, queues, exchanges)
   - src/main/java/com/medical/users/service/PatientValidationListener.java (consumidor de solicitudes de validación)
   - src/main/java/com/medical/users/service/PatientValidationResponsePublisher.java (publicador de respuestas)
   - src/main/java/com/medical/users/dto/PatientValidationRequestDTO.java
   - src/main/java/com/medical/users/dto/PatientValidationResponseDTO.java
   - Actualización en PatientService.java para delegar validación al nuevo listener (mantenemos interfaz existente)
   - application.yml: agregar configuración de RabbitMQ (host, port, credentials)

2. **appointments-service** (v2-asincrona/services/appointments-service/):
   - src/main/java/com/medical/appointments/config/RabbitMQConfig.java
   - src/main/java/com/medical/appointments/service/PatientValidationRequestPublisher.java (publica solicitudes)
   - src/main/java/com/medical/appointments/service/PatientValidationResponseListener.java (espera respuestas con timeout)
   - src/main/java/com/medical/appointments/dto/PatientValidationRequestDTO.java
   - src/main/java/com/medical/appointments/dto/PatientValidationResponseDTO.java
   - Modificación en AppointmentService.createAppointment() para usar validación asíncrona con timeout
   - application.yml: agregar configuración de RabbitMQ

3. **Shared Considerations**:
   - Los DTOs se duplicarán intencionalmente en ambos servicios para evitar acoplamiento innecesario (reutilizamos entidades existentes pero no creamos nuevos módulos compartidos en esta fase)
   - Se reutilizarán entidades Patient y User existentes sin modificaciones
   - Se mantendrán los mismos endpoints REST externos (los cambios son internos entre servicios)

**Risks and Mitigations**:
- **Risk**: Latencia aumentada en creación de citas por espera asíncrona
  - **Mitigation**: Timeout configurado (5s) y monitoreo de tiempos de cola; escalar consumers en users-service según carga
  
- **Risk**: Pérdida de mensajes si RabbitMQ falla
  - **Mitigation**: Configurar queues como durables, mensajes persistentes, acknowledgments manuales; considerar clustering de RabbitMQ
  
- **Risk**: Duplicado de procesamiento por fallas en acknowledgment
  - **Mitigation**: Diseñar listeners como idempotentes usando correlationId; guardar estado de validación procesada temporalmente
  
- **Risk**: Sobrecarga en users-service por acumulación de mensajes en cola
  - **Mitigation**: Configurar prefetch count apropiado; monitorear profundidad de cola; escalar horizontalmente users-service
  
- **Risk**: Timeout excesivo causando esperas largas en UI
  - **Mitigation**: Timeout ajustable vía configuration; circuit breaker pattern en futuras iteraciones; fallback a cached validation (Opción B) si se necesita

**Next Steps**: Tras aprobación, proceder a fase de Spec para detallar requisitos técnicos y escenarios de comportamiento.