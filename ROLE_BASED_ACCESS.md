# Role-Based Access Control

Complete overview of the role-based access system in the Music Streaming Backend.

## Overview

The backend implements a simple but effective role-based access control system with two roles:
- **Admin**: Can upload tracks + all user permissions
- **User**: Can browse, play music, and manage their own playlists

## Super Admin Account

### Automatic Creation

The super admin account is automatically created when the backend starts for the first time:

```
Username: kaman
Password: Johnedoms2@
Email: kaman@musicstream.local
Role: admin
```

This is configured in [cmd/server/main.go](cmd/server/main.go:97-142) and runs during the startup sequence.

### How It Works

1. Backend starts
2. Database migrations run
3. `seedSuperAdmin()` function checks if user "kaman" exists
4. If not exists, creates the user with:
   - Bcrypt hashed password
   - Role set to "admin"
   - Default preferences

## Database Schema

### Users Table

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) DEFAULT 'user',  -- NEW COLUMN
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

### Role Values

- `admin` - Can upload tracks
- `user` - Default for all new registrations

## API Changes

### 1. Authentication Responses

All auth endpoints now include the user's role:

**POST /api/auth/login**
**POST /api/auth/register**
**GET /api/user/me**

Response:
```json
{
  "token": "jwt-token-here",
  "user": {
    "id": 1,
    "username": "kaman",
    "email": "kaman@musicstream.local",
    "display_name": "Super Admin",
    "role": "admin",              ← NEW FIELD
    "created_at": "2024-01-15T10:30:00Z",
    "updated_at": "2024-01-15T10:30:00Z"
  }
}
```

### 2. Upload Endpoint Protection

**POST /api/tracks/upload** now checks user role:

```go
// Check if user is admin
var userRole string
err := a.storage.DB.DB.QueryRow(`SELECT role FROM users WHERE id = $1`, claims.UserID).Scan(&userRole)

if userRole != models.RoleAdmin {
    respondError(w, http.StatusForbidden, "only admins can upload tracks")
    return
}
```

**Response Codes:**
- `201 Created` - Upload successful (admin user)
- `403 Forbidden` - User is not admin
- `401 Unauthorized` - Not logged in

### 3. All Other Endpoints

All other endpoints (browse, play, playlists, search) are available to **all authenticated users** regardless of role.

## Permission Matrix

| Action | Admin | User |
|--------|-------|------|
| Register account | ✅ | ✅ |
| Login | ✅ | ✅ |
| Browse tracks | ✅ | ✅ |
| Stream tracks | ✅ | ✅ |
| Search | ✅ | ✅ |
| Create playlists | ✅ | ✅ |
| Manage own playlists | ✅ | ✅ |
| View play history | ✅ | ✅ |
| Update preferences | ✅ | ✅ |
| **Upload tracks** | ✅ | ❌ |
| **Delete tracks** | ✅ (own only) | ❌ |

## Android App Integration

### 1. Check User Role After Login

```kotlin
val response = RetrofitClient.api.login(
    LoginRequest(username = "kaman", password = "Johnedoms2@")
)

if (response.isSuccessful) {
    val user = response.body()?.user
    if (user?.role == "admin") {
        // Show upload button in UI
    } else {
        // Hide upload button
    }
}
```

### 2. Conditional UI Display

```kotlin
// In Settings Fragment
if (sessionManager.getUser()?.isAdmin() == true) {
    binding.uploadMusicCard.visibility = View.VISIBLE
} else {
    binding.uploadMusicCard.visibility = View.GONE
}
```

### 3. Handle 403 Errors

```kotlin
try {
    val response = RetrofitClient.api.uploadTrack(filePart)
    if (response.code() == 403) {
        Toast.makeText(context, "Only admins can upload tracks", Toast.LENGTH_LONG).show()
    }
} catch (e: Exception) {
    // Handle error
}
```

## Testing

### Test as Admin

