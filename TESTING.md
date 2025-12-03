# Testing Guide

Complete guide for testing the Music Streaming Backend.

## Prerequisites

- Docker and Docker Compose running
- curl or Postman installed
- Sample AAC audio files for testing

## Starting the Services

```bash
# Start all services
docker-compose up -d

# Wait for services to be ready (about 10-15 seconds)
sleep 15

# Check if backend is running
curl http://localhost:8080/health
```

Expected response:
```json
{
  "status": "ok",
  "service": "music-streaming-api"
}
```

## Test Suite

### 1. User Registration & Authentication

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "test123456",
    "display_name": "Test User"
  }'
```

**Expected:** 201 Created with token and user object

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "test123456"
  }'
```

**Expected:** 200 OK with token

**Save the token for subsequent tests:**
```bash
export TOKEN="your-token-here"
```

```bash
# Get current user
curl http://localhost:8080/api/user/me \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 200 OK with user details

### 2. Track Upload & Management

```bash
# Upload a track (replace with your AAC file path)
curl -X POST http://localhost:8080/api/tracks/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/your/song.aac"
```

**Expected:** 201 Created with track metadata

**Save the track ID:**
```bash
export TRACK_ID=1
```

```bash
# List all tracks
curl http://localhost:8080/api/tracks \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 200 OK with array of tracks

```bash
# Get specific track
curl http://localhost:8080/api/tracks/$TRACK_ID \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 200 OK with track details

```bash
# Filter tracks by artist
curl "http://localhost:8080/api/tracks?artist=Beatles" \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Audio Streaming

```bash
# Stream entire track
curl http://localhost:8080/api/tracks/$TRACK_ID/stream \
  -H "Authorization: Bearer $TOKEN" \
  -o downloaded_song.aac
```

**Expected:** 200 OK, file downloaded

```bash
# Stream with range request (first 1MB)
curl http://localhost:8080/api/tracks/$TRACK_ID/stream \
  -H "Authorization: Bearer $TOKEN" \
  -H "Range: bytes=0-1048575" \
  -o partial_song.aac
```

**Expected:** 206 Partial Content with Content-Range header

```bash
# Verify range support
curl -I http://localhost:8080/api/tracks/$TRACK_ID/stream \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** Accept-Ranges: bytes in headers

### 4. Playlist Management

```bash
# Create playlist
curl -X POST http://localhost:8080/api/playlists \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Playlist",
    "description": "My test playlist",
    "is_public": false
  }'
```

**Expected:** 201 Created with playlist object

**Save the playlist ID:**
```bash
export PLAYLIST_ID=1
```

```bash
# List playlists
curl http://localhost:8080/api/playlists \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 200 OK with array of playlists

```bash
# Add track to playlist
curl -X POST http://localhost:8080/api/playlists/$PLAYLIST_ID/tracks \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"track_id\": $TRACK_ID}"
```

**Expected:** 201 Created

```bash
# Get playlist with tracks
curl http://localhost:8080/api/playlists/$PLAYLIST_ID \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 200 OK with playlist and tracks array

```bash
# Update playlist
curl -X PUT http://localhost:8080/api/playlists/$PLAYLIST_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Playlist Name",
    "description": "Updated description",
    "is_public": true
  }'
```

**Expected:** 200 OK

```bash
# Remove track from playlist
curl -X DELETE http://localhost:8080/api/playlists/$PLAYLIST_ID/tracks/$TRACK_ID \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 200 OK

### 5. User Preferences

```bash
# Get preferences
curl http://localhost:8080/api/user/preferences \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 200 OK with preferences object

```bash
# Update preferences
curl -X PUT http://localhost:8080/api/user/preferences \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "language": "en",
    "theme": "dark",
    "bitrate_prefer": 320,
    "auto_play": true,
    "crossfade_duration": 5,
    "offline_mode": false,
    "notify_new_tracks": true
  }'
