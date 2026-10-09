-- BulSU Library Management System - SQL Server 2019+ schema.
--
--     sqlcmd -S localhost -d bulsu_lms -U sa -i db/mssql/01_schema.sql
--     sqlcmd -S localhost -d bulsu_lms -U sa -I -i db/mssql/02_seed.sql     (optional demo data)
--
-- Mirrors db/mysql/01_schema.sql. Differences are only dialect: NVARCHAR for Unicode text,
-- BIT for booleans, DATETIME2(0) for timestamps, IDENTITY for auto-numbering, LIKE patterns in
-- CHECK constraints (SQL Server has no REGEXP), and explicit indexes on foreign keys.

CREATE TABLE department (
    code NVARCHAR(8)   NOT NULL,
    name NVARCHAR(100) NOT NULL,
    CONSTRAINT pk_department PRIMARY KEY (code),
    CONSTRAINT uq_department_name UNIQUE (name)
);

CREATE TABLE course (
    code            NVARCHAR(16)  NOT NULL,
    name            NVARCHAR(120) NOT NULL,
    department_code NVARCHAR(8)   NOT NULL,
    CONSTRAINT pk_course PRIMARY KEY (code),
    CONSTRAINT fk_course_department FOREIGN KEY (department_code) REFERENCES department (code)
);

CREATE INDEX ix_course_department ON course (department_code);

-- Librarians and the administrator share one table; `role` decides the dashboard after sign-in.
CREATE TABLE staff (
    id              INT           NOT NULL IDENTITY(2100101, 1),
    first_name      NVARCHAR(30)  NOT NULL,
    middle_initial  NVARCHAR(5)   NULL,
    last_name       NVARCHAR(30)  NOT NULL,
    position        NVARCHAR(60)  NOT NULL,
    department_code NVARCHAR(8)   NOT NULL,
    username        NVARCHAR(20)  NOT NULL,
    password_hash   NVARCHAR(255) NOT NULL,
    role            NVARCHAR(10)  NOT NULL,
    locked          BIT           NOT NULL CONSTRAINT df_staff_locked DEFAULT 0,
    failed_attempts INT           NOT NULL CONSTRAINT df_staff_failed DEFAULT 0,
    created_at      DATETIME2(0)  NOT NULL CONSTRAINT df_staff_created DEFAULT SYSDATETIME(),
    CONSTRAINT pk_staff PRIMARY KEY (id),
    CONSTRAINT uq_staff_username UNIQUE (username),
    CONSTRAINT fk_staff_department FOREIGN KEY (department_code) REFERENCES department (code),
    CONSTRAINT ck_staff_role CHECK (role IN (N'ADMIN', N'LIBRARIAN')),
    CONSTRAINT ck_staff_failed CHECK (failed_attempts >= 0)
);

CREATE INDEX ix_staff_department ON staff (department_code);

CREATE TABLE student (
    student_number  NVARCHAR(10) NOT NULL,
    first_name      NVARCHAR(30) NOT NULL,
    middle_initial  NVARCHAR(5)  NULL,
    last_name       NVARCHAR(30) NOT NULL,
    department_code NVARCHAR(8)  NOT NULL,
    course_code     NVARCHAR(16) NOT NULL,
    section         NVARCHAR(5)  NOT NULL,
    membership_date DATE         NOT NULL,
    gender          NVARCHAR(1)  NOT NULL,
    contact_number  NVARCHAR(11) NULL,
    house_no        NVARCHAR(10) NOT NULL,
    street          NVARCHAR(60) NOT NULL,
    barangay        NVARCHAR(40) NOT NULL,
    municipality    NVARCHAR(40) NOT NULL,
    province        NVARCHAR(40) NOT NULL,
    CONSTRAINT pk_student PRIMARY KEY (student_number),
    CONSTRAINT fk_student_department FOREIGN KEY (department_code) REFERENCES department (code),
    CONSTRAINT fk_student_course FOREIGN KEY (course_code) REFERENCES course (code),
    CONSTRAINT ck_student_number CHECK (LEN(student_number) = 10 AND student_number NOT LIKE N'%[^0-9]%'),
    CONSTRAINT ck_student_gender CHECK (gender IN (N'M', N'F')),
    CONSTRAINT ck_student_contact CHECK (contact_number IS NULL
        OR (LEN(contact_number) = 11 AND contact_number LIKE N'09%' AND contact_number NOT LIKE N'%[^0-9]%'))
);

