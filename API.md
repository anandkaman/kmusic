# API Reference

Complete API documentation for the Music Streaming Backend.

## Base URL

```
http://localhost:8080
```

## Authentication

All endpoints except health check and auth endpoints require a Bearer token.

```
Authorization: Bearer <jwt-token>
```

---

## Authentication Endpoints

### Register User

Create a new user account.

**Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secure_password",
  "display_name": "John Doe"
}
```

**Response:** `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "display_name": "John Doe",
    "created_at": "2024-01-15T10:30:00Z",
    "updated_at": "2024-01-15T10:30:00Z"
  }
}
```

### Login

Authenticate and receive JWT token.

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "username": "johndoe",
  "password": "secure_password"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "display_name": "John Doe",
    "created_at": "2024-01-15T10:30:00Z",
    "updated_at": "2024-01-15T10:30:00Z"
  }
}
```

### Get Current User

Get authenticated user's information.

**Endpoint:** `GET /api/user/me`

**Response:** `200 OK`
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "display_name": "John Doe",
  "created_at": "2024-01-15T10:30:00Z",
  "updated_at": "2024-01-15T10:30:00Z"
}
```

---

## Track Endpoints

### Upload Track

Upload a music file (AAC recommended).

**Endpoint:** `POST /api/tracks/upload`

**Content-Type:** `multipart/form-data`

**Form Data:**
- `file`: Audio file (AAC, MP3, FLAC supported)

**Response:** `201 Created`
```json
{
  "id": 1,
  "title": "Song Title",
  "artist": "Artist Name",
  "album": "Album Name",
  "album_artist": "Album Artist",
  "genre": "Rock",
  "year": 2024,
  "track_number": 1,
  "duration": 240,
  "file_size": 5242880,
  "file_format": "AAC",
  "bitrate": 320,
  "sample_rate": 44100,
  "uploaded_by": 1,
  "play_count": 0,
  "created_at": "2024-01-15T10:30:00Z",
  "updated_at": "2024-01-15T10:30:00Z"
}
```

**Example:**
```bash
curl -X POST http://localhost:8080/api/tracks/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@song.aac"
```

### List Tracks

Get list of tracks with optional filtering.

**Endpoint:** `GET /api/tracks`

**Query Parameters:**
- `artist` (optional): Filter by artist name (partial match)
- `album` (optional): Filter by album name (partial match)
- `genre` (optional): Filter by genre (partial match)
- `limit` (optional): Number of results (default: 50, max: 200)
- `offset` (optional): Offset for pagination (default: 0)

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "title": "Song Title",
    "artist": "Artist Name",
    "album": "Album Name",
    "genre": "Rock",
    "year": 2024,
    "duration": 240,
    "play_count": 42,
    ...
  }
]
```

**Examples:**
```bash
# Get all tracks
GET /api/tracks

# Filter by artist
GET /api/tracks?artist=Beatles

# Pagination
GET /api/tracks?limit=20&offset=40

# Multiple filters
GET /api/tracks?artist=Beatles&album=Abbey&limit=10
```

### Get Track Details

Get detailed information about a specific track.

**Endpoint:** `GET /api/tracks/{id}`

**Response:** `200 OK`
```json
{
  "id": 1,
  "title": "Song Title",
  "artist": "Artist Name",
  "album": "Album Name",
  "album_artist": "Album Artist",
  "genre": "Rock",
  "year": 2024,
  "track_number": 1,
  "duration": 240,
  "file_size": 5242880,
  "file_format": "AAC",
  "uploaded_by": 1,
  "play_count": 42,
  "created_at": "2024-01-15T10:30:00Z",
  "updated_at": "2024-01-15T10:30:00Z"
}
```

### Stream Track

Stream audio file with HTTP range request support.

**Endpoint:** `GET /api/tracks/{id}/stream`

**Headers:**
- `Range` (optional): Byte range for partial content (e.g., `bytes=0-1023`)

