# Implementation Plan

- [x] 1. Create foundation with data model and hard link management





  - Create BlobUrl JPA entity with all required fields and annotations
  - Create database migration script for blob_urls table
  - Implement HardLinkManager class with cross-platform hard link creation/deletion
  - Add BlobUrlProperties configuration class and application.yml settings
  - Write comprehensive unit tests for HardLinkManager and integration tests for entity persistence
  - **Deliverable**: Working hard link creation/deletion with database persistence, fully tested
  - _Requirements: 1.1, 1.2, 2.1, 4.1, 6.1, 6.2, 6.4_

- [x] 2. Implement secure token service and blob URL core logic





  - Create TokenService with cryptographically secure token generation and validation
  - Implement BlobUrlService with createBlobUrl, getBlobUrlStatus, and validateAndGetFile methods
  - Create BlobUrlRepository with custom queries for expired URL cleanup
  - Integrate with existing FileService for file validation and metadata
  - Write unit tests for token security and service methods, plus integration tests for complete workflow
  - **Deliverable**: Complete blob URL creation and validation system with secure tokens, fully tested
  - _Requirements: 1.1, 1.2, 1.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3_

- [ ] 3. Build REST API with DTOs and error handling
  - Create BlobUrlResponse and request DTOs with proper validation annotations
  - Implement BlobUrlController with create, status, and download endpoints
  - Add comprehensive error handling with custom exceptions and global exception handler
  - Integrate Spring Security for authentication and authorization
  - Write integration tests for all API endpoints including error scenarios and security
  - **Deliverable**: Complete REST API for blob URLs with proper error handling, fully tested
  - _Requirements: 1.1, 1.2, 1.4, 3.1, 3.2, 3.3, 4.3, 5.1, 5.2, 5.4_

- [ ] 4. Add automatic cleanup and system validation
  - Implement CleanupScheduler with @Scheduled cleanup and startup orphan removal
  - Add filesystem validation on application startup with proper error handling
  - Create system health checks and monitoring for blob URL operations
  - Implement comprehensive logging for all operations with appropriate levels
  - Write integration tests for cleanup operations and end-to-end workflow tests
  - **Deliverable**: Complete system with automatic cleanup, validation, and monitoring, fully tested
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 6.1, 6.2_