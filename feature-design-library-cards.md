# Library Card Features Design

## Overview

The library card features enable patrons to apply for library cards, view their digital library cards, and allow librarians to manage card applications. The system generates wallet-sized PDF library cards with customizable designs.

## Purpose

- **Public Card Applications**: Allow non-authenticated users to apply for library cards
- **Application Management**: Librarians review and approve/reject applications
- **Digital Library Cards**: Authenticated users can view and print their library cards
- **PDF Generation**: Create wallet-sized library card PDFs with customizable designs

## Components

### 1. My Library Card (`/my-card`)

**Access**: Authenticated users only

Displays the current user's library card information and allows them to download a wallet-sized PDF version.

**Features**:
- Display user's name and member ID
- "Print Card" button to download PDF
- PDF includes custom library branding (logo, colors)
- Wallet-sized format (2.125" x 3.375")

**Frontend**: `frontend/src/pages/library-cards/MyLibraryCardPage.tsx`

### 2. Apply for Card (`/apply`)

**Access**: Public (no authentication required)

Public-facing form where prospective patrons can submit library card applications.

**Features**:
- Username and password input
- Client-side SHA-256 password hashing before submission
- Form validation
- Success/error messaging
- Automatically sets authority to "USER"

**Frontend**: `frontend/src/pages/library-cards/ApplyForCardPage.tsx`

**Workflow**:
1. User fills out application form (username, password)
2. Password is hashed client-side using SHA-256
3. Application submitted to `/api/application/public/register`
4. Application status set to `PENDING`
5. User receives confirmation message
6. Librarian reviews application in Applications page

### 3. Applications (`/applications`)

**Access**: Librarian only

Management interface for reviewing and processing library card applications.

**Features**:
- List all pending applications
- Approve applications (creates user account)
- Delete applications (reject)
- Displays applicant name and status
- Confirmation dialogs for approve/delete actions

**Frontend**: `frontend/src/pages/library-cards/ApplicationsPage.tsx`

**Approve Workflow**:
1. Librarian clicks "Approve" on an application
2. System creates a new User account with:
   - Username from application
   - Password from application (already bcrypt-hashed)
   - Authority: `USER`
3. Application status changed to `APPROVED`
4. User can now log in with their credentials

### 4. Library Card Design (User Settings)

**Access**: All authenticated users

Each user can select their preferred library card design from 5 predefined options.

**Features**:
- Selection from 5 predefined card designs
- Design preference saved per-user
- Applied when user generates their library card PDF
- Accessible in User Settings page (`/settings`)

**Available Designs**:
1. **Classical Devotion** - Traditional design with classic typography (default)
2. **Countryside Youth** - Fresh, youthful design with natural elements
3. **Sacred Heart Portrait** - Portrait-oriented design with sacred imagery
4. **Radiant Blessing** - Bright design with uplifting elements
5. **Patron of Creatures** - Nature-focused design with animal motifs

**Location**: Part of `/settings` page (User Settings)

**Implementation**: Design preference stored on `User` entity as `libraryCardDesign` field (enum type)

## Domain Model

### Applied Entity

**File**: `src/main/java/com/muczynski/library/domain/Applied.java`

```java
@Entity
public class Applied {
    private Long id;                    // Auto-generated primary key
    private String name;                // Applicant's username
    private String password;            // Bcrypt-hashed password
    private ApplicationStatus status;   // PENDING, APPROVED, REJECTED
}
```

**Fields**:
- `id`: Unique identifier (auto-generated)
- `name`: Requested username for the library account
- `password`: Password hashed with bcrypt (after client-side SHA-256)
- `status`: Enum tracking application state

**ApplicationStatus Enum**:
- `PENDING`: Default status, awaiting librarian review
- `APPROVED`: Application approved, user account created
- `REJECTED`: Application rejected (currently unused)

**Note**: Password is double-hashed:
1. Client-side: SHA-256 (prevents plain text over network)
2. Server-side: Bcrypt (secure storage)

### AppliedDto

**File**: `src/main/java/com/muczynski/library/dto/AppliedDto.java`

```java
public class AppliedDto {
    private Long id;
    private String name;
    private ApplicationStatus status;
    // password NOT included for security
}
```

