# Story 011: Refactor CacheRepository Interface and Adapt Redis Implementation

## User Story
As a developer, I want to refactor the CacheRepository interface to remove all Redis concurrency errors from interface. 
Then adapt the Redis implementation so that it passes the acceptance tests and provides a solid foundation for 
the in-memory implementation.

## Description
Update the CacheRepository interface based on insights from creating acceptance tests, then modify the Redis 
implementation to pass all acceptance tests. Temporarily disable any failing API tests if they break during 
this refactoring.

## Acceptance Criteria
- [ ] All existing functionality preserved
- [ ] CacheRepository interface updated with any necessary changes
- [ ] Redis implementation (RedisAdapter) passes all acceptance tests
- [ ] Redis implementation properly handles all exception scenarios
- [ ] Remove any Redis-specific tests that are now covered by acceptance tests

## Definition of Done
- [ ] Code implemented
- [ ] Acceptance tests passing
- [ ] ArchUnit tests passing
- [ ] Code reviewed
- [ ] Documentation updated

## Technical Considerations
- Thread-safety verified for Redis implementation
- May need to adjust return types or exception handling
- Focus on acceptance tests as single source of truth for behavior validation

## Implementation Notes
- Redis exceptions must be correctly mapped. If designed exceptions don't match requirements, adapt them
- Ensure proper TTL handling and cleanup mechanisms
- Validate sorted set ordering matches Redis lexicographical + score behavior
- **Only acceptance tests matter** - remove redundant Redis-specific tests

## Dependencies
[Create agnostic cache repository acceptance test](010-create-agnostic-cache-repository-acceptance-test.md)
