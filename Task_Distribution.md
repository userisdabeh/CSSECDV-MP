# SECURITY Svcs: Task Distribution (Dave, Joshua, Lorenz)

> **Course:** CSSECDV / ITSECWB Machine Project
> **Source:** Secure Web Development Case Project Checklist (29 items x 2 pts = **58 pts**)
> **Goal:** Close every Incomplete/Missing item before the live demo, with each person owning separate files to avoid merge conflicts.
> **Status key:** Done | Partial (drafted, needs finishing or verifying) | Missing

| Person | Workstream | Main files |
|---|---|---|
| **A: Dave** | Authentication, Registration, Validation, Database layer | `PasswordUtil`, `Validator`, `SQLite`, `Main`, `Login`, `Register` |
| **B: Joshua** | Authorization (RBAC), Admin panel, Logging, Error handling UI | `AccessControl`, `Frame`, `AdminHome`, `ErrorHandler` |
| **C: Lorenz** | Role features: Products, Purchases, History | `StaffHome`, `ManagerHome`, `ClientHome` |

---

## Checklist Status Snapshot

| # | Requirement | Status | What is left | Owner |
|---|---|---|---|---|
| 1.1.1 | Admin account (Role 5) | Done | Seeded in `Main.java`. Re-verify once login works. | A |
| 1.1.2 | Staff/Manager accounts (Roles 3, 4) | Done | Seeded in `Main.java`. | A |
| 1.1.3 | Customer account (Role 2) | Done | Seeded (`client1`, `client2`). | A |
| 2.1.1 | Authentication required for all pages | Missing | No `currentUser`; home panels reachable without a session. | B |
| 2.1.2 | Auth fails securely (deny on error) | Partial | Drafted in `loginAction`; verify every error path denies. | A |
| 2.1.3 | Salted one-way password hashes | Partial | PBKDF2 done for seeded users. **Register still saves raw password.** | A |
| 2.1.4 | Generic auth failure message | Partial | Drafted; confirm every failure path uses one message. | A |
| 2.1.5 | Password policy (upper, lower, digit, symbol) | Missing | `Validator` + Register integration. | A |
| 2.1.6 | Min/max password length | Missing | Same as above. | A |
| 2.1.7 | Masked password fields everywhere | Partial | Login drafted. Register (both fields) and change-password dialog pending. | A |
| 2.1.8 | Lockout to Role 1 after exactly 5 failures | Partial | DB side done (`recordFailure`); verify end to end. | A |
| 2.1.9 | Last login timestamp + status on success | Partial | DB side done; drafted in `loginAction`; verify. | A |
| 2.1.10 | Re-authenticate before password change | Missing | No change-password feature yet. | A |
| 2.2.1 | Single site-wide authorization component | Missing | Create `AccessControl`. | B |
| 2.2.2 | UI visibility + logic match role descriptions | Missing | B: nav visibility. C: features inside each panel. | B, C |
| 2.2.3 | Authorization fails securely | Missing | Null/unknown role = deny. | B |
| 2.2.4 | Users routed only to their own Home panel | Missing | Route by role after login. | B |
| 2.3.1 | Reject invalid input (no sanitizing) | Partial | Login only. Each person applies `Validator` to their own forms. | A, B, C |
| 2.3.2 | Numeric range checks (role 1-5, stock, price) | Missing | `SQLite.addUser` accepts any role. | A |
| 2.3.3 | Max character length on every input | Partial | Login only. A provides rules; B and C apply on their forms. | A, B, C |
| 2.3.4 | Prepared statements everywhere | Partial | All existing `SQLite` methods done. **Every new method must follow.** | A, B, C |
| 2.4.1 | No stack traces shown | Missing | `SQLite` still has `System.out.print(ex)` / `printStackTrace()`. | A |
| 2.4.2 | Generic, non-technical error messages | Missing | `ErrorHandler` helper, used in all UI handlers. | B |
| 2.4.3 | Methods wrapped to prevent crashes | Missing | A: DB methods. B and C: UI handlers. | A, B, C |
| 2.4.4 | Unrecoverable error goes to safe panel | Missing | Reset to Login with `currentUser = null`. | B |
| 2.5.1 | Log view/manage restricted to Admin (Role 5) | Missing | `AccessControl` + panel check. | B |
| 2.5.2 | Log all input validation failures | Partial | Login only. Each person logs failures on their own forms. | A, B, C |
| 2.5.3 | Log all auth successes and failures | Partial | Drafted in `loginAction`; verify. | A |
| 2.5.4 | Log access control violations | Missing | `AccessControl` hook. | B |

