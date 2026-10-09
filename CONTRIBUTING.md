# Contributing

Thank you for improving the BulSU Library Management System. This guide keeps contributions easy to review.

## Set up

1. Install **JDK 21+** and MySQL 8 *or* SQL Server (see the [README](README.md#quick-start)).
2. Fork and clone; create an **empty throw-away database whose name ends in `_test`** (the integration tests drop its tables and refuse any other name).
3. Check that everything is green before you change anything:
   ```bash
   export LMS_TEST_DB_URL=... LMS_TEST_DB_USER=... LMS_TEST_DB_PASSWORD=...   # see docs/TESTING.md
   ./mvnw verify
   ```

## Workflow

* Branch from `main`: `feature/short-name`, `fix/short-name`, `docs/short-name`.
* Keep a pull request to **one concern**. Small PRs get reviewed faster.
* [Conventional Commits](https://www.conventionalcommits.org/) for messages: `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`, `refactor: ...`. Explain *why* in the body.
* Open the PR using the template. A change is ready when `./mvnw verify` passes and CI is green on **both** databases.

## Code style

* Follow `.editorconfig` (UTF-8, LF, 4 spaces) and the surrounding code. No wildcard imports.
* **Layers point down** (`ui → service → persistence/domain`). `domain` must stay free of Swing and SQL. See [ARCHITECTURE.md](docs/ARCHITECTURE.md).
* **Validation belongs in the domain** (a static `requireXxx` validator + setter). Do not re-implement a rule in the UI or a service.
* **SQL**: always `PreparedStatement`, never string concatenation. Keep SQL portable: no `NOW()`, `LIMIT`, `TOP`; pass dates from Java.
* **UI**: no SQL and no business rules in `ui`. Database work goes through `Tasks.run`. Show messages through `Messenger`, never `JOptionPane` directly. Give components a `setName(...)` so tests can find them.
* Prefer small classes and clear names to comments; comment *why*, not *what*.
* **No secrets in commits** (`lms.properties` is git-ignored). Never put real people's data in the seed files or tests.

## Tests are part of the change

| You changed... | Add or update... |
|---|---|
| a domain rule | `DomainModelTest` |
| a repository, SQL or schema | `SchemaIT` / the matching service IT, **in both** `db/mysql` and `db/mssql` |
| a business rule | the matching `*ServiceIT` |
| a screen or wizard | `LoginFlowIT` / `LibrarianFlowIT` / `AdminFlowIT` |
| any class in the UML lists | regenerate the UML (below) |

Write the test first when you can; make it fail for the right reason, then fix it.

## Regenerating generated files

Do not hand-edit these; regenerate and commit the result.

| File | How |
|---|---|
| `docs/uml/class-diagram.md` | `UmlDiagramGenerator` (command in [TESTING.md](docs/TESTING.md#regenerating-generated-files)); `UmlDiagramTest` fails if you forget |
| `db/*/02_seed.sql` | `db/tools/SeedGenerator.java` |
| `docs/screenshots/*.png` | `ScreenshotsIT` |

## Reporting bugs and ideas

Use the issue templates. For a bug, say what you did, what you expected, what happened, and your Java version, OS and database. **Never paste real passwords or personal data.** For security problems, see [SECURITY.md](SECURITY.md) instead of opening a public issue.
