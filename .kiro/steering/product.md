# Product Overview

Advanced File Server (AFS) is a secure file exchange application designed for Synology DSM environments. The application provides:

- Secure file sharing and management with role-based access control
- Integration with Synology DSM authentication and file systems
- Multi-factor authentication (TOTP) support
- Virtual file system abstraction over physical shared folders
- RESTful API with comprehensive Swagger documentation
- User and group management with granular permissions
- File versioning and metadata tracking

The system is designed to run as a Spring Boot application that interfaces with Synology NAS systems while providing its own authentication layer and file management capabilities.

## Key Features

- JWT-based authentication with session management
- File upload/download with large file support
- Virtual path mapping to physical shared folders
- Group-based permissions and access control
- Email notifications and user registration
- Comprehensive audit logging
- Docker-based PostgreSQL database setup