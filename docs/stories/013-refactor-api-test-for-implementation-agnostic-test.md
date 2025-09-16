# Story 013: Refactor API Tests for Implementation-Agnostic Testing

## User Story
As a developer, I want to refactor existing API tests to be implementation-agnostic so that I can ensure 
the REST layer works correctly and prepare for testing with multiple cache implementations.

## Description
Transform the existing Redis-specific end-to-end tests into an abstract test suite that can validate API 
behavior against any CacheRepository implementation. Update tests to reflect recent acceptance test changes 
and fix controller implementation issues that don't match Redis specification.

## Acceptance Criteria
- [ ] Existing Redis-specific API tests converted to abstract test class
- [ ] API test coverage updated to reflect repository acceptance test changes
- [ ] Controller implementation fixed for Redis specification compliance (e.g. negative indices in ranges)
- [ ] Abstract test structure ready for multiple implementation testing

## Technical Considerations
- Use abstract test class pattern similar to CacheRepositoryTestAcceptanceTest
- Ensure tests validate API contracts, not implementation details
- Update any Redis-specific assertions to be implementation-neutral
- Fix controller logic to properly handle Redis specification edge cases

## Definition of Done
- [ ] Abstract API test class implemented
- [ ] Controller fixes implemented for Redis specification compliance
- [ ] Integration tests written and passing
- [ ] ArchUnit tests passing
- [ ] Code reviewed
- [ ] Documentation updated

## Implementation Notes
- Refactor existing CacheControllerIntegrationTest to abstract base
- Fix controller logic for negative index handling in range operations
- Ensure error responses match Redis behavior patterns
- Use RestAssured for HTTP-level validation

## Dependencies
- None (all features already implemented)