**Response:** `200 OK` or `206 Partial Content`
- Content-Type: `audio/aac`
- Accept-Ranges: `bytes`
- Content-Length: File size or range length
- Content-Range: (if partial) `bytes start-end/total`

**Example:**
```bash
# Stream full track
curl http://localhost:8080/api/tracks/1/stream \
  -H "Authorization: Bearer $TOKEN" \
  -o song.aac

# Request specific range
curl http://localhost:8080/api/tracks/1/stream \
  -H "Authorization: Bearer $TOKEN" \
  -H "Range: bytes=0-1048575" \
  -o partial.aac
```

### Delete Track

Delete a track (owner only).

**Endpoint:** `DELETE /api/tracks/{id}`

**Response:** `200 OK`
```json
{
  "message": "track deleted"
}
```

---

## Playlist Endpoints

### Create Playlist

Create a new playlist.

**Endpoint:** `POST /api/playlists`

**Request Body:**
```json
{
  "name": "My Favorites",
  "description": "All my favorite songs",
  "is_public": false
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "name": "My Favorites",
  "description": "All my favorite songs",
  "user_id": 1,
  "is_public": false,
  "track_count": 0,
  "created_at": "2024-01-15T10:30:00Z",
  "updated_at": "2024-01-15T10:30:00Z"
}
```

### List Playlists

Get user's playlists and public playlists.

**Endpoint:** `GET /api/playlists`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "My Favorites",
    "description": "All my favorite songs",
    "user_id": 1,
    "is_public": false,
    "track_count": 15,
    "created_at": "2024-01-15T10:30:00Z",
    "updated_at": "2024-01-15T10:30:00Z"
  }
]
```

### Get Playlist with Tracks

Get playlist details including all tracks.

**Endpoint:** `GET /api/playlists/{id}`

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "My Favorites",
  "description": "All my favorite songs",
  "user_id": 1,
  "is_public": false,
  "track_count": 2,
  "created_at": "2024-01-15T10:30:00Z",
  "updated_at": "2024-01-15T10:30:00Z",
  "tracks": [
    {
      "id": 1,
      "title": "Song 1",
      "artist": "Artist 1",
      ...
    },
    {
      "id": 2,
      "title": "Song 2",
      "artist": "Artist 2",
      ...
    }
  ]
}
```

### Update Playlist

Update playlist information.

**Endpoint:** `PUT /api/playlists/{id}`

**Request Body:**
```json
{
  "name": "Updated Name",
  "description": "Updated description",
  "is_public": true
}
```

**Response:** `200 OK`
```json
{
  "message": "playlist updated"
}
```

### Delete Playlist

Delete a playlist.

**Endpoint:** `DELETE /api/playlists/{id}`

**Response:** `200 OK`
```json
{
  "message": "playlist deleted"
}
```

### Add Track to Playlist

Add a track to a playlist.

**Endpoint:** `POST /api/playlists/{id}/tracks`

**Request Body:**
```json
{
  "track_id": 5
}
```

**Response:** `201 Created`
```json
{
  "message": "track added to playlist"
}
```

### Remove Track from Playlist

Remove a track from a playlist.

**Endpoint:** `DELETE /api/playlists/{id}/tracks/{trackId}`

**Response:** `200 OK`
```json
{
  "message": "track removed from playlist"
}
```

---

## User Preferences

### Get User Preferences

Get current user's preferences.

**Endpoint:** `GET /api/user/preferences`

**Response:** `200 OK`
```json
{
  "user_id": 1,
  "language": "en",
  "theme": "dark",
  "bitrate_prefer": 320,
  "auto_play": true,
  "crossfade_duration": 5,
  "offline_mode": false,
  "notify_new_tracks": true
}
```

### Update User Preferences

Update user preferences.

**Endpoint:** `PUT /api/user/preferences`

**Request Body:**
```json
{
  "language": "en",
  "theme": "light",
  "bitrate_prefer": 256,
  "auto_play": true,
  "crossfade_duration": 3,
  "offline_mode": false,
  "notify_new_tracks": false
}
```

