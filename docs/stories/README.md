# Development Stories

This document tracks all development stories for the Cache Service project. Each story is written to help AI
assistants in the future to get a deep understanding on how the project evolved.

## Technical Debt

| Issue                                  | Description                                                                                                                                                                                                                     | Reference File                                                                                                     | Priority |
|----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|----------|
| Parameter Validation                   | Add validation for null/empty keys and members                                                                                                                                                                                  | [RedisCacheRepository.kt](../../src/main/kotlin/com/fsg/cacheservice/infrastructure/redis/RedisCacheRepository.kt) | Medium   |
| Exception Handling                     | Implement consistent Redis exception wrapping across all methods                                                                                                                                                                | [RedisCacheRepository.kt](../../src/main/kotlin/com/fsg/cacheservice/infrastructure/redis/RedisCacheRepository.kt) | Low      |
| Decorator Pattern                      | Consider parameter validation decorator to respect SRP                                                                                                                                                                          | [RedisCacheRepository.kt](../../src/main/kotlin/com/fsg/cacheservice/infrastructure/redis/RedisCacheRepository.kt) | Low      |
| Set up redis dynamic properties        | Fix repeated @DynamicPropertySource in RedisTestBase                                                                                                                                                                            | [RedisTestBase.kt](../../src/test/kotlin/com/fsg/cacheservice/testcontainers/RedisTestBase.kt)                     | Medium   |
| Review parameter validation duplicated | Controllers and `RedisCacheRepository` have duplicated validation logic (e.g., range validation in both layers). This creates maintenance overhead and inconsistent error handling. Should consolidate validation in one layer. |                                                                                                                    | Medium   

## Story Status

| ID  | Story                                         | Status      | Description                                                       |
|-----|-----------------------------------------------|-------------|-------------------------------------------------------------------|
| 001 | Setup Project                                 | ✅ Done      | Initial project configuration and structure                       |
| 002 | ArchUnit Definition                           | ✅ Done      | Configure ArchUnit for architectural compliance                   |
| 003 | Cache Repository Definition                   | ✅ Done      | Define core cache repository interface                            |
| 004 | Configure Redis and Test Dependencies         | ✅ Done      | Add Redis, TestContainers and integration tests                   |
| 005 | Implement Cache Repository with Redis         | ✅ Done      | Implement Redis-based cache repository                            |
| 006 | Expose Cache Repository over READ API Service | ✅ Done      | Implement REST API controller to expose the cache                 |
| 007 | Control REST API errors with ControllerAdvice | ✅ Done      | Handle HTTP error responses and exceptions using ControllerAdvice |
| 007 | Document application setup                    | 🏗️ Planned | Document how to setup the projecto develop locally                |

## Status Legend

- 🏗️ **Planned**: Story defined but not started
- 🚧 **In Progress**: Currently being developed
- ✅ **Done**: Story completed and merged
- ⏸️ **Blocked**: Story blocked by dependencies
- ❌ **Cancelled**: Story cancelled or deprecated

## Story Template

Each story follows this template structure defined in the [Story Template File](000-story-template.md)

## Creating New Stories

1. Copy the template above
2. Create a new file: `XXX-story-name.md` in this directory
3. Fill in all sections
4. Update this README with the new story entry
5. Link any dependencies between stories

## Development Workflow

1. **Story Definition**: Define story card with all acceptance criteria
2. **Technical Design** (Optional): AI-assisted technical analysis and design decisions
3. **Implementation**: Code development
4. **Testing**: Comprehensive testing at unit and integration levels
5. **Review**: Code review and architectural compliance check
6. **Documentation**: Update relevant documentation

## Guidelines

- Each story should be independently testable
- Stories should follow the onion architecture principles
- All stories must pass ArchUnit tests
- Prefer small, focused stories over large ones
- Dependencies between stories should be minimal and explicit
