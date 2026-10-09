-- BulSU Library Management System - MySQL 8.0.16+ : database and least-privilege application user.
--
-- Run ONCE as an administrator (for example root):
--     mysql -u root -p < db/mysql/00_create_database.sql
--
-- BEFORE running: replace YOUR_STRONG_PASSWORD below, and use the same value in lms.properties
-- (lms.db.password) or the LMS_DB_PASSWORD environment variable. Never commit a real password.

CREATE DATABASE IF NOT EXISTS bulsu_lms
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- The application only needs to read and write rows. It cannot create, alter or drop tables,
-- so a bug (or an attacker) inside the app cannot destroy the schema.
CREATE USER IF NOT EXISTS 'bulsu_lms'@'localhost' IDENTIFIED BY 'YOUR_STRONG_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE ON bulsu_lms.* TO 'bulsu_lms'@'localhost';
FLUSH PRIVILEGES;
