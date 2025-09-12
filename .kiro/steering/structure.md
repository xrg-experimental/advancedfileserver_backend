# Project Structure

## Root Directory Organization
```
├── src/                    # Source code
├── target/                 # Maven build output
├── logs/                   # Application log files
├── docs/                   # Project documentation
├── project_docs/           # Analysis and sprint documentation
├── scripts/                # Utility scripts
├── .kiro/                  # Kiro AI assistant configuration
├── .github/                # GitHub workflows and templates
├── .mvn/                   # Maven wrapper configuration
├── pom.xml                 # Maven project configuration
├── docker-compose.yaml     # PostgreSQL container setup
└── .env.example           # Environment variables template
```

## Java Package Structure
Base package: `com.sme.afs`

```
├── config/                 # Spring configuration classes
├── controller/             # REST API endpoints
├── dto/                    # Data Transfer Objects
├── error/                  # Error codes and interfaces
├── exception/              # Custom exceptions and handlers
├── listener/               # Event listeners
├── model/                  # JPA entities and enums
│   └── filesystem/         # File system specific models
├── repository/             # JPA repositories
├── security/               # Security configuration and services
│   └── annotation/         # Security annotations
├── service/                # Business logic services
│   └── filesystem/         # File system services
├── util/                   # Utility classes
└── web/                    # Web filters and interceptors
```

## Resource Organization
```
src/main/resources/
├── db/
│   ├── migration/          # Flyway database migrations
│   └── demo/              # Demo data scripts
├── application.yml         # Main configuration
└── logback.xml            # Logging configuration

src/test/resources/
├── application-test.yml    # Test configuration
└── logback-test.xml       # Test logging configuration
```

## Naming Conventions

### Classes
- **Controllers**: `*Controller.java` (e.g., `FileController.java`)
- **Services**: `*Service.java` (e.g., `FileService.java`)
- **Repositories**: `*Repository.java` (e.g., `FileEntityRepository.java`)
- **DTOs**: `*Request.java`, `*Response.java`, `*DTO.java`
- **Entities**: Descriptive names (e.g., `FileEntity.java`, `User.java`)
- **Exceptions**: `*Exception.java` (e.g., `AfsException.java`)
- **Configuration**: `*Config.java`, `*Properties.java`

### Database
- **Tables**: Snake case (e.g., `file_entity`, `user_session`)
- **Migrations**: `V{version}__{description}.sql` (e.g., `V1__Initial_schema.sql`)

### API Endpoints
- Base path: `/api`
- RESTful conventions with plural nouns
- Examples: `/api/files`, `/api/users`, `/api/groups`

## Key Architectural Patterns
- **Layered Architecture**: Controller → Service → Repository → Entity
- **DTO Pattern**: Separate request/response objects from entities
- **Repository Pattern**: JPA repositories for data access
- **Configuration Properties**: External configuration via `@ConfigurationProperties`
- **Global Exception Handling**: Centralized error handling with `@ControllerAdvice`