**Security**: Password field intentionally excluded from DTO to prevent exposure in API responses.

### LibraryCardDesign Enum

**File**: `src/main/java/com/muczynski/library/domain/LibraryCardDesign.java`

Enum defining the 5 predefined library card design options.

**Values**:
- `CLASSICAL_DEVOTION` - Default design with traditional styling
- `COUNTRYSIDE_YOUTH` - Youthful design with natural elements
- `SACRED_HEART_PORTRAIT` - Portrait-oriented sacred design
- `RADIANT_BLESSING` - Bright, uplifting design
- `PATRON_OF_CREATURES` - Nature-themed design with animals

**Usage**: Stored on `User` entity as `libraryCardDesign` field. Each user can select their preferred design, which is applied when generating their library card PDF.

**Frontend Mapping**: TypeScript enum defined in `frontend/src/types/dtos.ts` with matching values for type safety.

## API Endpoints

### Public Endpoints (No Authentication)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/application/public/register` | Submit library card application |

**Request Body** (RegistrationRequest):
```json
{
  "username": "john_doe",
  "password": "sha256_hashed_password",
  "authority": "USER"
}
```

**Response**: `204 No Content` on success

### Authenticated Endpoints

| Method | Endpoint | Description | Authorization |
|--------|----------|-------------|---------------|
| GET | `/api/library-card/print` | Generate library card PDF for current user | `isAuthenticated()` |

**Response**: PDF file (application/pdf) with download headers

### Librarian-Only Endpoints

| Method | Endpoint | Description | Authorization |
|--------|----------|-------------|---------------|
| GET | `/api/applied` | Get all card applications | `hasAuthority('LIBRARIAN')` |
| POST | `/api/applied` | Create application (manual) | `hasAuthority('LIBRARIAN')` |
| PUT | `/api/applied/{id}` | Update application status | `hasAuthority('LIBRARIAN')` |
| DELETE | `/api/applied/{id}` | Delete application | `hasAuthority('LIBRARIAN')` |
| POST | `/api/applied/{id}/approve` | Approve application, create user account | `hasAuthority('LIBRARIAN')` |

## Backend Architecture

### Controllers

#### AppliedController

**File**: `src/main/java/com/muczynski/library/controller/AppliedController.java`

**Responsibilities**:
- Handle card application submissions
- Manage application CRUD operations
- Approve applications and trigger user account creation

**Key Methods**:
- `register()`: Public endpoint for card applications
- `getAllApplied()`: Retrieve all applications (returns DTOs)
- `approveApplication()`: Approve application and create user

**Security**:
- Uses `@PreAuthorize("hasAuthority('LIBRARIAN')")` for admin endpoints
- Public `/application/public/register` endpoint has no auth requirement
- All entity-to-DTO conversion uses `AppliedMapper`

#### LibraryCardController

**File**: `src/main/java/com/muczynski/library/controller/LibraryCardController.java`

**Responsibilities**:
- Generate library card PDFs for authenticated users

**Key Methods**:
- `printLibraryCard()`: Generates wallet-sized PDF for current user

**Security**:
- Uses `@PreAuthorize("isAuthenticated()")` for PDF generation
- Retrieves current user from Spring Security context

### Services

#### AppliedService

**File**: `src/main/java/com/muczynski/library/service/AppliedService.java`

**Responsibilities**:
- Business logic for application management
- Password validation (SHA-256 format check)
- Trigger user creation on approval

**Key Methods**:
- `createApplied()`: Validates and saves application
  - Checks password is SHA-256 hash
  - Hashes password with bcrypt
- `approveApplication()`: Approves application and calls UserService to create account

**Password Handling**:
```java
// 1. Validate client sent SHA-256 hash (64 hex chars)
if (!PasswordHashingUtil.isValidSHA256Hash(applied.getPassword())) {
    throw new IllegalArgumentException("Invalid password format");
}
// 2. Hash with bcrypt for storage
applied.setPassword(passwordEncoder.encode(applied.getPassword()));
```

#### LibraryCardPdfService

**File**: `src/main/java/com/muczynski/library/service/LibraryCardPdfService.java`