About 11 items are Missing, 14 are Partial, 3 are Done.

---

## Shared Contracts (agree on these first)

Everyone codes against these names so work merges cleanly. **A and B push stubs on day 1.**

### `Controller/Validator.java` (owner: A)
All methods return `true` only if the input is valid. Callers must **reject** on `false`, never "clean" the value.

| Method | Rule |
|---|---|
| `isValidUsername(String)` | `^[A-Za-z0-9_]{3,20}$` |
| `isValidPassword(char[])` | 8-64 chars; at least 1 upper, 1 lower, 1 digit, 1 symbol |
| `isValidRole(int)` | 1 to 5 |
| `isValidProductName(String)` | `^[A-Za-z0-9 ._-]{1,50}$` |
| `isValidStock(int)` | 0 to 10000 (team to confirm) |
| `isValidPrice(double)` | greater than 0, up to 1000000 (team to confirm) |

### `Controller/AccessControl.java` (owner: B)
- `static boolean canAccess(User user, String panel)` where `panel` is `"admin"`, `"manager"`, `"staff"`, `"client"`, or `"logs"`.
- **Fails closed:** `null` user, unknown role, or any exception returns `false`.
- Role to panel is 1:1 (5 = admin and logs, 4 = manager, 3 = staff, 2 = client, 1 = nothing).

### Session and navigation (owner: B, in `Frame.java`)
- `public User currentUser;` set on login, cleared on logout and on any fatal error.
- `showPanel(String panel)` is the **only** way to switch panels. It calls `AccessControl.canAccess`; on `false` it logs a violation and shows a generic message.
- A's `loginAction` sets `currentUser` and calls `showPanel` for the user's own panel (agree on this hand-off).

### Logging helper (owner: B)
- `main.log(String event, String username, String desc)` writes to `addLogs` with a timestamp.
- Use `"unknown"` as the username when there is none (`logs.username` is `NOT NULL`).
- Truncate attacker-supplied text to 100 chars before logging.

### Error helper (owner: B)
- `ErrorHandler.show(Component parent)` displays: "Something went wrong. Please try again."
- Never show `ex.getMessage()` or a stack trace.

---

## Workstreams

### A (Dave): Authentication, Registration, Validation & Database Layer
**Files:** `Controller/PasswordUtil.java`, `Controller/Validator.java` (new), `Controller/SQLite.java`, `Controller/Main.java`, `View/Login.java`, `View/Register.java`, `Frame.loginAction` / `Frame.registerAction`
**Checklist:** 1.1.x, 2.1.2 to 2.1.10, 2.3.2, 2.4.1, 2.5.3, plus rules and DB support for 2.3.x

**Already done**
- [x] `PasswordUtil`: PBKDF2WithHmacSHA256, 120,000 iterations, 16-byte random salt, constant-time compare
- [x] Seeded accounts for Roles 2, 3, 4, 5, all with hashed passwords (`Main.java`)
- [x] Converted `addUser`, `addHistory`, `addLogs`, `addProduct`, `removeUser`, `getProduct(name)` to prepared statements
- [x] Added `getUser`, `getLastAttempt`, `recordSuccess`, `recordFailure` (5-failure lockout to Role 1)
- [x] Added `failed_attempts`, `last_attempt`, `last_status` columns to `users`
- [x] Fixed `getProduct(name)` crash (missing `rs.next()`) and merged `addUser(user, pass)` into the 3-argument version

