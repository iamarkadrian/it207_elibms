-- DESTRUCTIVE: drops every LMS table and all of its data.
-- Use it to start over (then re-run 01_schema.sql and 02_seed.sql):
--     mysql -u root -p bulsu_lms < db/mysql/99_reset.sql

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS loan;
DROP TABLE IF EXISTS item;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS staff;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS department;
SET FOREIGN_KEY_CHECKS = 1;
