# Cache Service
A service wrapper example to implement a distributed cache over a REST service.

## Overview
The scope of the service is to provide a cache system by using a subset of Redis instructions. In the future this
structure can be replaced by another solution.

## Technical stack

- **Language**: Kotlin
- **Framework**: Spring Boot 3
- **Architecture**: Onion Architecture
- **Testing**: JUnit 5, TestContainers
- **Build**: Gradle

## Development approach
All stories should be implemented to support a future interaction with IA agents. To achieve this, every story must
be documented in a card placed in the `docs/stories` directory using Markdown format. Each story includes:
- User story definition
- Acceptance criteria
- Technical considerations
- Implementation notes

See [Development Stories](docs/stories/README.md) for understanding the current roadmap.

## Architecture
As the project scope is simple, an onion architecture has been chosen. If in the future the project evolves,
don't stick to this architecture without thinking pros and cons of moving to a more sophisticated one.

ArchUnit will be used to ensure no one breaks the designed architecture.  
