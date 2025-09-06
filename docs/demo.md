# Advanced File Server Demo Guide

This guide helps you test the Advanced File Server implementation using demo data.

## Demo Users

All users have the password: `password123`

| Username  | Type     | Description                    |
|-----------|----------|--------------------------------|
| admin1    | Admin    | Full system access             |
| admin2    | Admin    | Full system access             |
| admin3    | Admin    | Full system access             |
| internal1 | Internal | Marketing team member          |
| internal2 | Internal | Development team member        |
| external1 | External | External collaborator          |
| external2 | External | External collaborator          |

## Groups

| Name             | Description                    | Members                      |
|------------------|--------------------------------|------------------------------|
| Marketing        | Marketing team workspace       | admin1, internal1            |
| Development      | Development team workspace     | admin2, internal2            |
| External-Projects| External collaborators space   | admin1, external1, external2 |

## Testing the API

1. **Login**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin1", "password": "password123"}'
   ```

2. **Use the returned token for subsequent requests**
   ```bash
   # Replace {token} with the actual token from login response
   export TOKEN="{token}"
   ```

3. **Test Admin Access**
   ```bash
   curl http://localhost:8080/api/admin/test \
     -H "Authorization: Bearer $TOKEN"
   ```

4. **View User Sessions**
   ```bash
   curl http://localhost:8080/api/auth/sessions/internal1 \
     -H "Authorization: Bearer $TOKEN"
   ```

5. **Revoke User Sessions**
   ```bash
   curl -X POST http://localhost:8080/api/auth/sessions/internal1/revoke \
     -H "Authorization: Bearer $TOKEN"
   ```

6. **Test Different User Types**
   - Login as internal1 to test internal access
   - Login as external1 to test external access
   - Try accessing endpoints not allowed for your role

## Session Management Demo

1. **Create Multiple Sessions**
   - Login with the same user from different browsers/clients
   - Note that each login creates a new session
   - Maximum concurrent sessions are limited to 3 per user

2. **Session Expiration**
   - Sessions expire after 30 minutes of inactivity
   - Use token refresh before expiration:
   ```bash
   curl -X POST http://localhost:8080/api/auth/refresh \
     -H "Authorization: Bearer $TOKEN"
   ```

3. **Logout**
   ```bash
   curl -X POST http://localhost:8080/api/auth/logout \
     -H "Authorization: Bearer $TOKEN"
   ```

## Testing Different Access Levels

1. **Internal Access**
   ```bash
   # Login as internal1
   curl http://localhost:8080/api/internal/test \
     -H "Authorization: Bearer $TOKEN"
   ```

2. **External Access**
   ```bash
   # Login as external1
   curl http://localhost:8080/api/external/test \
     -H "Authorization: Bearer $TOKEN"
   ```

## Notes

- All passwords in the demo data are set to 'password123'
- Sessions automatically expire after 30 minutes of inactivity
- The refresh token window opens 5 minutes before token expiration
- Failed login attempts are logged
- Session management endpoints are only accessible to admin users