CREATE INDEX ix_student_name ON student (last_name, first_name);
CREATE INDEX ix_student_department ON student (department_code);
CREATE INDEX ix_student_course ON student (course_code);

CREATE TABLE item (
    code             NVARCHAR(20)  NOT NULL,
    item_type        NVARCHAR(10)  NOT NULL,
    title            NVARCHAR(255) NOT NULL,
    author           NVARCHAR(120) NULL,
    publisher        NVARCHAR(120) NULL,
    publication_year SMALLINT      NOT NULL,
    genre            NVARCHAR(60)  NULL,
    lc_class         NVARCHAR(120) NULL,
    pages            INT           NOT NULL,
    borrow_days      INT           NOT NULL CONSTRAINT df_item_borrow DEFAULT 0,
    url              NVARCHAR(500) NULL,
    material_type    NVARCHAR(10)  NULL,
    available        BIT           NOT NULL,
    CONSTRAINT pk_item PRIMARY KEY (code),
    CONSTRAINT ck_item_type CHECK (item_type IN (N'BOOK', N'DIGITAL', N'OTHER')),
    CONSTRAINT ck_item_pages CHECK (pages > 0),
    CONSTRAINT ck_item_borrow CHECK (borrow_days BETWEEN 0 AND 365),
    CONSTRAINT ck_item_year CHECK (publication_year BETWEEN 1000 AND 2200),
    -- Each kind must carry the columns it needs; other-materials are reference only (never available).
    CONSTRAINT ck_item_shape CHECK (
           (item_type = N'BOOK'    AND author IS NOT NULL AND publisher IS NOT NULL AND genre IS NOT NULL
                                   AND lc_class IS NOT NULL AND borrow_days > 0)
        OR (item_type = N'DIGITAL' AND author IS NOT NULL AND genre IS NOT NULL AND url IS NOT NULL
                                   AND borrow_days > 0 AND available = 1)
        OR (item_type = N'OTHER'   AND material_type IN (N'JOURNAL', N'MAGAZINE', N'THESIS') AND available = 0))
);

CREATE INDEX ix_item_type_title ON item (item_type, title);

CREATE TABLE loan (
    id             INT          NOT NULL IDENTITY(1000, 1),
    item_code      NVARCHAR(20) NOT NULL,
    student_number NVARCHAR(10) NOT NULL,
    issued_by      INT          NOT NULL,
    issued_at      DATETIME2(0) NOT NULL,
    due_at         DATETIME2(0) NOT NULL,
    returned_at    DATETIME2(0) NULL,
    CONSTRAINT pk_loan PRIMARY KEY (id),
    CONSTRAINT fk_loan_item FOREIGN KEY (item_code) REFERENCES item (code),
    CONSTRAINT fk_loan_student FOREIGN KEY (student_number) REFERENCES student (student_number),
    CONSTRAINT fk_loan_staff FOREIGN KEY (issued_by) REFERENCES staff (id),
    CONSTRAINT ck_loan_due CHECK (due_at >= issued_at),
    CONSTRAINT ck_loan_returned CHECK (returned_at IS NULL OR returned_at >= issued_at)
);

-- Speeds up the Issued / Returned / Missing lists and the dashboard counts.
CREATE INDEX ix_loan_status ON loan (returned_at, due_at);
CREATE INDEX ix_loan_item ON loan (item_code);
CREATE INDEX ix_loan_student ON loan (student_number);
CREATE INDEX ix_loan_issued_by ON loan (issued_by);
