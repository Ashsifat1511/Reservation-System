# Task 1 · Online Reservation System

A Swing desktop app for booking and cancelling train tickets. It uses JDBC with SQLite, so no database server is needed.

## Requirements

- JDK 17 or newer
- Maven 3.8 or newer

## Build, test, run

```bash
cd "Reservation System"
mvn package                               # compiles, runs 37 unit tests, builds the jar
java -jar target/reservation-system-1.0.0.jar
```

You can also run it without packaging: `mvn compile exec:java`.

Run a single test with `mvn -Dtest=ReservationDbTest#bookFetchCancelRoundTrip test`.

On first run the app creates `reservation.db` in the current directory, along with the tables, a demo user and the sample trains. To start from scratch, delete the file.

## Configuration

| Setting | How | Default |
|---------|-----|---------|
| Database file | `-Dreservation.db=path/to/file.db` | `./reservation.db` |
| Log file | `-Dreservation.log=path/to/file.log` | `./reservation.log` (written as `reservation.log.0`, rotated at 1 MB, 3 files kept) |
| Demo password (first run only) | env var `RESERVATION_DEMO_PASSWORD` | `demo@123` |

Example: `java -Dreservation.db=D:\data\res.db -Dreservation.log=D:\data\res.log -jar target/reservation-system-1.0.0.jar`

## Errors and logs

- Users see a plain message such as "Sorry, we could not save your booking". SQL errors and stack traces are never shown on screen.
- Technical details go to the log file, along with every login attempt (username plus success or failure). **Passwords are never logged.** Control characters in usernames are replaced before logging, so typed input can't fake log lines.
- If the database file is missing, unwritable or damaged, the app shows its path and exits cleanly instead of crashing.

## Backup and rollback

- **Data:** close the app and copy `reservation.db`. To roll back, copy the backup over it. Cancellation is a hard delete, so a backup is the only way to undo one.
- **Schema:** the DB records its schema version (`PRAGMA user_version`, currently 1). An older build refuses to open a DB written by a newer one instead of damaging it.
- **Code:** this folder is not under git yet. Running `git init` is recommended so any change can be reverted.

## Login

| Username | Password |
|----------|----------|
| `demo`   | `demo@123` |

To use a different demo password, set the `RESERVATION_DEMO_PASSWORD` environment variable **before the first run**, while the database is still being created. Passwords are stored as salted PBKDF2-HMAC-SHA256 hashes (210,000 iterations). Plaintext passwords are never written to disk.

## Sample trains

Typing a number from this list fills in the train name automatically. Any other number is rejected.

| Number | Name |
|--------|------|
| 12301 | Rajdhani Express |
| 12002 | Shatabdi Express |
| 12951 | Mumbai Rajdhani |
| 12627 | Karnataka Express |
| 12839 | Howrah Mail |
| 22691 | Bengaluru Rajdhani |
| 12259 | Duronto Express |
| 12049 | Gatimaan Express |

## Features (brief checklist)

| Brief item | Where |
|------------|-------|
| Login form; access denied for invalid credentials | `ui/LoginFrame`, `db/UserDao` |
| Reservation form: passenger, train number, train name (auto-filled), class, date, source, destination | `ui/ReservationPanel` |
| Insert/Book saves to the DB and generates a unique PNR | `service/ReservationService`, `service/PnrGenerator` |
| Confirmation dialog with the booking details | `ReservationPanel#showConfirmation` |
| Cancellation form: PNR + Fetch shows the full booking | `ui/CancellationPanel` |
| "Are you sure?" dialog, then the row is deleted | `CancellationPanel#cancel` |
| Validation: required fields, valid date, numeric train number | `service/BookingValidator` |

### Rules chosen where the brief is silent

- **PNR:** 10 random digits from `SecureRandom`, never starting with 0. It is checked against the DB and regenerated on a collision, and the `PRIMARY KEY` is the final guard.
- **Date:** must be `yyyy-MM-dd` and a real calendar date (`2026-02-30` is rejected). It cannot be in the past; today is allowed.
- **Train number:** digits only, and it must exist in the `trains` table.
- **Source / destination:** must be different from each other (case-insensitive).
- **Cancellation:** this is a hard delete, as the brief says ("removes the booking"). Any logged-in user can cancel any PNR, because the brief doesn't limit cancellation to the person who booked.

## Security notes

- **No secrets in code** apart from the documented demo password default, and only its hash is ever stored. There are no API keys or DB credentials, because SQLite is a local file.
- **SQL injection:** every statement uses `PreparedStatement`. Tests check that `' OR '1'='1` logins and PNRs, and `'); DROP TABLE` names, are stored or rejected literally.
- **Validation:** checks run twice. The form checks first for fast feedback, then `ReservationService` re-checks everything and looks up the train name from the DB, so data can't get past the rules even if the UI is bypassed. Text fields have length limits (name 100, station 60) and reject control characters. The same length limits are enforced by `CHECK` constraints, and foreign keys stop bookings for unknown trains or users.
- **Login:** salted PBKDF2. An unknown username takes as long to reject as a wrong password, so timing doesn't reveal which accounts exist. Stored hashes with an out-of-range iteration count are rejected. Password checks run off the UI thread, and password buffers are wiped after use.
- **Dependencies:** `sqlite-jdbc` 3.53.4.0, which bundles a SQLite build newer than the fixes for CVE-2025-29087/29088 and CVE-2025-6965.
- **Two windows open at once:** connections wait up to 5 s for a lock (`busy_timeout`) instead of failing. A test runs 40 concurrent bookings from two instances.
- **Limitations:** the DB file is not encrypted, so anyone with access to it can read the bookings. There is no login lockout after repeated failures, and any logged-in user can cancel any PNR. See `ai/known-issues.md`.

## Project layout

```
src/main/java/com/oasis/reservation/
├── Main.java, AppContext.java, AppLog.java (file logging)
├── db/        Database (schema + seed), UserDao, TrainDao, ReservationDao
├── model/     Reservation (record)
├── security/  PasswordHasher (PBKDF2)
├── service/   BookingValidator, PnrGenerator, ReservationService, ValidationException
└── ui/        LoginFrame, MainFrame, ReservationPanel, CancellationPanel, Errors
src/test/java/com/oasis/reservation/   PasswordHasherTest (5), BookingValidatorTest (8),
                                       ReservationDbTest (7), SecurityAndEdgeCaseTest (17)
```