**Responsibilities**:
- Generate wallet-sized library card PDFs using iText 8
- Apply library branding and design settings
- Format cards to standard wallet size (2.125" x 3.375")

**Technology**: iText 8 PDF library

**Key Methods**:
- `generateLibraryCardPdf(User user)`: Creates PDF byte array for user

**Card Contents**:
- Library logo (if configured)
- Library name
- Patron name
- Member ID (user ID)
- Barcode (optional)
- Background colors from LibraryCardDesign

### Mappers

#### AppliedMapper

**File**: `src/main/java/com/muczynski/library/mapper/AppliedMapper.java`

**Technology**: MapStruct

```java
@Mapper(componentModel = "spring")
public interface AppliedMapper {
    AppliedDto appliedToAppliedDto(Applied applied);

    @Mapping(target = "password", ignore = true)
    Applied appliedDtoToApplied(AppliedDto appliedDto);
}
```

**Security**: Password field ignored when mapping from DTO to entity (prevents password overwriting).

### Repositories

#### AppliedRepository

**File**: `src/main/java/com/muczynski/library/repository/AppliedRepository.java`

```java
public interface AppliedRepository extends JpaRepository<Applied, Long> {
    // Standard CRUD operations via JpaRepository
}
```

## Frontend Architecture

### Technology Stack

- **React 18** with TypeScript
- **TanStack Query v5** for API state management
- **Tailwind CSS** for styling
- **React Router v6** for navigation

### API Integration

**File**: `frontend/src/api/library-cards.ts`

**Hooks**:
- `useApplications()`: Fetch all applications (librarian)
- `useApplyForCard()`: Submit card application (public)
- `useApproveApplication()`: Approve application (librarian)
- `useDeleteApplication()`: Delete application (librarian)
- `printLibraryCard()`: Download PDF (authenticated user)

**Example Usage**:
```typescript
const { mutate: applyForCard } = useApplyForCard()

const handleSubmit = async () => {
  const hashedPassword = await hashPassword(password)
  applyForCard({
    username,
    password: hashedPassword,
    authority: 'USER'
  })
}
```

### Pages

#### MyLibraryCardPage

**File**: `frontend/src/pages/library-cards/MyLibraryCardPage.tsx`

**UI Elements**:
- User greeting with name
- Member ID display
- "Print Library Card" button
- Loading state during PDF generation
- Error handling

**PDF Download**:
```typescript
const handlePrint = async () => {
  const blob = await printLibraryCard()
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'library-card.pdf'
  a.click()
}
```

#### ApplyForCardPage

**File**: `frontend/src/pages/library-cards/ApplyForCardPage.tsx`

**UI Elements**:
- Username input (data-test="username-input")
- Password input (data-test="password-input")
- "Apply" button (data-test="apply-button")
- Success message display
- Error message display

**Validation**:
- Username required (non-empty)
- Password required (non-empty)
- Client-side SHA-256 hashing via `hashPassword()` utility

#### ApplicationsPage

**File**: `frontend/src/pages/library-cards/ApplicationsPage.tsx`

**UI Elements**:
- Applications table with columns: Name, Status, Actions
- "Approve" button per application (data-test="approve-button-{id}")
- "Delete" button per application (data-test="delete-button-{id}")
- Confirmation dialogs for approve/delete
- Empty state: "No pending applications"

**TanStack Query Integration**:
- `useApplications()` fetches data with 2-minute cache
- Mutations invalidate `['applications']` query key
- Optimistic updates for better UX

## Testing

### Backend Tests

#### AppliedControllerTest

**File**: `src/test/java/com/muczynski/library/controller/AppliedControllerTest.java`

**Test Coverage**:
- ✅ Get all applications (librarian only)
- ✅ Create application
- ✅ Update application status
- ✅ Delete application
- ✅ Approve application (creates user account)
- ✅ Public registration endpoint (no auth)
- ✅ Error handling (404, validation errors)

**Technology**: `@SpringBootTest`, `@AutoConfigureMockMvc`, `MockMvc`

#### LibraryCardControllerTest

**File**: `src/test/java/com/muczynski/library/controller/LibraryCardControllerTest.java`