**Still to do**
- [ ] Confirm `Login.java` uses `JPasswordField` and calls `frame.loginAction(...)`; confirm `loginAction` is in `Frame.java`
- [ ] Confirm one generic message ("Invalid username and/or password") on every failure path
- [ ] Create `Validator` and **push it on day 1** (B and C depend on it)
- [ ] `SQLite.addUser(username, password, role)` rejects roles outside 1-5
- [ ] `Register.java`: `JPasswordField` for both password fields
- [ ] `Frame.registerAction`: validate username, password policy, length, and match with confirm; **reject** on failure; hash; save as Role 2; log failures
- [ ] Decide how a duplicate username is reported on register (generic error, or document the risk)
- [ ] **Change Password** feature: asks for the current password first (re-authentication), then new password with the same policy, hashes, logs
- [ ] Replace every `System.out.print(ex)` and `ex.printStackTrace()` in `SQLite.java` with an empty catch or a comment
- [ ] `Main.java` cleanup: remove the print loops (they print password hashes) and the fake "User creation successful" sample logs
- [ ] Test: 5 wrong passwords lock the account; the 6th attempt with the right password is still refused; the last-login message is correct
- [ ] Add DB methods other people ask for (e.g. `updateUserRole`, `unlockUser`), prepared statements only

### B (Joshua): Authorization (RBAC), Admin Panel, Logging & Error Handling UI
**Files:** `Controller/AccessControl.java` (new), `Controller/ErrorHandler.java` (new), `View/Frame.java` (navigation, session), `View/AdminHome.java`, logging helper in `Main.java`
**Checklist:** 2.1.1, 2.2.1 to 2.2.4, 2.4.2, 2.4.4, 2.5.1, 2.5.4, plus 2.5.2 / 2.3.x on admin forms

- [ ] Create `AccessControl` (fails closed) and push a stub on day 1
- [ ] Add `currentUser` and `showPanel` to `Frame`; clear `currentUser` on logout
- [ ] Route to the correct Home panel by role right after login (2.2.4)
- [ ] Show only the nav button for the user's own panel; hide or disable the rest (2.2.2)
- [ ] Route **all** panel switches through `showPanel`; log every denial (2.5.4)
- [ ] Each Home panel checks `AccessControl` when shown, as a second layer
- [ ] Block every page except Login/Register when `currentUser == null` (2.1.1)
- [ ] Role 1 never reaches any panel
- [ ] Logging helper `main.log(...)` available to everyone
- [ ] `ErrorHandler.show(...)` and use it in `Frame` and `AdminHome` handlers (2.4.2, 2.4.3)
- [ ] `Thread.setDefaultUncaughtExceptionHandler` in `Main`: show the generic message, clear `currentUser`, return to Login (2.4.4)
- [ ] **Admin panel:** list users; change role (with `Validator.isValidRole`); re-enable a disabled account (reset `failed_attempts`, set role back); delete user
- [ ] **Logs view** loads only for Role 5 (2.5.1)
- [ ] Validate and log failures on admin forms
- [ ] Write the **Access Control Matrix** (Table 5)

### C (Lorenz): Role Features (Products, Purchases, History)
**Files:** `View/StaffHome.java`, `View/ManagerHome.java`, `View/ClientHome.java`, `SQLite.java` (new product/history methods; coordinate with A)
**Checklist:** 2.2.2 (feature side), plus 2.3.x, 2.3.4, 2.4.3, 2.5.2 on product and purchase forms

| Role | Allowed |
|---|---|
| Client (2) | Purchase products; view **own** purchase history only |
| Staff (3) | Add, edit, delete products |
| Manager (4) | Add, edit, delete products; view purchase history of **all** users |
| Admin (5) | Manage users and logs only (not products) |

- [ ] `SQLite.updateProduct(...)` and `SQLite.deleteProduct(...)`, **prepared statements only**
- [ ] `SQLite.getHistory(String username)` for the Client's own history (filter in SQL by `currentUser`, not only in the UI)
- [ ] Purchase flow: validate quantity, check stock, decrement stock and insert history together, then log
- [ ] Product forms validate through `Validator` (name, stock, price, max lengths); **reject**, don't clean
- [ ] Log validation failures on these forms through `main.log(...)`
- [ ] Wrap UI handlers and use `ErrorHandler.show(...)` (no stack traces)
- [ ] Confirm each panel's buttons match the role table above and nothing extra is visible

