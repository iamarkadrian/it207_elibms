# Rubric map

Where to find the evidence for each grading criterion. Paths are relative to `src/main/java/edu/bulsu/lms/` unless noted.

## 1. Foundational Java and program structure (10%)

| Requirement | Evidence |
|---|---|
| Data types, control structures, methods | everywhere; for example `service/AuthService.login` (branches, early returns), `persistence/ItemRepository.map` (`switch`) |
| Constructors **including an overloaded constructor** | `domain/Student` (full constructor + one for a brand-new member), `domain/Staff` (loaded account + new account), `domain/PrintedBook` (with and without `available`) |
| Proper access modifiers | fields `private`; `protected` constructors in abstract classes (`Person`, `LibraryItem`); `final` classes for leaves |
| **Static members** | `domain/Check` (static validators), `Person.requireName`, `Person.formatName`, `LibraryItem.requireCode`, constants such as `Staff.MAX_FAILED_ATTEMPTS`, `PasswordHasher.DEFAULT_ITERATIONS` |

## 2. Encapsulation (10%)

* Every field in the domain is `private`; state changes only through methods.
* **Every setter validates** (`domain/Person`, `Student`, `Staff`, `LibraryItem`, `PrintedBook`, `DigitalBook`, `OtherMaterial`) and **constructors call the setters**, so an invalid object cannot be built.
* Getters return immutable values (`String`, `int`, records) or copies (`ObjectTable.items()` returns `List.copyOf`).
* Behaviour is part of the object: `Staff.recordFailedLogin()` decides when an account locks; `LibraryItem.markLent()` decides whether a loan is allowed.
* Proof: `src/test/java/.../domain/DomainModelTest`.

## 3. Inheritance and class hierarchy (15%)

* **`LibraryItem`** (abstract) ► `PrintedBook`, `DigitalBook`, `OtherMaterial`: each calls `super(...)` and overrides `type()`, `isBorrowable()`, `consumesCopyOnLoan()`, `tableRow()`, `lines()`, `borrowText()` to behave **differently** (see the table in [ARCHITECTURE.md](ARCHITECTURE.md#2-inheritance-for-things-that-really-differ)).
* **`Person`** (abstract) ► `Student`, `Staff`: shared name validation via `super(first, middle, last)`; different `roleLabel()`, `identifier()`, `subtitle()`, `lines()`.
* Both hierarchies are `sealed`.

## 4. Polymorphism (10%)

* `ui/librarian/CatalogScreen` holds an `ObjectTable<LibraryItem>`; one piece of code renders books, e-books and other materials. **On screen the kinds differ**: different table columns (`ItemType.columns()` ⇄ `tableRow()`), different detail lines, and the detail button reads **BORROW** for lendable items but **DONE** for reference material (`CatalogScreen.customizeDetail`).
* `persistence/ItemRepository.map` builds the right subclass from the `item_type` column; `insert` uses an exhaustive pattern-matching `switch`.
* `DetailCard.show(Displayable)` renders a member, a book or a loan.
* Proof: `DomainModelTest.polymorphicProcessing`, `LibrarianFlowIT.theDetailCardAdaptsToWhatWasClicked`.

## 5. Abstraction and interfaces (10%)

* `LibraryItem` and `Person` are abstract; `LibraryItem` declares abstract methods (`type`, `isBorrowable`, `consumesCopyOnLoan`, `tableRow`, `borrowText`), `Person` declares `roleLabel` and `identifier`.
* **Interface `domain/Displayable`** is implemented by *unrelated* classes: `LibraryItem`, `Person` (through its subclasses) and the record `Loan`.
* Also: `ui/kit/Messenger` (implemented by `SwingMessenger` and, in tests, `RecordingMessenger`), `service/LoginResult` (sealed interface with four records).

## 6. GUI design and components (15%)

* Layout managers used (six): `BorderLayout`, `CardLayout`, `GridBagLayout`, `GridLayout`, `BoxLayout`, `FlowLayout`; see `ui/librarian/LibrarianView`, `MasterDetailScreen`, `DashboardScreen`, `ui/kit/FormPanel`, `ui/admin/AdminView`.
* Components: `JTable` in `JScrollPane`, `JComboBox` (including a dependent college → course pair), `JTextField`, `JPasswordField`, `JButton`, `JLabel`, `JPanel`, `JOptionPane`, `javax.swing.Timer` (clock), icons and images.
* Consistent look: one `Theme`, two `Palette`s (light and dark), reusable `Field`/`FormPanel`/`DetailCard`/`StatCard`.
* Screenshots: [README](../README.md#screenshots).

## 7. Event handling and dialogs (10%)

* Every control has a real listener: `ActionListener` on buttons and on <kbd>Enter</kbd> in text boxes (`FormPanel`), `ListSelectionListener` on tables (`ObjectTable`), `WindowListener` (`MainWindow` asks before closing), combo-box `ActionListener`s (`MembersScreen`: course follows college), a `Timer` for the clock.
* `JOptionPane` (all in `ui/kit/SwingMessenger`): **message** dialogs for validation and results, **confirm** dialogs (cancel with typed data, remove member, return book, exit), and an **input** dialog (type the librarian's username to confirm a removal, `AdminView.remove`).
* Proof: `LibrarianFlowIT`, `AdminFlowIT`, `LoginFlowIT` click the real buttons.

## 8. Data persistence and UML consistency (20%)

* **Persistence**: all data is in MySQL/SQL Server; close and reopen the app and everything is still there. Try it: add a member, quit, start again, the member is in the table. Tests: `MemberCatalogIT`, `CirculationIT`.
* **UML matches the code exactly**: [`docs/uml/class-diagram.md`](uml/class-diagram.md) is **generated from the compiled classes** by `UmlDiagramGenerator`, and `UmlDiagramTest` fails the build if the file is stale. It cannot drift.

## 9. Bonus: relational database via MySQL (+5%)

* JDBC with **`PreparedStatement`** everywhere (`persistence/*Repository`).
* **Create**: `StudentRepository.insert`, `ItemRepository.insert`, `StaffRepository.insert`, `LoanRepository.insert`
  **Read**: every `find*` / `list*` / `count*`
  **Update**: `ItemRepository.setAvailable/tryClaim`, `LoanRepository.markReturned`, `StaffRepository.updateAccount/updateLoginState`
  **Delete**: `StudentRepository.delete`, `StaffRepository.delete`
* Connections are opened and **always closed** by `Database.query/transaction` using try-with-resources; multi-step changes are transactions with rollback.
* MySQL scripts: [`db/mysql`](../db/mysql). The same code also runs on SQL Server: [`db/mssql`](../db/mssql).
