# Backend Requirements

## Logging Exceptions
Whenever the code catches an exception, it must log the exception with a helpful error message and exception traceback at the appropriate level. Use SLF4J Logger with the following guidelines:

- **ERROR level** for unexpected system failures (IO errors, database errors, initialization failures):
  `logger.error("Failed to initialize storage", e);`

- **WARN level** for expected operational failures (not found, validation errors, business rule violations):
  `logger.warn("Failed to retrieve book ID {}: {}", id, e.getMessage(), e);`

- **DEBUG level** for detailed diagnostics (parsing attempts, detailed flow):
  `logger.debug("Attempting fallback parsing method", e);`

Do not log sensitive data like passwords or tokens. This applies to all try-catch blocks in controllers, services, and handlers. User-facing responses should still return sanitized error messages without stack traces.

## Other Requirements
- Follow Spring Boot best practices for REST APIs.
- Use DTOs for all request/response bodies to avoid exposing entities.
- Secure endpoints with @PreAuthorize for role-based access.
- Handle transactions with @Transactional where needed.
- Validate inputs and throw meaningful RuntimeExceptions or IllegalArgumentExceptions.