**C progress**

| Item | Implementation and verification status |
|---|---|
| Product update and delete | Implemented and passed isolated JDBC checks for insertion, editing, stale stock rejection, price preservation, deletion, and duplicate names. Swing checks also passed for Staff add/edit, Manager delete, and displaying a price of 999999.99 with cents preserved. |
| Client history | Implemented the SQL username filter using the authenticated session. JDBC checks passed for two clients and the separate all-user query. |
| Purchase | Implemented bounded quantity input, a stock-conditional debit and history insert in one transaction, and an audit call after commit. JDBC checks passed for invalid quantities, insufficient stock, rollback on receipt failure, and two concurrent buyers competing for the final item. |
| Validation, logging, and errors | Product and purchase forms call `Validator`, `main.log`, and `ErrorHandler`. Stock and quantity accept up to five digits. Price accepts up to 32 characters in decimal notation. Swing checks passed for rejected submissions, audit writes after commit, cancellation, and the generic error dialog using test implementations of the shared interfaces and disposable SQLite. |
| Role controls | Client shows Purchase and My purchase history. Staff shows Add, Edit, and Delete. Manager shows Add, Edit, Delete, and Purchase history. Headless and Swing tests with test implementations of the shared interfaces verified role controls, both history views, and denial after a session change while a purchase dialog was open. Login and routing in the integrated app remain pending. |
| Distribution | The existing JAR predates these changes and `dist/database.db` is absent. After integration, run NetBeans Clean and Build, include the executable JAR, `lib/sqlite-jdbc-3.23.1.jar`, and the submission `database.db`. Run `java -jar CSSECDV.jar` from a copy of the distribution folder on a machine without NetBeans and repeat the role smoke tests. |

The C checkboxes remain open under the project definition of done until testing in the integrated app. The current checkout and remote branch lack `Validator`, `AccessControl`, `ErrorHandler`, `Frame.currentUser` / `showPanel`, and `Main.log`. The isolated checks used a portable JDK and the repository's SQLite JDBC driver. NetBeans and Ant were unavailable in the local build environment.

**Security Documentation (Table 4), C rows**

| Requirement | Files | Vulnerability | Impact | Security control |
|---|---|---|---|---|
| 2.2.2 | `ClientHome`, `StaffHome`, `ManagerHome`, `RoleFeatures`, `SQLite` | Identical controls across roles and unscoped history | Unauthorized catalog changes or disclosure of other customers' purchases | Role-specific controls and session checks. Client history binds the signed-in username in SQL. |
| 2.3.1, 2.3.3 | `RoleFeatures` | Invalid or oversized product and quantity fields | Invalid catalog data or purchase quantities | Reject invalid raw text. Bound text first and apply shared name, stock, and price validation. |
| 2.3.4 | `SQLite` | Unbound input and independent purchase writes | SQL manipulation or inconsistent stock and purchase records | JDBC prepared statements and a transaction with a stock-conditional update and rollback. |
| 2.4.3 | `RoleFeatures`, `SQLite` | Unhandled form or database failures | Interrupted actions and exposure of implementation details | Catch SQL and runtime failures in event handlers and call the shared generic error helper. |
| 2.5.2 | `RoleFeatures` | Malformed submissions leave no audit evidence | Repeated validation failures are difficult to investigate | Use `main.log` with a fixed event description and the session username. Log successful purchases after commit. |

**Access Control Matrix (Table 5), C feature inputs for B**

| Feature | Unregistered | Disabled (1) | Client (2) | Staff (3) | Manager (4) | Admin (5) |
|---|---|---|---|---|---|---|
| View catalog | No | No | Yes | Yes | Yes | No |
| Add, edit, delete products | No | No | No | Yes | Yes | No |
| Purchase products | No | No | Yes | No | No | No |
| View purchase history | No | No | Own only | No | All users | No |

