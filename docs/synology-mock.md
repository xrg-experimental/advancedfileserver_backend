# Synology DSM Authentication Mock Server Specification

## Overview
This document specifies a mock server that simulates the Synology DSM authentication API for development and demo purposes.

## Configuration

### Server Settings
- Host: localhost
- Port: 5000
- Protocol: http (not https for local development)
- Base Path: /webapi

### Demo Credentials
Use the credentials from demo.md:
- All users have password: `password123`
- Available users:
  - admin1
  - admin2
  - admin3

## API Endpoints

### Authentication Endpoint
```
GET /webapi/auth.cgi
```

#### Query Parameters
- `api` (required): Must be "SYNO.API.Auth"
- `version` (required): API version (accepts "2")
- `method` (required): Must be "login"
- `account` (required): Username
- `passwd` (required): Password
- `otp_code` (required): OTP
- `session` (required): Session name (e.g., "AFS")
- `format` (required): Must be "sid"

#### Success Response
Status: 200 OK
```json
{
    "success": true,
    "data": {
        "sid": "mock-session-{timestamp}-{username}"
    }
}
```

#### Error Responses

1. Invalid Credentials
Status: 200 OK
```json
{
    "success": false,
    "error": {
        "code": 400
    }
}
```

2. Missing Parameters
Status: 200 OK
```json
{
    "success": false,
    "error": {
        "code": 101
    }
}
```

3. Invalid API/Method
Status: 200 OK
```json
{
    "success": false,
    "error": {
        "code": 102
    }
}
```

## Error Codes
- 400: Invalid credentials
- 101: Missing parameter
- 102: Invalid API/method name

## Implementation Notes

1. The mock server should:
   - Accept HTTP connections (not HTTPS) for simplicity
   - Validate all required parameters
   - Check credentials against the demo user list
   - Generate consistent session IDs
   - Respond with proper error codes

2. Authentication Logic:
   - Check if username exists in demo users list
   - Verify password matches "password123"
   - Verify OTP matches "333666"
   - Generate a mock session ID using pattern: `mock-session-{timestamp}-{username}`

3. Response Headers:
   ```
   Content-Type: application/json
   Access-Control-Allow-Origin: *
   ```

4. CORS Support:
   - Enable CORS for local development
   - Allow all origins during development
   - Support OPTIONS preflight requests

## Testing

### Sample cURL Commands

1. Valid Login:
```bash
curl "http://localhost:5000/webapi/auth.cgi?api=SYNO.API.Auth&version=2&method=login&account=admin1&passwd=password123&otp_code=333666&session=FileStation&format=sid"
```

2. Invalid Password:
```bash
curl "http://localhost:5000/webapi/auth.cgi?api=SYNO.API.Auth&version=2&method=login&account=admin1&passwd=wrongpass&otp_code=333666&session=FileStation&format=sid"
```

3. Missing Parameter:
```bash
curl "http://localhost:5000/webapi/auth.cgi?api=SYNO.API.Auth&version=2&method=login&account=admin1&session=FileStation&format=sid"
```

### Expected Test Results

1. Valid Login:
```json
{
    "success": true,
    "data": {
        "sid": "mock-session-1234567890-admin1"
    }
}
```

2. Invalid Password:
```json
{
    "success": false,
    "error": {
        "code": 400
    }
}
```

3. Missing Parameter:
```json
{
    "success": false,
    "error": {
        "code": 101
    }
}
```

## Development Setup

1. Update application.yml for local development:
```yaml
synology:
  host: localhost
  port: 5000
  protocol: http
  verify-ssl: false
```

2. Start mock server before running the application
3. Use demo credentials for testing
4. Monitor mock server logs for authentication attempts
