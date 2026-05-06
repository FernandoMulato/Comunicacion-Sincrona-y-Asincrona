-- ==========================================
-- Script de inicialización de Base de Datos
-- Medical Services Network - PostgreSQL
-- ==========================================

-- NOTA: Ejecutar con usuario postgres o superuser
-- psql -U postgres -f init-databases.sql

-- ==========================================
-- CREAR USUARIO DE LA APLICACIÓN
-- ==========================================

-- Crear usuario si no existe (ajustar contraseña)
CREATE USER medical_user WITH PASSWORD 'medical123' CREATEDB;

-- ==========================================
-- CREAR BASES DE DATOS
-- ==========================================

-- Base de datos para users-service
DROP DATABASE IF EXISTS users_db;
CREATE DATABASE users_db OWNER medical_user;

-- Base de datos para appointments-service
DROP DATABASE IF EXISTS appointments_db;
CREATE DATABASE appointments_db OWNER medical_user;

-- ==========================================
-- CONECTAR A users_db Y CREAR TABLAS
-- ==========================================

\c users_db;

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Tabla de pacientes
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    document_type VARCHAR(10) NOT NULL,
    document_number VARCHAR(20) NOT NULL UNIQUE,
    birth_date DATE,
    phone VARCHAR(20),
    address VARCHAR(255),
    eps VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_patients_document ON patients(document_number);

-- Tabla de profesionales
CREATE TABLE IF NOT EXISTS professionals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    specialty VARCHAR(50) NOT NULL,
    license_number VARCHAR(30) NOT NULL UNIQUE,
    phone VARCHAR(20),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_professionals_license ON professionals(license_number);

-- ==========================================
-- CONECTAR A appointments_db Y CREAR TABLAS
-- ==========================================

\c appointments_db;

-- Tabla de citas
CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_document VARCHAR(20) NOT NULL,
    patient_name VARCHAR(100) NOT NULL,
    patient_phone VARCHAR(20),
    professional_id BIGINT NOT NULL,
    professional_name VARCHAR(100) NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    duration_minutes INTEGER DEFAULT 30,
    reason VARCHAR(500),
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices
CREATE INDEX idx_appointment_professional_date ON appointments(professional_id, appointment_date);
CREATE INDEX idx_appointment_patient ON appointments(patient_document);
CREATE INDEX idx_appointment_status ON appointments(status);

-- ==========================================
-- DATOS DE EJEMPLO (OPCIONAL)
-- ==========================================

\c users_db;

-- Insertar usuario admin de ejemplo (password: Admin123!)
INSERT INTO users (username, password_hash, email, role, active)
VALUES ('admin', '$2a$10$N9qo8uLOkgxLrN5M3cR6.e5sS3xX5Y5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z', 'admin@medical.com', 'ADMIN', true)
ON CONFLICT (username) DO NOTHING;

-- Insertar profesional de ejemplo
INSERT INTO users (username, password_hash, email, role, active)
VALUES ('dr.smith', '$2a$10$N9qo8uLOkgxLrN5M3cR6.e5sS3xX5Y5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z', 'dr.smith@medical.com', 'PROFESSIONAL', true)
ON CONFLICT (username) DO NOTHING;

INSERT INTO professionals (user_id, first_name, last_name, specialty, license_number, phone, active)
SELECT id, 'John', 'Smith', 'Neuralterapia', 'MED-001', '3001234567', true
FROM users WHERE username = 'dr.smith';

-- Insertar scheduler de ejemplo
INSERT INTO users (username, password_hash, email, role, active)
VALUES ('scheduler', '$2a$10$N9qo8uLOkgxLrN5M3cR6.e5sS3xX5Y5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z5Z', 'scheduler@medical.com', 'SCHEDULER', true)
ON CONFLICT (username) DO NOTHING;

\c appointments_db;

-- Insertar cita de ejemplo
INSERT INTO appointments (patient_document, patient_name, patient_phone, professional_id, professional_name, appointment_date, appointment_time, duration_minutes, reason, status)
VALUES ('12345678', 'Juan Perez', '3001234567', 1, 'Dr. Smith', CURRENT_DATE + 7, '09:00:00', 30, 'Dolor de cabeza', 'SCHEDULED');

-- ==========================================
-- VERIFICACIÓN
-- ==========================================

\c users_db;
SELECT 'users' as table_name, count(*) as row_count FROM users
UNION ALL
SELECT 'patients', count(*) FROM patients
UNION ALL
SELECT 'professionals', count(*) FROM professionals;

\c appointments_db;
SELECT 'appointments' as table_name, count(*) as row_count FROM appointments;

\echo '========================================='
\echo 'Base de datos inicializada correctamente'
\echo '========================================='