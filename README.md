# QuizMaster — SPPU OOP & Computer Graphics Practice System

QuizMaster is a Java 17 Windows desktop application for syllabus-based MCQ practice. Students register or sign in, choose an active subject, practice its unit quizzes, and review saved attempt history. Teachers sign in, select subjects they own, manage those subjects' question banks, and review saved results. Computer Graphics content is delivered through quiz questions; there is no separate lab module.

This is a local educational mini-project. It is not a server-secure multi-user service. Anyone with access to the computer and application files may inspect or alter local CSV data or run a modified build. Local accounts and teacher ownership checks organize normal application use; they do not protect against a malicious person with operating-system or source access.

## First run and demo accounts

The application initializes the following accounts only when the IDs are absent. A later launch never resets an existing account or password:

| Role | ID | Initial password |
|---|---|---|
| Student | `STU1001` | `Student@123` |
| Teacher | `TCH1001` | `Teacher@123` |

These are demo-only credentials. Change them through **Change password** after signing in before using real personal data. Passwords are stored as salted PBKDF2-HMAC-SHA-256 hashes with a per-account random salt and 310,000 iterations; plain-text passwords are not written to account CSVs or logs. Passwords must be at least 10 characters and contain a letter and a digit. A forgotten password has no email recovery: for a local demo, sign in with another authorized teacher to add a teacher account, or use the original account if it has not been changed. There is no in-app forgotten-password reset. A trusted local administrator must perform a deliberate hash reset in local account storage after taking a backup; do not delete or replace whole data files because they also contain profile history and subject ownership.

The welcome screen offers only **Student Login** and **Teacher Login**. Each login form has a small registration link directly below its fields.

Student registration asks for a display name and password confirmation. QuizMaster assigns the next unused `STU####` ID from saved student accounts, displays it after registration, and does not sign the student in automatically. Use that ID and password on Student Login. Existing pre-account profiles can use the explicit **Have an existing profile? Set its password** flow: the saved ID and exact saved display name must match, a new password is stored, and login is still required. A legacy profile is never logged in without a password.

Teacher registration asks for a name, one-time invitation code, and password confirmation. It assigns the next unused `TCH####` ID, displays it, and returns the user to Teacher Login. A currently signed-in teacher selects **Generate Teacher Invitation Code** in the owned-subject workspace, then shares the displayed code with the intended registrant. The dialog includes **Copy code**. The teacher-registration link beneath Teacher Login explains this policy before submission. The code is stored only as a SHA-256 digest, remains valid across restarts until used, and is consumed on account creation. Choosing Teacher on the welcome page alone grants no access. The initial trusted teacher account is `TCH1001`.

After student login, the next screen is always **Choose a subject**. It lists only active subjects from `subjects.csv`; the student must select a subject card before reaching the dashboard. The dashboard's **Switch subject** action clears the current subject context and returns to the picker. Teacher login opens **Your teaching subjects**, showing only subjects owned by that teacher; **Create subject** is available when there are no owned subjects.
## Subjects and ownership

The only seeded subject is **OOP & Computer Graphics**, with stable ID `PCC202-OOP-CG`; its owner is `TCH1001`. Existing legacy OOP questions and results are interpreted as belonging to this subject. No unrelated subjects or scores are seeded. Students see active subjects. Teachers see and manage only subjects whose owner ID matches their authenticated teacher account. An authenticated teacher can create more subjects using **Create subject**; question management and results are scoped to the selected owned subject. New subjects currently share generic Unit I–V categories; custom per-subject syllabus structures are not yet configurable.

Subject data is stored in `data/subjects.csv`. New question rows and submitted result summaries include a subject ID in `data/questions.csv` and `data/results.csv`. Existing legacy files remain readable and are mapped to the OOP subject until the first successful write in the new schema; at that time the old file is retained as `questions.csv.pre-subject-migration.bak` or `results.csv.pre-subject-migration.bak`. Student profile upgrades similarly retain `students.csv.pre-account-migration.bak`. Migrations preserve old rows and map old OOP records to the initial subject. If input data is malformed or the header is unrecognized, writes are blocked where needed rather than silently replacing the file.

## Syllabus mapping

The app groups questions under the five configured PCC-202 areas:

1. Unit I — OOP fundamentals and control structures
2. Unit II — Classes, objects, arrays, and input/output
3. Unit III — Inheritance, polymorphism, exception handling, and multithreading
4. Unit IV — Computer Graphics primitives, scan conversion, polygon filling, and clipping
5. Unit V — 2D/3D transformations and projections

The starter bank is a small sample across those areas, not complete syllabus coverage. Teachers can expand it from the question manager.

## Data files

All application data is local under `data/` and uses UTF-8 CSV with quoted-field escaping:

