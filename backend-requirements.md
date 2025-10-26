# Backend Requirements

## Logging Exceptions
Whenever the code catches an exception, it must log the exception with a helpful error message and exception traceback, at debug level. Use SLF4J Logger with `logger.debug("Contextual error message describing what failed", e);` to include the full stack trace. Do not log sensitive data like passwords. This applies to all try-catch blocks in controllers, services, and handlers. User-facing responses should still return sanitized error messages without stack traces.

## Other Requirements
- Follow Spring Boot best practices for REST APIs.
- Use DTOs for all request/response bodies to avoid exposing entities.
- Secure endpoints with @PreAuthorize for role-based access.
- Handle transactions with @Transactional where needed.
- Validate inputs and throw meaningful RuntimeExceptions or IllegalArgumentExceptions.
