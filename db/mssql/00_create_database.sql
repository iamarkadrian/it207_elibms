-- BulSU Library Management System - SQL Server 2019+ : database and least-privilege application login.
--
-- Run ONCE as an administrator, for example in SSMS or:
--     sqlcmd -S localhost -U sa -P <admin-password> -i db/mssql/00_create_database.sql
--
-- BEFORE running: replace YOUR_Strong_Password_1! below, and use the same value in lms.properties
-- (lms.db.password) or the LMS_DB_PASSWORD environment variable. Never commit a real password.
-- (This file uses GO, so run it with SSMS or sqlcmd; the other scripts have no GO.)

IF DB_ID(N'bulsu_lms') IS NULL
    CREATE DATABASE bulsu_lms;
GO

-- The application only needs to read and write rows. It cannot create, alter or drop tables.
IF SUSER_ID(N'bulsu_lms') IS NULL
    CREATE LOGIN bulsu_lms WITH PASSWORD = N'YOUR_Strong_Password_1!', CHECK_POLICY = ON;
GO

USE bulsu_lms;
GO

IF USER_ID(N'bulsu_lms') IS NULL
    CREATE USER bulsu_lms FOR LOGIN bulsu_lms;
ALTER ROLE db_datareader ADD MEMBER bulsu_lms;
ALTER ROLE db_datawriter ADD MEMBER bulsu_lms;
GO