**Test Coverage**:
- ✅ Generate PDF for authenticated user
- ✅ All 5 card design variations
- ✅ Error handling (user not found, not authenticated)
- ✅ PDF format validation (byte array, headers)

### UI Tests

#### ApplyForCardUITest

**File**: `src/test/java/com/muczynski/library/ui/ApplyForCardUITest.java`

**Technology**: Playwright

**Test Coverage**:
- ✅ Form renders correctly
- ✅ Validation on empty fields
- ✅ Successful application submission
- ✅ Error handling
- ✅ Password hashing verification

**Missing UI Tests** (identified in code review):
- ❌ My Library Card page UI tests
- ❌ Applications page UI tests

## Security Considerations

### Password Security

**Two-Layer Hashing**:
1. **Client-side SHA-256**: Prevents plain text password transmission over HTTPS
2. **Server-side Bcrypt**: Secure storage in database

**Validation**: `AppliedService` validates password is SHA-256 format (64 hex characters) before accepting.

### Authentication & Authorization

**Public Access**:
- `/api/application/public/register`: Intentionally public for card applications

**Authenticated Access**:
- `/api/library-card/print`: Requires any authenticated user

**Librarian-Only Access**:
- `/api/applied/**`: All application management endpoints
- Applications page: Frontend route protected with `<LibrarianRoute>`

### Data Protection

**DTOs Exclude Sensitive Data**:
- `AppliedDto` does NOT include password field
- All controller endpoints return DTOs, never entities
- MapStruct mapper ignores password when mapping from DTO

## Workflow: Complete Application Process

### 1. User Applies for Card

```
User → /apply page
  → Fills form (username, password)
  → Frontend hashes password (SHA-256)
  → POST /api/application/public/register
  → AppliedService validates & saves
  → Applied entity created (status: PENDING)
  → User sees success message
```

### 2. Librarian Reviews Application

```
Librarian → /applications page
  → GET /api/applied
  → Sees list of pending applications
  → Clicks "Approve" on application
  → Confirmation dialog
```

### 3. Application Approved

```
POST /api/applied/{id}/approve
  → AppliedService.approveApplication()
  → UserService.createUserFromApplied()
    → Creates User with username, password, authority=USER
  → Applied.status = APPROVED
  → Frontend cache invalidated
  → Application removed from pending list
```

### 4. User Logs In and Gets Card

```
User → /login
  → Enters username & password
  → Password hashed (SHA-256)
  → Spring Security validates
  → User authenticated

User → /my-card
  → GET /api/library-card/print
  → LibraryCardPdfService generates PDF
  → User downloads wallet-sized PDF
```

## PDF Generation Details

### iText 8 Library

**Technology**: iText 8 (com.itextpdf)

**Card Dimensions**:
- Width: 2.125 inches (54mm)
- Height: 3.375 inches (85.6mm)
- Standard credit card size

**Card Elements**:
- Header: Library logo + name
- Body: Patron name, Member ID
- Footer: Barcode (optional)
- Background: Custom colors from LibraryCardDesign

**Customization**: All design elements pulled from `LibraryCardDesign` entity (or defaults if not configured).

## Integration with User Management

### User Creation from Application

When an application is approved:

1. `AppliedService.approveApplication(id)` called
2. Retrieves `Applied` entity by ID
3. Calls `UserService.createUserFromApplied(applied)`
4. UserService creates User entity:
   - `username = applied.name`
   - `password = applied.password` (already bcrypt-hashed)
   - `authority = "USER"`
5. User saved to database
6. Application status updated to `APPROVED`

**Note**: Password is NOT re-hashed; it's already bcrypt-hashed when the application was created.

## Future Enhancements

### Potential Features
- Barcode generation on library cards (QR code or Code 39)
- Email notifications on application approval
- Application rejection workflow (currently approve/delete only)
- Expiration dates on library cards
- Card renewal process
- Photo upload for library cards
- Digital wallet integration (Apple Wallet, Google Pay)

## Related Documentation

- `CLAUDE.md`: Main project overview
- `backend-requirements.md`: Backend development patterns
- `feature-design-security.md`: Authentication and authorization details
- `endpoints.md`: Complete API endpoint reference
- `uitest-requirements.md`: UI testing guidelines with Playwright
