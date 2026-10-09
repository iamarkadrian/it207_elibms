# From the legacy NetBeans project to this one

The original project (BSIT 2B-G1, 2021) worked and looked good, but its structure made it hard to change, unsafe to expose and impossible to test. This page lists **what was wrong, in the code you can still find in git history**, and what replaced it. It is meant to be read as a worked example of refactoring.

## At a glance

| | Legacy | Now |
|---|---|---|
| Build | Apache Ant + NetBeans project files, jars committed | Maven wrapper, dependencies declared in `pom.xml` |
| Java / libraries | 15, FlatLaf 1.3, mssql-jdbc 9.2 (2021) | 21, FlatLaf 3.7, mssql-jdbc 13, MySQL Connector/J 26 |
| Code | 3 generated classes, 11,062 lines (`LIBRARIAN_DASHBOARD` alone: 8,681) mixing UI, SQL and rules | about 70 small classes in four layers |
| Login | two tabs (hard-coded admin + librarian), global "who is logged in" flag | **one** screen, role comes from the database |
| Database | SQL Server only, T-SQL | MySQL **and** SQL Server |
| Passwords | plain text, 6 characters | PBKDF2 hashes, minimum 8 characters with a letter and a digit |
| Tests | none | ~150 automated tests, including GUI end to end |
| Data | real-looking people | synthetic people (Data Privacy Act) |

## Defects found in the legacy code, and the fix

| # | Legacy problem | Where it was | Replaced by |
|---|---|---|---|
| 1 | **Database password hard-coded** (`...password=bulsulms2021`) in seven places | every dashboard and login method | `AppConfig`: environment / git-ignored `lms.properties` |
| 2 | **Administrator credentials hard-coded** (`"lms.ad"` / `"bsulms"`) | `LOGIN.java` | the administrator is a row in `staff`; one `AuthService` |
| 3 | **Plain-text passwords**, compared inside SQL | `LIBRARIAN.LibrarianPassword varchar(6)` | `PasswordHasher` (PBKDF2-HMAC-SHA256, salted, 600k iterations) |
| 4 | **SQL injection**: queries built by string concatenation, e.g. `"... WHERE StudentNumber = '" + borrower + "'"`, `"DELETE FROM STUDENT WHERE StudentNumber = '" + id + "'"` | borrow, return and remove handlers | `PreparedStatement` for **every** statement |
| 5 | A **new connection for every query, never closed** | all `*_update()` methods | `Database.query/transaction`, try-with-resources |
| 6 | Issuing a book was `INSERT ...; UPDATE ...` in one statement: **not atomic** and two librarians could lend the same copy | `add_book_veriftystep2` | one transaction + conditional `UPDATE ... WHERE available = 1` |
| 7 | **Who is logged in** stored in the database (`LoginStatus = 'Active'`) and read back with `WHERE LoginStatus = 'Active'`: breaks with two sessions, wrong after a crash | `LIBRARIAN_DASHBOARD` constructor | the signed-in `Staff` object is passed to the screens |
| 8 | Lock-out dialog **copy-pasted 12 times** (and `Executors` threads never shut down) | `LOGIN.java` | `AuthService` + `LoginResult` (sealed) + one switch |
| 9 | **26 shared `int a, b, c ... z` fields** used as loop counters across methods | `LIBRARIAN_DASHBOARD` line 37 | local variables |
| 10 | **Wrong code**: "College of Architecture and Fine Arts" saved as `CAL` | `admin_add_addMouseClicked` (a 23-case `switch`) | designations come from the `department` table; combo items carry their own code |
| 11 | Data carried in **hidden table columns** (A-H) | `members_table` etc. | `ObjectTable<T>` holds the real objects |
| 12 | **Errors swallowed or shown raw**: `catch (NumberFormatException) { /* nothing */ }`; `showMessageDialog(null, e)` printing exceptions | many handlers | `UserFacingException` vs `DataAccessException`; one `UiContext.report` |
| 13 | Course codes derived by a **nested `switch` hundreds of lines long** on display names | `add_member_verifystep4` | `course` table; the combo lists the chosen college's courses |
| 14 | Greeting pattern `"yyyy/mm/dd"` (`mm` is *minutes*), hours 21-24 matched two branches | constructor | clean `Mood.at(hour)` |
| 15 | Heading truncated ("Today's Summ...", "Add Librari..."), student number and ISBN stored as numbers (leading zeros lost) | UI and DDL | full text; `VARCHAR` identifiers |
| 16 | Wizard "cancel" handlers duplicated for every step (24 near-identical methods) | all `*cancelstep*` | one `Wizard` class |
| 17 | T-SQL everywhere (`GETDATE()`, `+` concatenation, `isnull`) | SQL strings | portable SQL; dates passed from Java |
| 18 | `ISSUE` and `RETURNED` tables **duplicated** every column | schema | one `loan` table with `returned_at` |

## What the GUI keeps, and what is intentionally different

**Kept**: the colors, fonts (SF Pro Display, falling back to the JVM default), icons and pictures, the sidebar and card layout, the master/detail screens with the round **+** button, the multi-step wizards with the same steps and labels, the greeting banner and clock, the dark administrator dashboard.

**Changed on purpose**

| Change | Why |
|---|---|
| One sign-in; "Sign in as administrator" link removed | the requested single login; the role decides the dashboard |
| Table columns showing a summary instead of eleven squeezed columns | headings were unreadable; details are in the side card; wide tables scroll sideways |
| Review step and detail view are the same card | less code, and the review shows exactly what will be saved |
| Clicking a row in the administrator table fills in the Librarian ID | no more typing numbers |
| Confirmation before removing a member / returning a book; type-the-username before removing a librarian | removals were instant and irreversible |
| Username 4-20 characters, password 8+ with a letter and a digit | six-character passwords are too weak |
| Tables refresh after every change | the legacy tables could show stale rows |
| Members and librarians with history cannot be deleted (with an explanation) | the legacy app threw a foreign-key error and showed nothing |
| A locked account shows the same message as a wrong password | the sign-in screen must not reveal which usernames exist |
| Window can be resized | layout managers instead of absolute pixel positions |

## Where did each legacy file go?

| Legacy | Now |
|---|---|
| `LOGIN.java` | `ui/LoginPanel`, `service/AuthService`, `service/LoginResult` |
| `ADMINISTRATOR_DASHBOARD.java` | `ui/admin/AdminView`, `service/StaffService` |
| `LIBRARIAN_DASHBOARD.java` | `ui/librarian/*` (`LibrarianView`, `Sidebar`, `DashboardScreen`, `MembersScreen`, `CatalogScreen`, `ItemWizards`, `LoansScreen`), `service/*` |
| table `STUDENT` | `student` |
| table `BOOK` (`BookClassification`) | `item` (`item_type`: `BOOK` / `DIGITAL` / `OTHER`) |
| tables `ISSUE` + `RETURNED` | `loan` |
| table `LIBRARIAN` + hard-coded admin | `staff` (`role`) |
| `dist/BulSU_LMS_Query.sql` | `db/mysql`, `db/mssql` (real catalog kept as `db/seed-data/*.csv`; people regenerated) |
| `.form` files (NetBeans designer) | plain Java in `ui/` |

## Suggested exercise

Open `git log` for the original `LIBRARIAN_DASHBOARD.java`, pick one handler (say `add_book_veriftystep2MouseClicked`), and trace it through this codebase: which class validates the student number? which one decides the book can be lent? where is the transaction? Which test proves it?
