# Data Integrity Protection

## Problem

During JSON import, duplicate entities could cause `NonUniqueResultException: Query did not return a unique result` errors when Optional-returning repository methods encountered multiple matching rows.

## Solution Layers

### 1. Database Unique Constraints

Every entity with a natural key has a unique constraint:

| Entity | Constraint | Columns |
|--------|-----------|---------|
| Library | uk_library_branch_name | name |
| Book | uk_book_title | title |
| Loan | uk_loan_book_user_date | book_id, user_id, loan_date |
| Authority | uk_authority_name | name |
| Applied | uk_applied_name | name |
| User | uk_user_username | username |
| PhotoUploadSession | uk_upload_session_upload_id | uploadId |
| Author | uk_author_name | name (pre-existing) |

Any constraint violation caught at the DB level returns **409 CONFLICT** via `DataIntegrityViolationException` handler in `GlobalExceptionHandler`.

### 2. Service-Layer Duplicate Checks

Before `save()`, services check for existing entities:

- `LoanService.checkoutBook()` / `checkoutBookWithPhoto()` — checks for existing loan with same book, user, and date
- `AppliedService.createApplied()` — checks for existing application by name

### 3. findOrCreate Methods

Reusable methods that query first, create only if not found:

- `AuthorService.findOrCreateAuthor(String name)`
- `BranchService.findOrCreateBranch(String branchName, String librarySystemName)`
- `BookService.findOrCreateBook(String title, String authorName, Library library)`
- `LoanService.findOrCreateLoan(Long bookId, Long userId, LocalDate loanDate)`
- `UserService.findOrCreateAuthority(String name)`

### 4. List-Based Repository Lookups

All natural-key lookups use `findAllBy*OrderByIdAsc()` returning `List<T>` instead of `Optional<T>`. When duplicates exist, the entity with the lowest ID (oldest) is used.

The Optional-returning methods are marked `@Deprecated` with guidance to use list alternatives.

### 5. DuplicateEntityException

`DuplicateEntityException` extends `LibraryException` with enriched fields:

- `entityType` — e.g., "Book", "Author"
- `entityName` — e.g., "The Adventures of Tom Sawyer"
- `existingEntityId` — ID of the existing entity

Returns **409 CONFLICT** with all fields in the JSON response.

### 6. Test Infrastructure

- `TestEntityHelper` provides `findOrCreateAuthority()`, `findOrCreateAuthor()`, `findOrCreateLibrary()` for test setup
- SQL test data files use `ON CONFLICT (name) DO NOTHING` for idempotent inserts
- Non-transactional tests clean up all entities in `@BeforeEach`
