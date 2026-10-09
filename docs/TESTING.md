# Testing

The suite has three layers. Each answers a different question and runs at a different speed.

| Layer | Files | Needs a database? | Answers |
|---|---|---|---|
| **Unit** (`*Test`) | `DomainModelTest`, `PasswordHasherTest`, `AppConfigTest`, `UmlDiagramTest` | no | Do the objects enforce their rules? Is the UML current? |
| **Integration** (`*IT`, services and SQL) | `SchemaIT`, `AuthServiceIT`, `StaffServiceIT`, `MemberCatalogIT`, `CirculationIT` | yes | Do the SQL scripts, repositories and services work together, including constraints, transactions and a concurrent race? |
| **GUI end-to-end** (`*IT`, screens) | `LoginFlowIT`, `LibrarianFlowIT`, `AdminFlowIT` | yes | Does a user clicking through the real components get the right result *in the database*? |

Unit tests run in Surefire (`./mvnw test`); `*IT` classes run in Failsafe during `./mvnw verify`.

## Running

```bash
./mvnw test                      # unit tests only, a few seconds, no setup
./mvnw verify                    # everything; integration tests need the variables below
./mvnw verify -Dit.test=LibrarianFlowIT      # one class
```

### Pointing the integration tests at a database

> ⚠️ The first integration test **drops all tables** of the target database and reloads `01_schema.sql` + `02_seed.sql`.
> Use an empty, throw-away database. As a safety net the tests **refuse to run unless the database name ends in `_test`**
> (`bulsu_lms_test`); `LMS_TEST_ALLOW_ANY_DB=true` overrides that, so only use it if you are certain.

| Variable | Meaning |
|---|---|
| `LMS_TEST_DB_URL` | JDBC URL of the throw-away database. If unset, integration tests are **skipped**. |
| `LMS_TEST_DB_USER`, `LMS_TEST_DB_PASSWORD` | the account the services run as |
| `LMS_TEST_ADMIN_USER`, `LMS_TEST_ADMIN_PASSWORD` | optional: an account that may create tables (defaults to the pair above) |
| `LMS_TEST_ADMIN_URL` | optional, SQL Server only: a URL for `master`, used to create the database if it is missing |
| `LMS_REQUIRE_DB=true` | fail (instead of skip) when no database is configured. CI sets this so a misconfigured pipeline cannot go green silently. |

```bash
# MySQL, for example in a throw-away container
docker run -d -p 3307:3306 -e MYSQL_ROOT_PASSWORD=rootpw -e MYSQL_DATABASE=bulsu_lms_test \
           -e MYSQL_USER=lms -e MYSQL_PASSWORD=lmspw mysql:8.0
export LMS_TEST_DB_URL=jdbc:mysql://127.0.0.1:3307/bulsu_lms_test LMS_TEST_DB_USER=lms LMS_TEST_DB_PASSWORD=lmspw
./mvnw verify
```

## How the GUI tests work

`testsupport/SwingTestKit` drives *real* Swing components without opening a window:

* **find** a component by the `name` the application gave it (`"signIn"`, `"studentNumber"`, `"members.table"`, `"primary"`, ...). Only **visible** components count, so a card hidden by a `CardLayout` cannot be clicked, exactly as for a human.
* **act**: `click`, `type`, `choose` (combo box), `selectRow`.
* **wait**: `settle()` blocks until every background `Tasks.run(...)` has finished, so assertions never race the UI.
* **render**: lay out and paint any component to a PNG (this is how the README screenshots are made).

`testsupport/GuiHarness` assembles the whole app (login → dashboards) over a real database, with a `RecordingMessenger` in place of `JOptionPane`; tests assert on `messenger.lastWarning()` and so on. `TestClock` moves "now" forward to create overdue loans without waiting.

Because nothing needs a display, the same tests run on a build server.

> What these tests do *not* cover: pixel-exact look on every operating system or display scale. Run the app once by eye after UI changes (`java -jar target/bulsu-lms.jar`).

## What is covered

* every validation rule of the object model, both accepted and rejected input
* password hashing (salting, cost, malformed input) and the password policy
* the SQL scripts: volume, every row loads into a domain object, internal consistency (availability ⇄ open loans), and that the database itself **rejects** bad rows (unique, foreign key, `CHECK`)
* sign-in for both roles, lock-out after three failures, throttling, unlocking by an administrator, never locking the administrator
* members, catalog (all three kinds, Unicode round-trip), lending, returning, overdue detection, dashboard numbers, **eight concurrent borrowers racing for one copy**
* each wizard end to end, validation messages on the step where the mistake happens, cancel confirmations, EDIT going back without losing answers
* the administrator forms, the type-to-confirm removal, refusal to remove people who have history
* the UML file equals the code

## Continuous integration

`.github/workflows/ci.yml` runs on every push and pull request:

| Job | Runs against |
|---|---|
| `unit` | no database (JDK 21) |
| `mysql` | a `mysql:8.0` service container: full `./mvnw verify` |
| `mssql` | a SQL Server 2022 service container: the **same** `./mvnw verify` |

The SQL Server job is what proves the T-SQL scripts and the portable SQL in the repositories; if you change either, watch that job.

## Regenerating generated files

| File | Command |
|---|---|
| `docs/screenshots/*.png` | `./mvnw verify -Dit.test=ScreenshotsIT -Dlms.screenshots=docs/screenshots` |
| `docs/uml/class-diagram.md` | `java -cp "target/classes;target/test-classes" edu.bulsu.lms.docs.UmlDiagramGenerator` (use `:` on Linux/macOS) after `./mvnw test-compile` |
| `db/*/02_seed.sql` | `java -cp target/classes db/tools/SeedGenerator.java` after `./mvnw compile` |
