# Story 003: Cache Repository Definition

## User Story

As a developer, I want to define the core cache repository interface so that I can implement different cache backends
while maintaining a clean contract.

## Description

This story focuses on defining the core abstraction for cache operations. The interface will be
framework-agnostic and placed in the core layer, following onion architecture principles.
This contract will be implemented later by Redis-specific classes.

1. SET key value
2. SET key value EX seconds
3. GET key
4. DEL key
5. DBSIZE
6. INCR key
7. ZADD key score member
8. ZCARD key
9. ZRANK key member
10. ZRANGE key start stop

## Acceptance Criteria

- [ ] Define the methods to implement all the required functionality

## Technical Considerations

- The interface will be created in the core package to preserve architecture.
- To avoid null pointer exceptions, use nullable types when data must be returned
- First version will be synchronous. If asynchrony is required, it will be implemented later.

## Definition of Done

- [ ] Code implemented
- [ ] ArchUnit tests passing
- [ ] Code reviewed
- [ ] Documentation updated

## Dependencies

- It can be done after [Project Setup story is done](001-project-setup.md).