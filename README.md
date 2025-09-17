# Cache Service

A service wrapper example to implement a distributed cache over a REST service.

## Overview

The scope of the service is to provide a cache system by using a subset of Redis instructions. In the future this
structure can be replaced by another solution.

## Prerequisites

- **Java**: OpenJDK 21 or higher
- **Docker**: For running Redis locally
- **Docker Compose**: For orchestrating local development infrastructure

## Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd cache-service
   ```

2. **Start local infrastructure ** (Only required to work with Redis implementation)
   ```bash
   cd infrastructure/local
   docker-compose up -d
   ```

3. **Run the application**
   ```bash
   ./gradlew bootRun
   ```

4. **Verify the service is running**
   ```bash
   curl http://localhost:8080/keys/count
   # Should return: 0
   ```

## API Usage Examples

### Basic Key-Value Operations

```bash
# Set a value
curl -X PUT "http://localhost:8080/mykey" \
     -H "Content-Type: text/plain" \
     -d "Hello World"

# Get a value
curl http://localhost:8080/mykey
# Returns: Hello World

# Delete a key
curl -X DELETE http://localhost:8080/mykey

# Count total keys
curl http://localhost:8080/keys/count
```

### Counter Operations

```bash
# Increment a counter
curl -X PUT http://localhost:8080/counter/increment
# Returns: 1

# Increment again
curl -X PUT http://localhost:8080/counter/increment
# Returns: 2
```

### Ranking Operations

```bash
# Add players to ranking
curl -X POST "http://localhost:8080/leaderboard/ranking" \
     -H "Content-Type: application/json" \
     -d '{"member": "player1", "score": 100.0}'

curl -X POST "http://localhost:8080/leaderboard/ranking" \
     -H "Content-Type: application/json" \
     -d '{"member": "player2", "score": 250.0}'

# Get player rank (0-based)
curl http://localhost:8080/leaderboard/ranking/player1/rank
# Returns: 0 (lowest score = rank 0)

# Get ranking range
curl "http://localhost:8080/leaderboard/ranking/range?start=0&stop=10"
# Returns: ["player1", "player2"]

# Get ranking count
curl http://localhost:8080/leaderboard/ranking/count
# Returns: 2
```

## Technical Stack

- **Language**: Kotlin
- **Framework**: Spring Boot 3
- **Architecture**: Onion Architecture
- **Cache**: Redis/In-Memory (Configurable)
- **Testing**: JUnit 5, TestContainers
- **Build**: Gradle
- **Architecture Validation**: ArchUnit

## Local Development Infrastructure

The directory `infrastructure/local` contains a Docker Compose configuration for local development when 
the application is configured to work with Redis as foundation of the `CacheRepository`.

### Services

- **Redis** - Cache database (port 6379)
- **Redis Insight** - Web UI for Redis (port 5540)

### Usage

```bash
cd infrastructure/local

# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Access

- **Redis**: `localhost:6379`
- **Redis Insight UI**: http://localhost:5540

### Configuration

- `redis.conf`: Redis server configuration
- Data is persisted in Docker volume `redis_data`
- Memory limit: 256MB with LRU eviction policy

## Application Configuration

The application can be configured through `application.yaml`:
To configure the cache implementation to use, set the cache implementation to one of the following options:

```yaml
cache:
  implementation: inmemory  # Default: inmemory, Options: inmemory|redis
```

## Building and Testing

### Build the application

```bash
./gradlew build
```

### Run tests

All tests
```bash
./gradlew test
```

Architecture validation only

```bash
./gradlew test --tests "*ArchitectureTest"
```

## Development Approach

All stories should be implemented to support a future interaction with AI agents. To achieve this, every story must
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

### Package Structure

```
src/main/kotlin/com/fsg/cacheservice/
├── CacheServiceApplication.kt          # Application entry point
├── api/                                # REST controllers and DTOs
├── core/                               # Domain logic and interfaces
├── infrastructure/                     # External integrations (Redis)
└── configuration/                      # Spring configuration
```

## Test Infrastructure

### Integration Tests

**TestContainers** are used to design integration tests against the infrastructure. The project provides
base classes to simplify TestContainer usage.

### Base Test Classes

#### RedisTestBase

Provides Redis container for integration tests:

```kotlin
@SpringBootTest
class MyIntegrationTest : RedisTestBase() {

    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("Unused")
        fun configureProperties(registry: DynamicPropertyRegistry) {
            configureRedisProperties(registry)
        }
    }

    @Test
    fun `test redis operations`() {
        // Redis container available automatically
        redisTemplate.opsForValue().set("key", "value")
        // ...
    }
}
```

Calling the `configureRedisProperties` method is a workaround that must be fixed the sooner as possible.

#### RedisTestWithInsightBase

In case a  RedisInsight container is needed together with the Redis one, the class `RedisTestWithInsightBase` can be
used instead of `RedisTestBase`. It must be configured the same way.

### Important Notes

- **Port Isolation**: Test containers use different ports than development environment
- **Shared Containers**: Containers are shared across tests for performance
- **Automatic Cleanup**: Containers are automatically stopped after test execution

## Troubleshooting

### Common Issues

**Tests failing with containers**

```bash
# Clean Docker resources
docker system prune -f

# Restart Docker daemon if needed
```

## Contributing

1. Follow the existing code style and architecture patterns
2. Write tests for new functionality
3. Update documentation for API changes
4. Ensure ArchUnit tests pass
5. Create story cards for new features in `docs/stories/`
