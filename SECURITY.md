# Security policy

## Reporting a vulnerability

Please **do not** open a public issue for a security problem. Use GitHub's *private vulnerability reporting* (Security tab ▸ Report a vulnerability) or contact the maintainers privately. Include steps to reproduce and the affected version; do not include real personal data. We aim to acknowledge reports within a few days.

## What the project already does

| Area | Measure | Where |
|---|---|---|
| Injection | Every SQL statement is a `PreparedStatement`; table/column names are constants | `persistence/*` |
| Passwords | PBKDF2-HMAC-SHA256, 600,000 iterations, 16-byte random salt per password; the cost is stored with each hash; constant-time comparison; policy: 8+ characters with a letter and a digit | `security/*` |
| Authorization | Administrator-only operations take the acting user and re-check **in the service, against the database**, that they are an unlocked administrator; lending re-checks that the issuing librarian is still allowed to | `StaffService`, `CirculationService` |
| Brute force | Librarian accounts lock after 3 wrong passwords (an administrator unlocks them); 3 failures within a minute pause sign-in for 15 s; a successful sign-in does **not** reset that pause; the administrator is throttled but never locked, so it cannot be locked out | `AuthService`, `Staff` |
| User enumeration | Unknown user, wrong password and locked account show the **same** message, and each costs one hash verification | `AuthService`, `LoginPanel` |
| Secrets | None in the repository: connection settings come from environment variables or a git-ignored file; `AppConfig.toString()` hides the password; the example placeholder password is refused at start-up | `config/AppConfig` |
| Least privilege | The application's database user can only `SELECT/INSERT/UPDATE/DELETE` (no `DROP`/`ALTER`) | `db/*/00_create_database.sql` |
| Information leaks | SQL errors and stack traces are logged, never shown; the screen gets a generic message | `UiContext.report` |
| UI injection | Swing renders any label starting with `<html>` as a web page (even fetching remote images). Table cells, tooltips, headings and detail lines show user text as plain text | `ObjectTable`, `DetailCard`, `Theme.escapeHtml` |
| Input hygiene | Control characters and bidirectional-override characters are rejected in text fields; digital-book links must be `http(s)` and may not contain credentials | `domain/Check`, `DigitalBook` |
| Data integrity | `CHECK`, `UNIQUE` and foreign keys in the database, plus transactions for lending/returning | `db/*/01_schema.sql`, `Database` |
| Privacy | Demo data is synthetic (Data Privacy Act of 2012) | `db/tools/SeedGenerator.java` |
| Test safety | The integration tests drop every table, so they refuse to run unless the database name ends in `_test` | `TestDatabase` |

## Known limits (read these before real use)

Honest trade-offs, so nobody mistakes this teaching project for a hardened product:

1. **Everyone shares one database login.** Every workstation holds the same `bulsu_lms` credentials in `lms.properties`. The service-layer role checks stop the *application* from letting a librarian act as administrator, but a librarian with access to that file could connect to the database directly and edit the `staff` table. The least-privilege account protects the *schema* (no `DROP`/`ALTER`), not individual rows. A real deployment needs a middle tier or separate database accounts with column-level grants. Treat `lms.properties` like a password.
2. **A lock-out can be abused.** Anyone who knows a librarian's username can lock that account with three wrong guesses; that is the nature of a lock-out. The screen does not reveal which usernames exist or are locked, but usernames in this project follow a guessable pattern. Administrators can unlock; the administrator account cannot be locked.
3. **The 15-second pause is held in memory.** Restarting the program resets it. It slows a guesser; it does not stop a determined one with access to the machine. Persist it (as `failed_attempts` already is for librarians) if that matters.
4. **No automatic password upgrade.** New hashes use the current cost; an existing hash keeps the cost it was created with until that account's password changes.
5. **Failure counting is read-modify-write.** With several application instances guessing at the same instant, counts can be missed. A single `UPDATE ... SET failed_attempts = failed_attempts + 1` would fix that.
6. **No audit log, no password-reset flow, no session timeout.** A librarian removed while signed in keeps the open window until they sign out, although lending and account changes re-check the database.
7. **TLS is up to you.** `encrypt=true;trustServerCertificate=true` in the examples is for a *local development* server with a self-signed certificate. For a database on another machine, use real certificates and drop `trustServerCertificate` (SQL Server) or set `sslMode=VERIFY_CA` / `VERIFY_IDENTITY` (MySQL).

## Before you use it for real

* **Change or delete the demo accounts** (`admin / Admin@2026`, `librarian / Library@2026`). All seeded librarians share one password hash, and nothing forces a change.
* Use a strong database password and a network only the app can reach.
* Treat member records as personal data: collect the minimum, restrict who can sign in, back up securely and define a retention period.
* Enable Dependabot (a configuration is included) and re-run the tests when dependencies change.

## Supported versions

The latest release on `main`.
