# PRD — Sistema de Agendamiento de Citas Médicas
## Red de Servicios Médicos de Piedrazul

**Versión:** 1.0  
**Fecha:** Abril 2026  
**Estado:** Borrador

---

## 1. Resumen ejecutivo

La Red de Servicios Médicos de Piedrazul, ubicada en el kilómetro 5 vía al Huila (Popayán), es una organización sin ánimo de lucro especializada en medicina alternativa: terapia neural, quiropráctica y fisioterapia. Actualmente opera con un software de escritorio monolítico que requiere que el personal médico agende cada cita manualmente vía llamada o WhatsApp, consumiendo tiempo valioso en las tardes.

Este documento define los requisitos del nuevo sistema web de agendamiento, diseñado para automatizar el proceso de citas, liberar al personal médico de la carga administrativa y sentar las bases para una plataforma escalable a múltiples organizaciones de salud.

---

## 2. Problema

| Síntoma | Impacto |
|---|---|
| El personal médico agenda cada cita manualmente (llamada/WhatsApp) | Consume 2-5 horas diarias del tiempo de médicos y funcionarios |
| El sistema de escritorio no permite autogestión del paciente | Dependencia total del personal para cualquier cita |
| Arquitectura monolítica con alto acoplamiento | Difícil de mantener, escalar o evolucionar |
| Sin canal digital de autogestión para pacientes | Experiencia de usuario deficiente |
| Sin soporte multi-organización | No permite expandir el modelo a otros centros |

---

## 3. Objetivos del producto

### Objetivo primario
Reemplazar el flujo manual de agendamiento por una aplicación web que permita al personal de Piedrazul registrar y gestionar citas de forma eficiente, reduciendo el tiempo administrativo de las tardes.

### Objetivos secundarios
- Sentar la base arquitectónica para que, en el futuro, los pacientes puedan agendar sus propias citas de forma autónoma.
- Diseñar el sistema para soportar múltiples organizaciones de salud (multi-tenencia) sin rediseño estructural.
- Preparar integraciones externas (WhatsApp, email, SMS) como extensión futura.

### No objetivos (fuera del alcance actual)
- Autogestión de citas por parte del paciente (fase futura).
- Integraciones reales con WhatsApp, SMS o email (fase futura).
- Aplicación móvil nativa (fase futura).
- Módulo de pagos o facturación.
- Gestión de inventario o farmacia.

---

## 4. Usuarios y roles

### 4.1 Roles del sistema

| Rol | Descripción | Permisos principales |
|---|---|---|
| **Administrador** | Gestiona la configuración global del sistema | Todo: usuarios, médicos, configuración, reportes, auditoría |
| **Agendador de citas** | Personal administrativo que recibe solicitudes y registra citas | Agendar, reagendar, consultar citas, exportar |
| **Médico / Terapista** | Profesional de salud que atiende pacientes | Agendar, reagendar, registrar historia clínica, consultar su agenda |
| **Paciente** *(futuro)* | Usuario externo que agenda su propia cita | Autogestión de citas propias |

### 4.2 Perfil de usuario actual
Los usuarios principales son personal médico y administrativo con nivel técnico básico. La interfaz debe ser simple, en español, y requerir capacitación mínima.

---

## 5. Requerimientos funcionales

### RF-01 · Gestión de usuarios del sistema

El sistema debe permitir crear, editar, consultar y desactivar usuarios.

**Datos requeridos por usuario:**
- Nombre de usuario (login) — único en el sistema
- Contraseña — almacenada de forma cifrada
- Nombre completo
- Rol: Administrador / Agendador / Médico-Terapista
- Estado: activo o inactivo

**Reglas de negocio:**
- Un usuario con rol Médico/Terapista debe poder vincularse al registro del profesional correspondiente.
- Un usuario inactivo no puede iniciar sesión ni realizar acciones.
- El sistema debe registrar en auditoría cualquier creación, modificación o desactivación de usuario.

**Funcionalidad opcional (futura):** recuperación de contraseña por correo electrónico o preguntas de seguridad.

---

### RF-02 · Autenticación y control de acceso

El sistema debe permitir el inicio de sesión mediante usuario y contraseña.

**Reglas de negocio:**
- Cada rol tiene acceso únicamente a las funcionalidades autorizadas para su perfil.
- Las sesiones inactivas deben cerrarse automáticamente tras un tiempo configurable.
- Los intentos fallidos de inicio de sesión deben quedar registrados en auditoría.

---

### RF-03 · Gestión de médicos y terapistas

El sistema debe permitir administrar el catálogo de profesionales de salud.

**Datos requeridos por profesional:**
- Nombres completos
- Tipo: médico o terapista
- Especialidad: Terapia neural / Quiropráctica / Fisioterapia
- Intervalo de atención entre citas (en minutos)
- Estado: activo o inactivo