- `students.csv` — student ID, display name, password salt/hash metadata
- `teachers.csv` — teacher ID, display name, password salt/hash metadata
- `teacher-invitations.csv` — one-use invitation hashes and owner IDs (created when an authorized teacher issues an invite)
- `subjects.csv` — subject ID/name/description/owner/status/timestamp
- `questions.csv` — subject-scoped four-option question bank
- `results.csv` — submitted-attempt summaries only; it does not store answers or answer keys

The entire `data/` directory is installation-local and is excluded from Git. It can contain student and teacher accounts, password hashes, results, teacher-issued invitation hashes, teacher-authored questions, and migration backups. Do not commit or copy a personal `data/` directory to GitHub. The five public starter OOP/Computer Graphics questions are defined in `StarterQuestionBank.java`; the application seeds them into an empty local question bank, so a CSV fixture is not needed in the repository. These sample questions contain no personal information.

Result history preserves the submitted score, counts, quiz identity, student display-name snapshot, subject identity, and timestamp. Older result formats did not preserve an answer-by-answer selection snapshot or question wording/explanation. Those details cannot be reconstructed reliably after the question bank changes, so historical details are limited to the summary saved at the time.

The leaderboard ranks each submitted attempt, including multiple attempts by the same student, only within a matching subject and stable quiz identity. It sorts by correct answers descending, percentage descending, completion time ascending, then deterministic student/attempt IDs. Exact ties on the ranking fields use competition rank (`1, 1, 3`).

## Java and OOP concepts

The source is organized into models, storage, services/security, and Swing UI packages. Encapsulated models represent questions, accounts, sessions, subjects, quizzes, attempts, and results. Services enforce role and subject-owner rules. Collections hold loaded records and quiz state. CSV stores isolate file I/O and use exceptions to report malformed or unavailable data. The Swing UI uses reusable theme helpers, `CardLayout`, and standard layout managers. Password hashing uses the Java standard library (`PBKDF2WithHmacSHA256`).

## Build, run, and test

Requirements: Windows and a Java 17 JDK (including `javac`). VS Code is optional. No external libraries or package installation are required.

### Set up another Windows laptop

1. Install a Java 17 JDK. In Windows Environment Variables, set `JAVA_HOME` to the JDK installation folder (the folder containing `bin`, not the `bin` folder). For example: `C:\Program Files\Eclipse Adoptium\jdk-17...`.
2. Add `%JAVA_HOME%\bin` to `Path`, then open a new terminal and confirm both `java -version` and `javac -version` report version 17.
3. Clone the GitHub repository and open a terminal in the cloned `QuizMaster` folder. Do not copy another computer's `data/` directory into the clone.
4. Build and run:

```bat
.\build.bat
.\test.bat
.\run.bat
```

The app creates its local `data/` directory and missing CSV files automatically. On a clean install it seeds the documented demo student and teacher accounts, the single **OOP & Computer Graphics** subject, and the five original starter questions. It initializes the starter question bank only when no questions exist and never replaces an existing account or question bank. Result and teacher-invitation files are created as those features are first accessed. The local data stays outside the Git repository.

`test.bat` compiles the application and dependency-free test harness, then runs the Phase 2–5 regression checks and Phase 7 account, subject, and migration checks, including fresh-JVM persistence checks. The tests use temporary data under ignored `build/` output; they do not use or reset a developer's live `data/` files.

`build.bat` and `test.bat` use `%JAVA_HOME%\bin\javac.exe` and `%JAVA_HOME%\bin\java.exe`. Set `JAVA_HOME` as above before running them. `run.bat` starts the compiled app and attempts a build first if the compiled entry point is missing.

## GitHub upload safety

Commit `.gitignore`, this README, the three `.bat` scripts, and the `src/main/java/` and `src/test/java/` source trees. Do not commit `data/`, generated `build/` output, local IDE settings, logs, temporary GUI-check files, credentials, or backups. The demo passwords documented above are public sample credentials, not private user records; change them before using real student information. The repository does not need any live CSV files to initialize a fresh installation.

## Known limitations

- Data and authentication are local; there is no server, remote account recovery, encryption-at-rest, or protection from OS-level access.
- The initial demo passwords are public in this README and should be changed before real use.
- Teacher account registration is controlled by an authenticated existing teacher. Signed-in users can change their own password from the subject-selection screen. There is no self-service forgotten-password recovery; local account recovery requires a trusted computer administrator and a deliberate account-hash reset after backing up data. The first-run seeder never resets accounts.
- Results are summaries, not immutable exam records. Old score summaries remain associated with the initial OOP subject during migration, but historic answer-level review cannot be recovered if it was not saved.









