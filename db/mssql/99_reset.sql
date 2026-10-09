-- DESTRUCTIVE: drops every LMS table and all of its data.
-- Use it to start over (then re-run 01_schema.sql and 02_seed.sql):
--     sqlcmd -S localhost -d bulsu_lms -U sa -i db/mssql/99_reset.sql

DROP TABLE IF EXISTS loan;
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS staff;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS department;