```

**Expected:** 200 OK

### 6. Search

```bash
# Search tracks
curl "http://localhost:8080/api/search?q=test" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 200 OK with matching tracks

### 7. Play History

```bash
# Record a play
curl -X POST http://localhost:8080/api/history \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"track_id\": $TRACK_ID,
    \"duration\": 180,
    \"completed\": true
  }"
```

**Expected:** 201 Created

```bash
# Get play history
curl http://localhost:8080/api/history \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 200 OK with play history array

### 8. Statistics

```bash
# Get stats
curl http://localhost:8080/api/stats \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 200 OK with statistics

### 9. Delete Operations

```bash
# Delete playlist
curl -X DELETE http://localhost:8080/api/playlists/$PLAYLIST_ID \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 200 OK

```bash
# Delete track (creates file in MinIO, then removes it)
curl -X DELETE http://localhost:8080/api/tracks/$TRACK_ID \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 200 OK

## Error Testing

### 1. Unauthorized Access

```bash
# Try to access protected endpoint without token
curl http://localhost:8080/api/tracks
```

**Expected:** 401 Unauthorized

### 2. Invalid Token

```bash
# Use invalid token
curl http://localhost:8080/api/tracks \
  -H "Authorization: Bearer invalid_token"
```

**Expected:** 401 Unauthorized

### 3. Invalid Credentials

```bash
# Login with wrong password
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "wrongpassword"
  }'
```

**Expected:** 401 Unauthorized

### 4. Not Found

```bash
# Get non-existent track
curl http://localhost:8080/api/tracks/99999 \
  -H "Authorization: Bearer $TOKEN"
```

**Expected:** 404 Not Found

### 5. Duplicate Username

```bash
# Try to register with existing username
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "another@example.com",
    "password": "test123456",
    "display_name": "Another User"
  }'
```

**Expected:** 409 Conflict

## Performance Testing

### Concurrent Streaming

```bash
# Test concurrent streams (requires siege or ab)
# Install: sudo apt-get install siege

# 100 concurrent requests
siege -c 100 -r 1 \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/tracks/$TRACK_ID/stream
```

### Upload Large Files

```bash
# Test max upload size (should be 100MB by default)
# Create a large file
dd if=/dev/zero of=large_file.aac bs=1M count=150

# Try to upload (should fail)
curl -X POST http://localhost:8080/api/tracks/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@large_file.aac"
```

**Expected:** 400 Bad Request (file too large)

## Database Testing

```bash
# Connect to PostgreSQL
docker exec -it music_postgres psql -U musicuser -d musicstream

# Check tables
\dt

# Count users
SELECT COUNT(*) FROM users;

# Check tracks
SELECT id, title, artist, play_count FROM tracks;

# Check playlists
SELECT * FROM playlists;

# Exit
\q
```

## MinIO Testing

Access MinIO Console:
- URL: http://localhost:9001
- Username: minioadmin
- Password: minioadmin123

Navigate to "music-files" bucket to see uploaded tracks.

## Redis Testing

```bash
# Connect to Redis
docker exec -it music_redis redis-cli

# Check keys (currently Redis is set up but not heavily used)
KEYS *

# Exit
exit
```

## Load Testing Script

Create a file `load_test.sh`:

```bash
#!/bin/bash

TOKEN=$1

if [ -z "$TOKEN" ]; then
  echo "Usage: ./load_test.sh <your-jwt-token>"
  exit 1
fi

echo "Starting load test..."

# Test 1: 100 GET requests to /api/tracks
echo "Test 1: GET /api/tracks (100 requests)"
ab -n 100 -c 10 \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/tracks

# Test 2: 50 requests to specific track
echo "Test 2: GET /api/tracks/1 (50 requests)"
ab -n 50 -c 5 \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/tracks/1

# Test 3: Search requests
echo "Test 3: Search (50 requests)"
ab -n 50 -c 5 \
  -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/search?q=test"

echo "Load test completed!"
```