**Reglas de negocio:**
- Solo los profesionales con estado activo pueden recibir asignación de citas.
- El intervalo de atención determina los slots disponibles en la agenda.

**Funcionalidad opcional (futura):** configuración de horarios de atención por profesional (días y franjas horarias).

---

### RF-04 · Agendamiento manual de citas

El personal con rol Agendador o Médico/Terapista puede registrar citas manualmente.

**Datos requeridos por cita:**
- Número de documento del paciente
- Nombres y apellidos del paciente
- Número de celular
- Género
- Fecha de nacimiento / edad
- Profesional asignado
- Fecha y hora de la cita
- Tipo de atención / especialidad
- Observaciones (opcional)

**Reglas de negocio:**
- No se pueden crear dos citas para el mismo profesional en la misma fecha y hora.
- Solo se pueden asignar citas a profesionales activos.
- El sistema debe respetar el intervalo de atención configurado por profesional.
- Se debe validar que la fecha y hora estén dentro del horario de atención del centro (lunes a viernes, mañanas).

---

### RF-05 · Agendamiento autónomo de citas *(fase futura)*

En una fase posterior, los pacientes podrán acceder con su propio usuario y agendar citas de forma autónoma. El sistema calculará y sugerirá la fecha y hora más cercana disponible según la configuración del profesional.

**Mecanismos de seguridad requeridos en esa fase:**
- Verificación de identidad al registrarse (correo o número telefónico).
- Controles anti-abuso para evitar creación masiva de cuentas o reservas.
- Validación básica de datos ingresados.

---

### RF-06 · Reagendamiento de citas

Los usuarios con rol Agendador o Médico/Terapista pueden modificar la fecha y hora de una cita existente.

**Reglas de negocio:**
- Se debe conservar el historial completo de cambios: fecha anterior, fecha nueva, usuario responsable y fecha/hora del cambio.
- Aplican las mismas validaciones de disponibilidad que en el agendamiento manual.

---

### RF-07 · Exportación de citas

El sistema debe permitir exportar las citas de una fecha específica y un profesional determinado.

**Especificaciones:**
- Formato de salida: CSV (compatible con hojas de cálculo).
- Campos del archivo: hora, nombre completo del paciente, número de documento, celular, observaciones.
- El archivo debe poder abrirse, editarse e imprimirse en Excel u hoja de cálculo equivalente.

---

### RF-08 · Historia clínica básica

El médico o terapista puede registrar el control clínico asociado a cada cita atendida.

**Datos mínimos por registro clínico:**
- Fecha y hora de la atención
- Profesional que realizó el control
- Descripción del procedimiento o control médico realizado

**Reglas de negocio:**
- Solo usuarios con rol Médico/Terapista pueden crear o consultar historias clínicas.
- Los registros clínicos son inmutables una vez guardados, salvo mediante un mecanismo controlado que conserve el historial de modificaciones.
- Toda consulta o modificación queda registrada en auditoría.

---

### RF-09 · Auditoría del sistema

El sistema registra automáticamente todas las acciones relevantes.

**Eventos auditados (mínimo):**
- Creación, modificación y desactivación de usuarios
- Inicio y cierre de sesión (incluyendo intentos fallidos)
- Agendamiento y reagendamiento de citas
- Registro y modificación de historias clínicas
- Exportación de datos

**Datos por evento:**
- Usuario que ejecutó la acción
- Descripción de la acción
- Fecha y hora exacta

**Reglas de negocio:**
- Los registros de auditoría son de solo lectura.
- Solo el Administrador puede consultar los logs de auditoría.
- Ninguna acción crítica puede ejecutarse sin quedar registrada.

---

### RF-10 · Reportes y estadísticas

El sistema genera reportes estadísticos visuales para la toma de decisiones.

**Reportes requeridos:**
- Cantidad de citas por mes
- Cantidad de citas por médico o terapista
- Comparativo por especialidad

**Especificaciones:**
- Los reportes se presentan con tablas y gráficos de barras.
- Filtros mínimos: año, mes, profesional, especialidad.

**Funcionalidad opcional (futura):** exportación de reportes a PDF o Excel.

---

## 6. Requerimientos no funcionales

### RNF-01 · Seguridad y autenticación

- Las contraseñas se almacenan cifradas; nunca en texto plano ni recuperables.
- El acceso a funcionalidades está protegido por control de roles.
- Las sesiones inactivas se cierran automáticamente tras un tiempo configurable por el administrador.

---

### RNF-02 · Trazabilidad y auditoría

- Toda acción crítica queda registrada automáticamente (usuario, acción, fecha/hora).
- Los registros de auditoría son consultables únicamente por el Administrador.

---

### RNF-03 · Usabilidad

