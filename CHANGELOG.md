# Changelog

All notable changes are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [1.0.0] - 2026-10-09

First release of the modernised system, rebuilt from the 2021 NetBeans project. See [docs/MIGRATION.md](docs/MIGRATION.md) for the full story.

### Added
- **MySQL 8 and SQL Server** support from one code base, with `db/mysql` and `db/mssql` scripts (create, schema, seed, reset).
- **Single sign-in** for librarians and the administrator; the account's role opens the matching dashboard.
- Object model with abstract, sealed hierarchies (`LibraryItem` ► `PrintedBook`/`DigitalBook`/`OtherMaterial`, `Person` ► `Student`/`Staff`) and the `Displayable` interface.
- Layered architecture (`domain`, `persistence`, `security`, `service`, `ui`) with constructor injection.
- PBKDF2 password hashing, password policy, lock-out and throttling.
- Transactional lending/returning that is safe under concurrent borrowers.
- Reusable Swing toolkit (`Field`, `FormPanel`, `Wizard`, `ObjectTable`, `DetailCard`, `Tasks`) reproducing the original design.
- ~600 synthetic members, 630 catalog entries, ~1,000 loans and 30 staff accounts as demo data.
- ~150 automated tests: unit, database integration (including constraints and a concurrency race) and GUI end to end.
- Service-layer authorization (administrator-only operations, issuing-librarian re-check), a uniform sign-in message, plain-text rendering of user data, and a test-database safety guard.
- GitHub Actions: unit tests, then the full suite on MySQL and SQL Server containers.
- Documentation: README, architecture, database, rubric map, testing, migration, generated UML, generated screenshots.

### Changed
- Build moved from Ant to Maven (with wrapper); Java 15 ➜ 21; FlatLaf 1.3 ➜ 3.7; mssql-jdbc 9.2 ➜ 13; added MySQL Connector/J 26.
- `ISSUE` and `RETURNED` tables merged into one `loan` table; `BOOK` renamed `item`; `LIBRARIAN` generalised to `staff`.
- Absolute layouts replaced by layout managers; the window is now resizable.

### Fixed
- Hard-coded database and administrator credentials; plain-text passwords; SQL injection; unclosed connections; non-atomic lending (see the table in MIGRATION.md).
- "College of Architecture and Fine Arts" being saved with the code of another college.
- Tables and counters showing stale data after adding or removing records.

### Removed
- The NetBeans project files, generated `.form` files and committed third-party jars/samples.
