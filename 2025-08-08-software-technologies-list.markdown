This list outlines the software technologies and architectural elements to guide the development of design documents for future Java-based projects using Spring Boot. It combines technologies and patterns from previous projects (e.g., Scrabble, word-unscramble-game, kana flashcards) with new requirements, focusing on robust backend and frontend integration, testing, and cloud deployment. These components will serve as a foundation for creating maintainable and testable applications.

- Java JDK 17
- Spring Boot 3.5.x
- gradle 8.14.x
- JUnit
- Lombok
- PostgreSQL
- AWS Cloud PostgreSQL
- AWS App Runner
- Static Frontend Serving (Spring Boot: JavaScript, HTML, CSS from src/main/resources/static/js/)
- Separate Test Location (JavaScript tests in src/test/resources/js-tests/)
- QUnit (JavaScript testing in src/test/resources/qunit.js)
- HtmlUnit (JUnit integration for QUnit tests in Gradle)
- No File Copying (ClassPathResource for production and test files)
- DTOs (Data Transfer Objects for structured data exchange between frontend and backend, e.g., CartoonRequestDTO, FlashCardRequestDTO)
- Entities (JPA/Hibernate entities for database mapping, e.g., Kana, Cartoon, FlashCard, Word, Hint)
- Translator Classes (Service-layer classes to map entities to DTOs and vice versa, e.g., FlashCard to FlashCardDTO)
- Validators (Spring components for validating frontend data into endpoints, throwing custom exceptions like KanaException with HTTP 422 status)
- Global Exception Handler (Handles custom exceptions like KanaException for consistent error responses, e.g., converting to HTTP 422)
- Repository Pattern (Spring Data JPA repositories for database operations, e.g., KanaRepository, WordRepository, with fetch joins to avoid lazy loading issues)
- Service Layer (Business logic encapsulation, e.g., GameService for Scrabble, handling word unscrambling and hint generation)
- Controller Layer (REST controllers for handling HTTP requests, e.g., KanaController, with @Transactional for lazy loading)

  