- Interfaz en español, clara e intuitiva para usuarios con bajo nivel técnico.
- Lenguaje comprensible, flujos simples y retroalimentación visual ante errores o acciones exitosas.
- Un usuario nuevo debe poder agendar o consultar una cita con capacitación mínima.
- Preparado para internacionalización (i18n) en futuras versiones.

---

### RNF-04 · Concurrencia y consistencia de datos

- El sistema soporta múltiples usuarios simultáneos sin degradar la integridad de la información.
- Dos usuarios no pueden asignar la misma cita al mismo profesional en el mismo horario (control de concurrencia).
- No debe haber pérdida de información, duplicación de citas ni inconsistencias en la agenda.

---

### RNF-05 · Disponibilidad

- Disponibilidad mínima del 99 % durante horario de operación.
- El tiempo de indisponibilidad no debe superar el 1 % del tiempo total de operación.
- En caso de fallo, el sistema debe permitir recuperación rápida minimizando el impacto al usuario.

---

### RNF-06 · Portabilidad

- El sistema debe poder desplegarse en servidores Linux y Windows sin cambios en el código fuente.
- La instalación y configuración deben estar documentadas para facilitar su replicación en distintos entornos.

---

### RNF-07 · Escalabilidad y multi-tenencia

- La arquitectura debe permitir escalar horizontal y verticalmente ante el crecimiento de usuarios y peticiones.
- El diseño debe contemplar la evolución hacia multi-tenencia: múltiples organizaciones de salud con datos aislados entre sí.
- La incorporación de nuevas organizaciones no debe requerir rediseñar la arquitectura base.

---

### RNF-08 · Privacidad y confidencialidad clínica

- El acceso a la historia clínica está restringido exclusivamente a usuarios con rol clínico autorizado.
- Los datos clínicos se almacenan y transmiten de forma segura.
- Toda consulta o modificación de datos clínicos queda trazada en auditoría.
- Ningún usuario sin rol clínico puede ver o modificar la historia clínica de un paciente.

---

## 7. Restricciones

| Restricción | Detalle |
|---|---|
| **Plataforma inicial** | Aplicación web (navegador); móvil en fase futura |
| **Idioma inicial** | Español; preparado para internacionalización futura |
| **Integraciones externas** | No requeridas en la versión inicial (WhatsApp, SMS, email son fase futura) |
| **Autogestión de pacientes** | No incluida en la versión inicial; prevista para fase futura |
| **Multi-tenencia** | No activa en la versión inicial; la arquitectura debe permitirla sin rediseño |

---

## 8. Fases y hoja de ruta

### Fase 1 — MVP (actual)
Sistema web para uso interno del personal de Piedrazul.

- Gestión de usuarios y roles
- Autenticación y control de acceso
- Gestión de médicos y terapistas
- Agendamiento manual de citas
- Reagendamiento de citas
- Exportación de citas a CSV
- Historia clínica básica
- Auditoría del sistema
- Reportes y estadísticas básicos

### Fase 2 — Autogestión del paciente
- Registro y autenticación de pacientes
- Asistente de agendamiento autónomo
- Verificación de identidad (correo/teléfono)
- Controles anti-abuso

### Fase 3 — Integraciones y notificaciones
- Notificaciones por email
- Notificaciones por WhatsApp / SMS
- Recordatorios automáticos de citas

### Fase 4 — Multi-tenencia y móvil
- Soporte para múltiples organizaciones de salud
- Aislamiento de datos por organización
- Aplicación móvil (iOS / Android)
- Internacionalización de la interfaz

---

## 9. Criterios de aceptación global

| Criterio | Condición de éxito |
|---|---|
| Agendamiento sin duplicados | Dos usuarios no pueden reservar el mismo slot para el mismo médico |
| Integridad de historia clínica | Solo usuarios con rol clínico acceden a registros clínicos |
| Auditoría completa | Toda acción crítica queda registrada sin excepción |
| Exportación funcional | El CSV generado se abre y se imprime correctamente en Excel |
| Seguridad de contraseñas | Ninguna contraseña es legible en texto plano en la base de datos |
| Disponibilidad | Uptime ≥ 99 % en periodos de operación activa |
| Usabilidad | Un usuario nuevo agenda una cita en menos de 5 minutos sin asistencia |

---

## 10. Glosario

| Término | Definición |
|---|---|
| **Cita** | Reserva de un slot horario con un profesional de salud para un paciente |
| **Slot** | Franja de tiempo disponible determinada por el intervalo de atención del profesional |
| **Reagendamiento** | Modificación de la fecha u hora de una cita ya existente |
| **Historia clínica** | Registro del control o procedimiento médico realizado en una cita |
| **Multi-tenencia** | Capacidad del sistema de gestionar datos de múltiples organizaciones de forma aislada |
| **Auditoría** | Registro automático de acciones relevantes realizadas por los usuarios del sistema |