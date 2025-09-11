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

## Local Development Infrastructure

The directory `/infrastructure/local` contains a Docker Compose configuration for local development.

### Services

- **Redis** - Cache database (port 6379)
- **Redis Insight** - Web UI for Redis (port 5540)

### Usage

```bash
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

## Test Infrastructure

### Integration test
**TestContainers** are used to be able to design integration test against the infrastructure. To ease the usage
the annotation `@WithTestContainers` can be used.

### @WithTestContainers annotation
This annotation orchestrate multiple tests containers. The idea is making easier the enabling of the needed containers.

#### Usage example
```kotlin
@SpringBootTest
@WithTestContainers(redis = true, redisInsight = true)
class MyIntegrationTest {

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>
    
    @Test
    fun `test redis operations`() {
        // Redis available on dynamic port
    }
}
```

#### Available parameters
- `redis: Boolean = false`: Starts Redis container exposed on dynamic port
- `redisInsight: Boolean = false`: Starts a container running RedisInsight. It's exposed in port 5541

#### Features

- Modular: Enable only the containers you need
- Singleton containers: Shared across tests for performance
- Port isolation: Test containers use different ports than development
- Automatic configuration: Spring properties configured automatically
- Dependency management: RedisInsight automatically depends on Redis
