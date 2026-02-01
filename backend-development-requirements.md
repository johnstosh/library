# Backend Development Requirements

## Testing Requirements

All backend features and services **MUST** include the following tests:

### 1. Unit Tests

**Required**: Yes

**Purpose**: Test individual methods and components in isolation

**Scope**:
- Test business logic in services
- Test data transformations in mappers
- Test validation logic
- Test utility functions
- Mock external dependencies (databases, APIs, etc.)

**Location**: `src/test/java/com/muczynski/library/`

**Naming Convention**: `{ClassName}Test.java`

**Example**:
```java
@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserSettingsService userSettingsService;

    @Test
    void testGooglePhotosApiKeyPersistence() {
        // Test implementation
    }
}
```

**Minimum Coverage**: Each service method should have at least one unit test covering the happy path and common error cases.

---

### 2. API Integration Tests

**Required**: Yes

**Purpose**: Test REST endpoints with actual HTTP requests and database interactions

**Scope**:
- Test controller endpoints end-to-end
- Test request/response serialization
- Test authentication and authorization
- Test error responses and status codes
- Use actual database (embedded PostgreSQL via Testcontainers for tests)

**Location**: `src/test/java/com/muczynski/library/controller/`

**Naming Convention**: `{ControllerName}Test.java`

**Example**:
```java
@SpringBootTest
@AutoConfigureMockMvc
class BooksFromFeedControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "librarian", authorities = {"LIBRARIAN"})
    void testProcessEndpoint() throws Exception {
        mockMvc.perform(post("/api/books-from-feed/process"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").exists());
    }
}
```

**Minimum Coverage**: Each controller endpoint should have at least:
- One test for successful request (2xx status)
- One test for unauthorized access (401/403 status)
- One test for invalid input (400 status)

---

## When to Write Tests

**Before Implementation** (Test-Driven Development - Recommended):
1. Write failing tests that define expected behavior
2. Implement code to make tests pass
3. Refactor while keeping tests green

**After Implementation** (Minimum Requirement):
1. Implement feature/service
2. Write unit tests for all service methods
3. Write API integration tests for all endpoints
4. Ensure all tests pass before committing

---

## Test Data Management

### SQL Test Data Files

**Location**: `src/test/resources/data-*.sql`

**Requirement**: When adding new database columns or tables, **ALL** test data SQL files must be updated.

**Files to Update**:
- `data1.sql`
- `data2.sql`
- `data3.sql`
- Any other `data-*.sql` files

**Example**: When adding OAuth token fields to User entity, all INSERT statements in test SQL files must include the new columns:

```sql
INSERT INTO users (id, username, password, xai_api_key, google_photos_api_key, google_photos_refresh_token, google_photos_token_expiry, last_photo_timestamp)
VALUES (1, 'librarian', '$2a$10$...', '', '', '', '', '');
```

---

## Running Tests

**Run All Tests**:
```bash
./gradlew test
```

**Run Specific Test Class**:
```bash
./gradlew test --tests UserSettingsServiceTest
```

**Run Tests with Coverage**:
```bash
./gradlew test jacocoTestReport
```

**Run Integration Tests Only**:
```bash
./gradlew test --tests "*ControllerTest"
```

**Run Unit Tests Only**:
```bash
./gradlew test --tests "*ServiceTest"
```

---

## Test Quality Guidelines

1. **Independence**: Tests should not depend on each other or run in a specific order
2. **Repeatability**: Tests should produce the same results every time
3. **Clarity**: Test names should clearly describe what is being tested
4. **Coverage**: Test both success and failure paths
5. **Speed**: Unit tests should run fast (< 1 second each)
6. **Assertions**: Each test should have clear assertions about expected behavior

---

## Continuous Integration

All tests **MUST** pass before:
- Creating a pull request
- Merging to main branch
- Deploying to any environment

**CI Pipeline** (if configured):
1. Run all unit tests
2. Run all integration tests
3. Check code coverage
4. Build application
5. Run UI tests (if applicable)

---

## Additional Resources

- **JUnit 5**: https://junit.org/junit5/docs/current/user-guide/
- **Mockito**: https://site.mockito.org/
- **Spring Boot Testing**: https://spring.io/guides/gs/testing-web/
- **MockMvc**: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/servlet/MockMvc.html

---

## Exceptions

In rare cases where tests cannot be written (e.g., testing third-party integrations without mocks), document the reason in the code and create a tracking issue.

**No exceptions for**:
- Service layer business logic
- REST controller endpoints
- Data mappers and transformations
