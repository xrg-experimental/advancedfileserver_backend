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