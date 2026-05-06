-- Otorgar permisos al usuario medical_user
-- Ejecutar con usuario postgres

-- Conectar a users_db
\c users_db

-- Otorgar permisos en todas las tablas
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO medical_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO medical_user;

-- Conectar a appointments_db
\c appointments_db

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO medical_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO medical_user;

\echo 'Permisos otorgados correctamente'