Run with:
```bash
chmod +x load_test.sh
./load_test.sh $TOKEN
```

## Memory & CPU Monitoring

```bash
# Monitor backend container
docker stats music_backend

# Check logs for any errors
docker logs -f music_backend

# Check all containers
docker ps
```

## Cleanup After Testing

```bash
# Stop services
docker-compose down

# Remove volumes (deletes all data)
docker-compose down -v

# Remove test files
rm -f downloaded_song.aac partial_song.aac large_file.aac
```

## Automated Test Script

Create `test_all.sh`:

```bash
#!/bin/bash

set -e

BASE_URL="http://localhost:8080"

echo "🎵 Music Streaming Backend - Automated Test Suite"
echo "=================================================="

# Health check
echo "✓ Testing health endpoint..."
curl -s $BASE_URL/health | grep -q "ok" && echo "  Health check passed"

# Register
echo "✓ Registering user..."
REGISTER_RESPONSE=$(curl -s -X POST $BASE_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "autotest",
    "email": "autotest@example.com",
    "password": "test123456",
    "display_name": "Auto Test"
  }')

TOKEN=$(echo $REGISTER_RESPONSE | jq -r '.token')
echo "  Token received: ${TOKEN:0:20}..."

# Get current user
echo "✓ Testing get current user..."
curl -s $BASE_URL/api/user/me \
  -H "Authorization: Bearer $TOKEN" | grep -q "autotest" && echo "  User info retrieved"

# Get preferences
echo "✓ Testing preferences..."
curl -s $BASE_URL/api/user/preferences \
  -H "Authorization: Bearer $TOKEN" | grep -q "user_id" && echo "  Preferences retrieved"

# Create playlist
echo "✓ Creating playlist..."
PLAYLIST_RESPONSE=$(curl -s -X POST $BASE_URL/api/playlists \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Playlist",
    "description": "Auto test playlist",
    "is_public": false
  }')

PLAYLIST_ID=$(echo $PLAYLIST_RESPONSE | jq -r '.id')
echo "  Playlist created with ID: $PLAYLIST_ID"

# Get playlists
echo "✓ Testing get playlists..."
curl -s $BASE_URL/api/playlists \
  -H "Authorization: Bearer $TOKEN" | grep -q "Test Playlist" && echo "  Playlists retrieved"

# Search (should return empty or existing tracks)
echo "✓ Testing search..."
curl -s "$BASE_URL/api/search?q=test" \
  -H "Authorization: Bearer $TOKEN" > /dev/null && echo "  Search working"

# Stats
echo "✓ Testing stats..."
curl -s $BASE_URL/api/stats \
  -H "Authorization: Bearer $TOKEN" | grep -q "total_tracks" && echo "  Stats retrieved"

echo ""
echo "=================================================="
echo "✅ All tests passed!"
echo "Token for manual testing: $TOKEN"
```

Run with:
```bash
chmod +x test_all.sh
./test_all.sh
```

## Common Issues & Solutions

### 1. Connection Refused
- Check if services are running: `docker-compose ps`
- Wait longer for services to start: `sleep 30`

### 2. Database Connection Error
- Check PostgreSQL logs: `docker logs music_postgres`
- Verify credentials in .env file

### 3. MinIO Upload Fails
- Check MinIO logs: `docker logs music_minio`
- Verify bucket exists in MinIO console

### 4. 401 Unauthorized
- Verify token is not expired (default: 7 days)
- Check Authorization header format: `Bearer <token>`

### 5. File Upload Fails
- Check file size (max 100MB by default)
- Verify file is valid audio format
- Check available disk space

## Next Steps

After testing the backend, you can:
1. Configure nginx reverse proxy
2. Set up HTTPS with Let's Encrypt
3. Start building the Android app
4. Add more tracks and test with real music library