**Test Cases (Table 6), C rows**

| Group | Procedure | Expected result |
|---|---|---|
| Authorization | Open each role panel with its assigned role, a different role, a disabled account, and no session. Switch accounts between opens. | Controls match the matrix. Unauthorized panels and the previous client's history are cleared. |
| Authorization | Purchase with two different clients, then open each client's history and the manager's history. | Each client sees only its SQL-filtered rows. The manager sees both clients' rows and usernames. |
| Data Validation | Submit an invalid name or 51-character name, stock outside 0–10,000, and malformed, overlength, zero, or over-limit price. Then add and edit with valid inputs, including a price of 999999.99. | Invalid values are rejected without changes. Valid values are stored and displayed with the correct cents. |
| Data Validation | Submit zero, negative, fractional, oversized, and above-stock quantities, then buy the exact stock. | Invalid requests create no history and leave stock unchanged. The exact-stock purchase leaves zero stock and creates one history row. |
| Data Validation | From two sessions, buy the final item simultaneously. Keep a product edit dialog open while another session buys the item, then submit the edit. | Exactly one purchase succeeds. A stale edit cannot restore stock. |
| Error Handling | On a disposable database, make the history insert fail. Also try adding a duplicate product. | No partial purchase remains. The generic helper displays an error without SQL or a stack trace. |
| Logging | Submit an invalid product and invalid quantity, cancel a dialog, and complete a purchase. | Invalid submissions log validation failures. Cancellations create no failure log. The purchase log follows commit. |

**Integration review of A and B**

The existing password hash and verification helper, database-side lockout on the fifth failure, and last-attempt status passed isolated checks. Login still opens the main view without authenticating. Registration still passes the raw password to the database, and startup still drops the tables. Admin currently exposes Products and History, and role-based routing and a session are absent. These findings apply to the checked-out source.

---

## Deliverables

| Deliverable | Owner | Notes |
|---|---|---|
| Security Documentation (Table 4) | Each person fills rows for their own items; A assembles | Columns: Requirement, Files, Vulnerability, Impact, Security Control (name the exact algorithm, e.g. PBKDF2WithHmacSHA256, 120,000 iterations, 16-byte salt) |
| Access Control Matrix (Table 5) | B | Rows = every feature. Unregistered and Disabled must be "No" for everything except Login/Register. |
| Test Cases (Table 6) | Each person, for their own items | Grouped by Authentication / Authorization / Data Validation / Error Handling / Logging |
| Source code folder | All | Clean and build in NetBeans first |
| Distribution folder | C | Executable JAR + `lib/` (SQLite JDBC driver) + `database.db`; test on a machine without NetBeans |

**Known weakness to document honestly:** unknown usernames return faster than real ones (no hash is computed), which can reveal which usernames exist through timing. Either add a dummy `verify` call for unknown users or list it as an accepted risk.

---

## Integration Checklist

Merge order matters, because later branches depend on earlier ones.

1. [ ] **Day 1:** A pushes `Validator`; B pushes `AccessControl`, `ErrorHandler`, and the `currentUser` / `showPanel` / `main.log` stubs
2. [ ] A confirms login works end to end; B and C rebase on it
3. [ ] B and C build on the shared contracts
4. [ ] Everyone applies validation, logging, and error handling on their own forms
5. [ ] **Full test run** using the Test Cases table, with all four roles plus a disabled account
6. [ ] **Switch `Main.java` for submission (A):** wrap the table drops in `if (sqlite.DEBUG_MODE == 1)`, seed only when `getUsers().isEmpty()`, delete `database.db` once, and re-run the tests
7. [ ] C builds the distribution folder and runs it standalone
8. [ ] **GitHub:** separate feature branch per person, with commits from all three

### Definition of done (per item)
- Works in the running app, not just in code
- Has a row in the Security Documentation, the ACM (if it is a feature), and the Test Cases
- Every new DB method uses prepared statements
- No error message shows a stack trace or SQL text