**Response:** `200 OK`
```json
{
  "message": "preferences updated"
}
```

**Preference Fields:**
- `language`: UI language code (e.g., "en", "es", "fr")
- `theme`: UI theme ("dark", "light")
- `bitrate_prefer`: Preferred bitrate in kbps (128, 192, 256, 320)
- `auto_play`: Auto-play next track (boolean)
- `crossfade_duration`: Crossfade duration in seconds (0-10)
- `offline_mode`: Enable offline mode (boolean)
- `notify_new_tracks`: Notify about new tracks (boolean)

---

## Search & Discovery

### Search

Search tracks by title, artist, or album.

**Endpoint:** `GET /api/search`

**Query Parameters:**
- `q` (required): Search query

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "title": "Matching Song",
    "artist": "Artist Name",
    "album": "Album Name",
    ...
  }
]
```

**Example:**
```bash
GET /api/search?q=beatles
```

---

## Play History & Statistics

### Record Play

Record a play event.

**Endpoint:** `POST /api/history`

**Request Body:**
```json
{
  "track_id": 1,
  "duration": 180,
  "completed": true
}
```

**Response:** `201 Created`
```json
{
  "message": "play recorded"
}
```

### Get Play History

Get user's play history.

**Endpoint:** `GET /api/history`

**Query Parameters:**
- `limit` (optional): Number of results (default: 50, max: 200)

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "track_id": 5,
    "title": "Song Title",
    "artist": "Artist Name",
    "album": "Album Name",
    "played_at": "2024-01-15T10:30:00Z",
    "duration": 180,
    "completed": true
  }
]
```

### Get Statistics

Get user and system statistics.

**Endpoint:** `GET /api/stats`

**Response:** `200 OK`
```json
{
  "total_tracks": 1523,
  "total_playlists": 12,
  "total_plays": 456,
  "total_duration_seconds": 245678
}
```

---

## Error Responses

All endpoints may return these error responses:

### 400 Bad Request
```json
{
  "error": "invalid request body"
}
```

### 401 Unauthorized
```json
{
  "error": "missing authorization header"
}
```

### 403 Forbidden
```json
{
  "error": "not authorized to access this resource"
}
```

### 404 Not Found
```json
{
  "error": "resource not found"
}
```

### 500 Internal Server Error
```json
{
  "error": "internal server error"
}
```

---

## Rate Limiting

Currently no rate limiting is implemented. For production use, consider adding rate limiting middleware or using nginx's rate limiting features.

---

## CORS

CORS is configured to allow all origins in development. For production, update the CORS settings in `internal/api/router.go`:

```go
AllowedOrigins: []string{"https://yourdomain.com"},
```

---

## Webhooks (Future)

Webhook support is planned for future releases to notify external services about:
- New track uploads
- Playlist changes
- Play events

---

## Examples with curl

### Complete workflow example:

```bash
# 1. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "email": "john@example.com",
    "password": "password123",
    "display_name": "John"
  }'

# 2. Login (save token)
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"password123"}' \
  | jq -r '.token')

# 3. Upload track
curl -X POST http://localhost:8080/api/tracks/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@song.aac"

# 4. Create playlist
PLAYLIST_ID=$(curl -X POST http://localhost:8080/api/playlists \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"My Playlist","is_public":false}' \
  | jq -r '.id')

# 5. Add track to playlist
curl -X POST http://localhost:8080/api/playlists/$PLAYLIST_ID/tracks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"track_id":1}'

# 6. Get playlist with tracks
curl http://localhost:8080/api/playlists/$PLAYLIST_ID \
  -H "Authorization: Bearer $TOKEN"

# 7. Search
curl "http://localhost:8080/api/search?q=beatles" \
  -H "Authorization: Bearer $TOKEN"

# 8. Stream track (download)
curl http://localhost:8080/api/tracks/1/stream \
  -H "Authorization: Bearer $TOKEN" \
  -o downloaded_song.aac
```