```bash
# Login as admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"kaman","password":"Johnedoms2@"}' \
  | jq -r '.token'

# Save token
export TOKEN="your-token-here"

# Upload (should succeed)
curl -X POST http://localhost:8080/api/tracks/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@song.aac"

# Expected: 201 Created
```

### Test as Regular User

```bash
# Register regular user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","email":"user1@test.com","password":"pass123","display_name":"User One"}' \
  | jq -r '.token'

# Save token
export USER_TOKEN="user-token-here"

# Try to upload (should fail)
curl -X POST http://localhost:8080/api/tracks/upload \
  -H "Authorization: Bearer $USER_TOKEN" \
  -F "file=@song.aac"

# Expected: 403 Forbidden
# {"error":"only admins can upload tracks"}
```

### Verify Role in Response

```bash
# Login and check role
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"kaman","password":"Johnedoms2@"}' \
  | jq '.user.role'

# Output: "admin"
```

## Security Considerations

### 1. Password Security

The super admin password is:
- ✅ Hashed with bcrypt (not stored in plain text)
- ✅ Created only once on first startup
- ✅ Can be changed by updating the code and restarting

**For production:** Consider using environment variables for admin credentials:
```go
username := os.Getenv("ADMIN_USERNAME")
password := os.Getenv("ADMIN_PASSWORD")
```

### 2. Role Changes

Currently, roles cannot be changed after user creation. To implement role promotion:

```sql
-- Make a user an admin
UPDATE users SET role = 'admin' WHERE username = 'someuser';

-- Demote an admin to user
UPDATE users SET role = 'user' WHERE username = 'someuser';
```

### 3. Multiple Admins

To create additional admin users, directly insert into the database:

```sql
-- After the user registers normally
UPDATE users SET role = 'admin' WHERE username = 'newadmin';
```

Or add a management endpoint (future enhancement):
```go
// POST /api/admin/users/{id}/role (admin only)
// Body: {"role": "admin"}
```

## Future Enhancements

Potential improvements to the role system:

1. **More Granular Roles**
   - `super_admin` - Can manage other admins
   - `admin` - Can upload/delete tracks
   - `moderator` - Can delete tracks but not upload
   - `user` - Basic access

2. **Role Management API**
   ```
   POST /api/admin/users/{id}/role
   GET /api/admin/users (list all users with roles)
   ```

3. **Permissions System**
   ```go
   type Permission string
   const (
       PermissionUpload Permission = "tracks.upload"
       PermissionDelete Permission = "tracks.delete"
       PermissionManageUsers Permission = "users.manage"
   )
   ```

4. **Environment-Based Admin**
   - Configure admin credentials via .env
   - Support multiple initial admins
   - Rotate admin passwords

5. **Audit Log**
   - Track all upload/delete actions
   - Log role changes
   - Monitor admin activities

## Files Modified

To implement role-based access, these files were modified:

1. [internal/models/models.go](internal/models/models.go) - Added `Role` field and `IsAdmin()` method
2. [internal/storage/postgres.go](internal/storage/postgres.go) - Added `role` column to users table
3. [cmd/server/main.go](cmd/server/main.go) - Added `seedSuperAdmin()` function
4. [internal/api/auth_handlers.go](internal/api/auth_handlers.go) - Include role in auth responses
5. [internal/api/track_handlers.go](internal/api/track_handlers.go) - Check admin role before upload

## Summary

- ✅ Super admin `kaman` created automatically
- ✅ Role field added to User model
- ✅ Upload endpoint protected (admin only)
- ✅ Role returned in all auth responses
- ✅ Simple two-role system (admin/user)
- ✅ Android integration documented
- ✅ Test cases provided

**For your Android app:** Check `user.role == "admin"` after login to show/hide the upload button in settings!

---

See [ANDROID_INTEGRATION.md](ANDROID_INTEGRATION.md) for complete Android implementation guide.
