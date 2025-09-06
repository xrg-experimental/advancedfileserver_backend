# Advanced File Server

A secure file exchange application for Synology DSM.

## Database Setup

You can set up the database either locally or using Docker Compose.

### Using Docker Compose (Recommended)

1. Make sure you have Docker and Docker Compose installed on your system

2. Create a `.env` file in the project root with the following variables:
   ```
   POSTGRES_DB=advancedfileserver
   POSTGRES_USER=afs_user
   POSTGRES_PASSWORD=your_password
   ```

3. Start the PostgreSQL container:
   ```bash
   docker-compose up -d
   ```

4. Verify the database is running:
   ```bash
   docker-compose ps
   ```

5. To stop the database:
   ```bash
   docker-compose down
   ```

### Database Schema

The database schema will be automatically created by Flyway migrations when the application starts.

Wipe existing database:

```bash
mvn flyway:clean
```

Create or migrate database:

```bash
mvn flyway:migrate
```

Create or migrate database and insert demo data:
```bash
mvn flyway:migrate@demo-data
```

#### Manually Access to the Database

Open a terminal in the running Docker container and enter

```bash
psql -U afs_user advancedfileserver
```

You can copy & paste the database schema now.

### Manual Setup

If you prefer to install PostgreSQL locally, follow these steps:

1. Install PostgreSQL if not already installed
2. Connect to PostgreSQL as superuser:
   ```psql
   psql -U postgres
   ```

3. Create the database:
   ```psql
   CREATE DATABASE advancedfileserver;
   ```

4. Create the application user:
   ```psql
   CREATE USER afs_user WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE advancedfileserver TO afs_user;
   ```

5. Set the database password environment variable:
   ```bash
   export DB_PASSWORD=your_password
   ```

#### How to access the `swagger-ui`?  

Based on the configuration in `application.yml` and `SecurityConfig.java`, the Swagger UI is accessible. 
You can access it in two ways:

1. Main Swagger UI interface:
```
http://localhost:8080/api/swagger-ui.html
```

2. OpenAPI documentation in JSON format:
```
http://localhost:8080/api/v3/api-docs
```

Note that the `/api` prefix is included because you have configured `server.servlet.context-path: /api` in your `application.yml`.

The security configuration in `SecurityConfig.java` already permits access to these endpoints without authentication:
```java
.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
```
