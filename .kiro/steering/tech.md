# Technology Stack

## Build System
- **Maven** - Primary build tool with Spring Boot parent POM
- **Java 17** - Target JVM version
- **Spring Boot 3.2.0** - Application framework

## Core Technologies
- **Spring Boot** - Web, Security, Data JPA, Validation, Mail
- **Spring Security** - Authentication and authorization
- **PostgreSQL** - Primary database (production)
- **H2** - In-memory database (testing)
- **Flyway** - Database migration management
- **JWT (JJWT 0.11.5)** - Token-based authentication
- **Lombok** - Code generation and boilerplate reduction

## Key Libraries
- **Jackson** - JSON processing with JSR310 datetime support
- **Apache Commons** - Lang3 and IO utilities
- **Apache HttpClient5** - HTTP client operations
- **WireMock** - API mocking for testing
- **SpringDoc OpenAPI** - API documentation (Swagger)
- **TOTP Library** - Multi-factor authentication

## Development Tools
- **Docker Compose** - Local PostgreSQL setup
- **Swagger UI** - API documentation at `/api/swagger-ui.html`
- **Maven Wrapper** - Consistent build environment

## Common Commands

### Database Management
```bash
# Start PostgreSQL with Docker Compose
docker-compose up -d

# Clean database schema
mvn flyway:clean

# Run migrations
mvn flyway:migrate

# Run migrations with demo data
mvn flyway:migrate@demo-data

# Access database directly
docker exec -it afs-postgres psql -U afs_user advancedfileserver
```

### Application Development
```bash
# Run application
mvn spring-boot:run

# Run tests
mvn test

# Build JAR
mvn clean package

# Skip tests during build
mvn clean package -DskipTests
```

### Environment Setup
```bash
# Copy environment template
copy .env.example .env
# Edit .env with your database credentials
```

## Configuration Profiles
- **demo** - Development with local file system (D:/demo/shared)
- **test** - Testing environment (D:/test/shared)  
- **production** - Synology NAS integration (/volume1/shared)

## Development Guidelines

### Time Handling
- **Use UTC for internal time objects** - All internal time representations should use UTC to avoid timezone issues
- Store and process times in UTC, convert to local time only for display purposes

### Testing Standards
- **Prefer AssertJ with AAA pattern** - Use AssertJ assertions with Arrange, Act, Assert structure
- Add clear comments marking each section: `// Arrange`, `// Act`, `// Assert`
- Each task should include at least unit or integration tests to validate functionality

### Security Practices
- **Do not expose sensitive data in error messages** - Remove passwords completely or show only partial tokens
- **Validate all user input** - Implement proper input validation for all endpoints and services
- Sanitize error messages to prevent information leakage

### Code Quality
- **Extract functions for repetitive code** - Reduce duplication by creating reusable utility functions
- **Create focused, testable tasks** - Each task should produce visible, testable results
- Prefer smaller, well-defined tasks over large complex implementations

### Task Structure
- **Reduced number of meaningful tasks** - Create tasks that show clear progress and results
- Each task completion should demonstrate working functionality through tests
- Focus on incremental, verifiable progress rather than large feature dumps