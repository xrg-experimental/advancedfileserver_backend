# Virtual File System Specification

## 1. Authentication System

### 1.1 Administrator Authentication
- Uses Synology DSM authentication
- Requires OTP (One-Time Password) setup
- Full access to DSM features and settings

### 1.2 Regular User Authentication
- Separate authentication system (not using DSM)
- Simple username/password authentication
- No DSM account required
- Authentication token-based access

## 2. Core Virtual File System Components

### 2.1 Base Structure
- Root directory mapping to single DSM shared folder
- Metadata storage for files and directories
- ACL configuration storage
- Share link records

### 2.2 Storage Hierarchy
```
/mock_storage/
  ├── groups/               # Shared spaces for groups of users
  │   ├── group-a/         # Group specific directories
  │   ├── group-b/
  │   └── group-c/
  ├── system/              # System related storage
  │   ├── acls/           # ACL configuration files
  │   ├── metadata/       # File metadata storage
  │   └── share_links/    # Share link definitions
  └── logs/               # Admin-only access logs
```

## 3. API Endpoints

### 3.1 Authentication Endpoint DSM
```
# Admin Authentication (DSM)
GET    /webapi/auth.cgi     # Existing DSM auth endpoint

### 3.2 Authentication Endpoints Backend

# User Authentication
POST   /api/auth/login      # Admin or User login
POST   /api/auth/code       # Security code (OTP or other)
POST   /api/auth/logout     # Admin or User logout
GET    /api/auth/validate   # Validate session token
```

### 3.2 File Operation Endpoints
```
GET    /api/files/list      # List directory contents
GET    /api/files/info      # Get file/directory info
POST   /api/files/create    # Create directory
POST   /api/files/delete    # Delete file/directory
POST   /api/files/rename    # Rename file/directory
POST   /api/files/move      # Move file/directory
GET    /api/files/download  # Download file
POST   /api/files/upload    # Upload file
```

### 3.3 Share Management Endpoints
```
POST   /api/share/create    # Create share link
GET    /api/share/list      # List share links
DELETE /api/share/delete    # Delete share link
```

## 4. Access Control Implementation

### 4.1 ACL Structure
```json
{
    "path": "/groups/group-a/project1",
    "permissions": {
        "users": {
            "internal1": ["read", "write", "delete"],
            "external1": ["read"]
        },
        "groups": {
            "admins": ["read", "write", "delete", "admin"],
            "developers": ["read", "write"]
        }
    }
}
```

### 4.2 Permission Types
- READ: View files and folders
- WRITE: Create and modify files
- DELETE: Remove files and folders
- ADMIN: Modify permissions (admin only)
- SHARE: Create sharing links

### 4.3 Admin Privileges
- Full access to all directories
- ACL management
- User management
- Storage quota management
- Access to system logs

## 5. Share Link System

### 5.1 Link Structure
```json
{
    "linkId": "unique-share-id",
    "sourcePath": "/groups/group-a/project1/document.pdf",
    "expiration": "2024-12-31T23:59:59Z",
    "permissions": ["read", "download"],
    "password": "optional-password-hash",
    "creator": "internal1",
    "created": "2024-11-12T10:00:00Z"
}
```

### 5.2 Share Management
- Link expiration handling
- Password protection (optional)
- Access logging
- Download tracking

## 6. Implementation Tasks

1. **Authentication System**
   - Implement DSM authentication with OTP for admins
   - Create separate authentication system for regular users
   - Set up token-based session management
   - Implement permission verification middleware

2. **File System Operations**
   - Implement basic file operations
   - Set up ACL checking system
   - Create file metadata management
   - Implement file upload/download handling

3. **Access Control**
   - Implement ACL storage and retrieval
   - Create permission inheritance system
   - Set up group-based access control
   - Implement admin privilege management

4. **Share System**
   - Create share link generation
   - Implement link expiration
   - Set up password protection
   - Create access logging system

## 7. Testing Requirements

### 7.1 Authentication Tests
- Admin DSM authentication with OTP
- Regular user authentication
- Session management
- Permission validation

### 7.2 File Operation Tests
```json
{
    "testCases": [
        {
            "operation": "upload",
            "user": "internal1",
            "path": "/groups/group-a/test.txt",
            "expectedResult": "success"
        },
        {
            "operation": "read",
            "user": "external1",
            "path": "/groups/group-a/test.txt",
            "expectedResult": "denied"
        }
    ]
}
```

### 7.3 ACL Tests
- Permission inheritance
- Group access rules
- Admin override capabilities
- Share link access

## 8. Response Formats

### 8.1 Authentication Response
```json
{
    "success": true,
    "data": {
        "token": "user-session-token",
        "user": {
            "username": "internal1",
            "groups": ["group-a", "developers"],
            "isAdmin": false
        }
    }
}
```

### 8.2 File Operation Response
```json
{
    "success": true,
    "data": {
        "files": [
            {
                "path": "/groups/group-a/document.pdf",
                "name": "document.pdf",
                "size": 1024576,
                "modified": "2024-11-13T10:00:00Z",
                "permissions": {
                    "read": true,
                    "write": true,
                    "delete": false
                }
            }
        ]
    }
}
```
