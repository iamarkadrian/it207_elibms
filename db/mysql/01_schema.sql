-- BulSU Library Management System - MySQL 8.0.16+ schema (utf8mb4, InnoDB).
--
--     mysql -u root -p bulsu_lms < db/mysql/01_schema.sql
--     mysql -u root -p bulsu_lms < db/mysql/02_seed.sql     (optional demo data)
--
-- Design notes
--   * Business rules the database can enforce are enforced here too (CHECK, UNIQUE, FOREIGN KEY),
--     so bad data is rejected even if some other program writes to the tables.
--   * All catalog kinds live in one table `item` (single-table inheritance); `item_type` says which.
--   * A loan is one row from issue to return: `returned_at` stays NULL while the item is out.

CREATE TABLE department (
    code VARCHAR(8)   NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_department PRIMARY KEY (code),
    CONSTRAINT uq_department_name UNIQUE (name)
);

CREATE TABLE course (
    code            VARCHAR(16)  NOT NULL,
    name            VARCHAR(120) NOT NULL,
    department_code VARCHAR(8)   NOT NULL,
    CONSTRAINT pk_course PRIMARY KEY (code),
    CONSTRAINT fk_course_department FOREIGN KEY (department_code) REFERENCES department (code)
);

-- Librarians and the administrator share one table; `role` decides the dashboard after sign-in.
CREATE TABLE staff (
    id              INT          NOT NULL AUTO_INCREMENT,
    first_name      VARCHAR(30)  NOT NULL,
    middle_initial  VARCHAR(5)   NULL,
    last_name       VARCHAR(30)  NOT NULL,
    position        VARCHAR(60)  NOT NULL,
    department_code VARCHAR(8)   NOT NULL,
    username        VARCHAR(20)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(10)  NOT NULL,
    locked          BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_attempts INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_staff PRIMARY KEY (id),
    CONSTRAINT uq_staff_username UNIQUE (username),
    CONSTRAINT fk_staff_department FOREIGN KEY (department_code) REFERENCES department (code),
    CONSTRAINT ck_staff_role CHECK (role IN ('ADMIN', 'LIBRARIAN')),
    CONSTRAINT ck_staff_failed CHECK (failed_attempts >= 0)
) AUTO_INCREMENT = 2100101;

CREATE TABLE student (
    student_number  VARCHAR(10) NOT NULL,
    first_name      VARCHAR(30) NOT NULL,
    middle_initial  VARCHAR(5)  NULL,
    last_name       VARCHAR(30) NOT NULL,
    department_code VARCHAR(8)  NOT NULL,
    course_code     VARCHAR(16) NOT NULL,
    section         VARCHAR(5)  NOT NULL,
    membership_date DATE        NOT NULL,
    gender          VARCHAR(1)  NOT NULL,
    contact_number  VARCHAR(11) NULL,
    house_no        VARCHAR(10) NOT NULL,
    street          VARCHAR(60) NOT NULL,
    barangay        VARCHAR(40) NOT NULL,
    municipality    VARCHAR(40) NOT NULL,
    province        VARCHAR(40) NOT NULL,
    CONSTRAINT pk_student PRIMARY KEY (student_number),
    CONSTRAINT fk_student_department FOREIGN KEY (department_code) REFERENCES department (code),
    CONSTRAINT fk_student_course FOREIGN KEY (course_code) REFERENCES course (code),
    CONSTRAINT ck_student_number CHECK (student_number REGEXP '^[0-9]{10}$'),
    CONSTRAINT ck_student_gender CHECK (gender IN ('M', 'F')),
    CONSTRAINT ck_student_contact CHECK (contact_number IS NULL OR contact_number REGEXP '^09[0-9]{9}$')
);

CREATE INDEX idx_student_name ON student (last_name, first_name);

CREATE TABLE item (
    code             VARCHAR(20)  NOT NULL,
    item_type        VARCHAR(10)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    author           VARCHAR(120) NULL,
    publisher        VARCHAR(120) NULL,
    publication_year SMALLINT     NOT NULL,
    genre            VARCHAR(60)  NULL,
    lc_class         VARCHAR(120) NULL,
    pages            INT          NOT NULL,
    borrow_days      INT          NOT NULL DEFAULT 0,
    url              VARCHAR(500) NULL,
    material_type    VARCHAR(10)  NULL,
    available        BOOLEAN      NOT NULL,
    CONSTRAINT pk_item PRIMARY KEY (code),
    CONSTRAINT ck_item_type CHECK (item_type IN ('BOOK', 'DIGITAL', 'OTHER')),
    CONSTRAINT ck_item_pages CHECK (pages > 0),
    CONSTRAINT ck_item_borrow CHECK (borrow_days BETWEEN 0 AND 365),
    CONSTRAINT ck_item_year CHECK (publication_year BETWEEN 1000 AND 2200),
    -- Each kind must carry the columns it needs; other-materials are reference only (never available).
    CONSTRAINT ck_item_shape CHECK (
           (item_type = 'BOOK'    AND author IS NOT NULL AND publisher IS NOT NULL AND genre IS NOT NULL
                                  AND lc_class IS NOT NULL AND borrow_days > 0)
        OR (item_type = 'DIGITAL' AND author IS NOT NULL AND genre IS NOT NULL AND url IS NOT NULL
                                  AND borrow_days > 0 AND available = TRUE)
        OR (item_type = 'OTHER'   AND material_type IN ('JOURNAL', 'MAGAZINE', 'THESIS') AND available = FALSE))
);

CREATE INDEX idx_item_type_title ON item (item_type, title);

CREATE TABLE loan (
    id             INT         NOT NULL AUTO_INCREMENT,
    item_code      VARCHAR(20) NOT NULL,
    student_number VARCHAR(10) NOT NULL,
    issued_by      INT         NOT NULL,
    issued_at      DATETIME    NOT NULL,
    due_at         DATETIME    NOT NULL,
    returned_at    DATETIME    NULL,
    CONSTRAINT pk_loan PRIMARY KEY (id),
    CONSTRAINT fk_loan_item FOREIGN KEY (item_code) REFERENCES item (code),
    CONSTRAINT fk_loan_student FOREIGN KEY (student_number) REFERENCES student (student_number),
    CONSTRAINT fk_loan_staff FOREIGN KEY (issued_by) REFERENCES staff (id),
    CONSTRAINT ck_loan_due CHECK (due_at >= issued_at),
    CONSTRAINT ck_loan_returned CHECK (returned_at IS NULL OR returned_at >= issued_at)
) AUTO_INCREMENT = 1000;

-- Speeds up the Issued / Returned / Missing lists and the dashboard counts.
CREATE INDEX idx_loan_status ON loan (returned_at, due_at);
