# Story 007: Control REST API errors with ControllerAdvice

## User Story

As a developer using the cache API, I want to receive consistent and meaningful error responses so that
I can handle failures appropriately in my application and understand what went wrong.

## Description

This story implements global exception handling for the REST API using Spring's @ControllerAdvice.
It will catch all exceptions thrown by the cache repository and controllers, transform them into appropriate
HTTP responses with proper status codes and error messages, ensuring a consistent error handling experience
across all API endpoints.

## Acceptance Criteria

- [ ] All cache repository exceptions are caught and handled appropriately
- [ ] HTTP status codes match the type of error (400, 404, 500, etc.)
- [ ] Error responses have consistent JSON structure
- [ ] Input validation errors return 400 Bad Request with clear messages
- [ ] Resource not found scenarios return 404 Not Found
- [ ] Internal errors return 500 Internal Server Error without exposing sensitive details
- [ ] Error responses should include meaningful error messages
- [ ] Exception handling is testable with unit and integration tests

## Technical Considerations

- Use @ControllerAdvice for global exception handling
- Map custom exceptions to appropriate HTTP status codes:
    - InvalidCacheRangeException → 400 Bad Request
    - InvalidIncrementValueException → 400 Bad Request
    - CacheException → 500 Internal Server Error
    - Exception → 500 Internal Server Error (cath-all)
- Don't expose internal stack traces in production responses

## Definition of Done

- [ ] Code implemented
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing
- [ ] ArchUnit tests passing
- [ ] Code reviewed
- [ ] Error handling manually tested with invalid requests
- [ ] Documentation updated

## Dependencies

- [Expose cache repository over REST Api service](006-expose-cache-repository-over-rest-api-service.md)Story 006 (Expose
  Cache Repository over REST API Service) must be completed
