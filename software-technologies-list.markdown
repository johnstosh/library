# Software Technologies List

This list outlines the software technologies and architectural elements used in the Library Management System project. It serves as a reference for current technology stack and development practices, focusing on robust backend and frontend integration, comprehensive testing, and cloud deployment readiness.

## Core Technologies

### Backend
- **Java JDK 17** - Programming language and runtime
- **Spring Boot 3.5.+** - Application framework
- **Spring Data JPA** - Database access and ORM
- **Spring Security** - Authentication and authorization
- **PostgreSQL** - Production database
- **H2 Database** - In-memory database for testing
- **Gradle 8.14** - Build automation tool
- **Lombok** - Code generation for boilerplate reduction
- **MapStruct 1.5.5** - Type-safe bean mapping

### Frontend
- **Vanilla JavaScript** - Frontend logic
- **HTML5/CSS3** - UI markup and styling
- **Static Resource Serving** - Spring Boot serves from `src/main/resources/static/`

### External Integrations
- **Google OAuth 2.0** - Google Photos API integration
- **Google Photos API** - Photo management and synchronization
- **Google Cloud SQL** - Cloud database connectivity (Postgres Socket Factory 1.13.+)

## Architecture & Patterns

### Data Layer
- **Entities** - JPA/Hibernate entities for database mapping (User, Book, Author, Library, Loan, Applied, Photo)
- **DTOs (Data Transfer Objects)** - Structured data exchange between layers (UserDto, BookDto, AuthorDto, etc.)
- **Mappers** - MapStruct-based bidirectional mapping between entities and DTOs
- **Repository Pattern** - Spring Data JPA repositories with custom queries and fetch joins

### Service Layer
- **Service Classes** - Business logic encapsulation (UserService, BookService, GooglePhotosService, etc.)
- **Transactional Management** - `@Transactional` annotations for data consistency
- **OAuth Token Management** - Automatic token refresh and validation

### Controller Layer
- **REST Controllers** - RESTful API endpoints with proper HTTP methods
- **Security Annotations** - `@PreAuthorize` for role-based access control
- **Exception Handling** - Centralized error handling with appropriate HTTP status codes

### Validation & Error Handling
- **Request Validation** - Spring validation annotations and custom validators
- **Global Exception Handler** - Consistent error response formatting
- **HTTP Status Codes** - Proper use of 200, 201, 400, 401, 403, 404, 500

## Testing Strategy

### Unit Tests
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework with `@Mock` and `@InjectMocks`
- **Test Profiles** - `@ActiveProfiles("test")` for isolated test environments

### API Integration Tests
- **RestAssured 5.5.0** - Fluent API for REST endpoint testing
- **Spring MockMvc Integration** - `@SpringBootTest` with `@AutoConfigureMockMvc`
- **MockitoBean** - Service layer mocking in integration tests
- **Security Testing** - `@WithMockUser` for authentication/authorization tests

### UI Tests
- **Playwright 1.55.+** - Browser automation and end-to-end testing
- **Headless Chrome** - Fast UI test execution
- **Data-driven Testing** - SQL scripts for test data setup
- **Screenshot on Failure** - Automatic debugging artifacts

### Test Data Management
- **H2 In-Memory Database** - Fast test database
- **SQL Scripts** - `data-*.sql` files in `src/test/resources/`
- **Test Isolation** - Each test uses fresh database state

## Security

### Authentication & Authorization
- **Spring Security** - Security framework
- **Role-Based Access Control** - USER and LIBRARIAN roles
- **Session Management** - HTTP session-based authentication
- **Password Encoding** - BCrypt password hashing

### OAuth 2.0 Integration
- **Authorization Code Flow** - Standard OAuth 2.0 flow
- **Token Storage** - Secure token persistence in database
- **Refresh Token Management** - Automatic access token renewal
- **CSRF Protection** - State parameter validation

## Development Practices

### Code Quality
- **Lombok** - Reduces boilerplate with `@Data`, `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`
- **MapStruct** - Compile-time type-safe mapping (no reflection)
- **SLF4J Logging** - Structured logging throughout application

### Testing Requirements
- **Backend Requirements** - As per `backend-development-requirements.md`:
  - Unit tests for all service methods
  - API integration tests for all endpoints
  - Each endpoint must have: success test (2xx), unauthorized test (401), validation test (400)
- **UI Test Requirements** - As per `uitest-requirements.md`:
  - Maximum timeout: 20 seconds
  - NETWORKIDLE waits after CRUD operations
  - Screenshot on failure
  - Data-driven with SQL scripts

### Documentation
- **Javadoc** - Method and class documentation
- **README** - Project overview and setup instructions
- **Requirements Documents** - `backend-development-requirements.md`, `uitest-requirements.md`
- **Lessons Learned** - `backend-lessons-learned.md` for best practices

## Cloud Deployment
- **Google Cloud Runner** - Container-based application hosting
- **Google Cloud SQL for PostgreSQL** - cloud database

### Configuration
- **Environment Variables** - Externalized configuration
- **Application Profiles** - Development, test, and production profiles
- **Database Migration** - Controlled schema updates

