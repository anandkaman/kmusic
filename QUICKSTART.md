# Quick Start Guide

Get your music streaming backend running in 5 minutes!

## Prerequisites

- Docker Desktop installed and running
- Windows/Linux/Mac with at least 2GB free RAM

## Step 1: Start Services (1 minute)

### On Windows:
```bash
# Double-click start.bat or run in PowerShell:
.\start.bat
```

### On Linux/Mac:
```bash
# Make the docker-compose.yml executable
chmod +x start.sh

# Or manually start:
docker-compose up -d

# Wait 30 seconds for services to initialize
sleep 30
```

## Step 2: Test It Works (1 minute)

```bash
# Check health
curl http://localhost:8080/health
```

Expected response:
```json
{"status":"ok","service":"music-streaming-api"}
```

## Step 3: Create Your First User (1 minute)

```bash
# Windows PowerShell:
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method POST -ContentType "application/json" -Body '{"username":"admin","email":"admin@music.local","password":"changeme123","display_name":"Admin"}'
$TOKEN = $response.token
echo $TOKEN

# Linux/Mac:
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@music.local","password":"changeme123","display_name":"Admin"}' \
  | jq -r '.token')
echo $TOKEN
```

**Save this token!** You'll need it for all API requests.

## Step 4: Upload Your First Song (2 minutes)

```bash
# Linux/Mac:
curl -X POST http://localhost:8080/api/tracks/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/your/song.aac"

# Windows PowerShell:
$headers = @{
    "Authorization" = "Bearer $TOKEN"
}
Invoke-RestMethod -Uri "http://localhost:8080/api/tracks/upload" `
  -Method POST `
  -Headers $headers `
  -Form @{file=Get-Item "C:\path\to\song.aac"}
```

## Step 5: Stream Your Music! (30 seconds)

### In a browser:
1. Get the track ID from the upload response (e.g., `"id": 1`)
2. Open: `http://localhost:8080/api/tracks/1/stream`
3. Add header: `Authorization: Bearer YOUR_TOKEN_HERE`

**Easier: Use a REST client like Postman or Insomnia**

### With curl:
```bash
# Download the track
curl http://localhost:8080/api/tracks/1/stream \
  -H "Authorization: Bearer $TOKEN" \
  -o downloaded.aac

# Play it with VLC, mpv, or your favorite player
```

## Common Commands

### View Logs
```bash
# Backend logs
docker-compose logs -f music_backend

# All services
docker-compose logs -f
```

### Stop Services
```bash
docker-compose down
```

### Restart Services
```bash
docker-compose restart music_backend
```

### Check Status
```bash
docker-compose ps
```

## Access Web Interfaces

- **MinIO Console**: http://localhost:9001
  - Username: `minioadmin`
  - Password: `minioadmin123`
  - Browse uploaded music files

## Quick API Reference

### Authentication
```bash
# Register
POST /api/auth/register
Body: {"username":"user","email":"user@example.com","password":"pass123","display_name":"Name"}

# Login
POST /api/auth/login
Body: {"username":"user","password":"pass123"}
```

### Tracks
```bash
# Upload
POST /api/tracks/upload
Header: Authorization: Bearer TOKEN
Body: multipart/form-data with "file" field

# List all
GET /api/tracks
Header: Authorization: Bearer TOKEN

# Stream
GET /api/tracks/{id}/stream
Header: Authorization: Bearer TOKEN

# Delete
DELETE /api/tracks/{id}
Header: Authorization: Bearer TOKEN
```

### Playlists
```bash
# Create
POST /api/playlists
Header: Authorization: Bearer TOKEN
Body: {"name":"My Playlist","description":"Description","is_public":false}

# List
GET /api/playlists
Header: Authorization: Bearer TOKEN

# Get with tracks
GET /api/playlists/{id}
Header: Authorization: Bearer TOKEN

# Add track
POST /api/playlists/{id}/tracks
Header: Authorization: Bearer TOKEN
Body: {"track_id":1}
```

### Search
```bash
# Search tracks
GET /api/search?q=query
Header: Authorization: Bearer TOKEN
```

## Testing with Postman

1. **Import as Collection:**
   - Base URL: `http://localhost:8080`
   - Create environment variable: `token`

2. **Register/Login:**
   - POST to `/api/auth/login`
   - Save response `token` to environment

3. **Set Authorization:**
   - Type: Bearer Token
   - Token: `{{token}}`

4. **Start Testing!**

## Troubleshooting

### "Connection refused"
```bash
# Check if Docker is running
docker ps

# Start services
docker-compose up -d

# Wait 30 seconds
```

### "Unauthorized"
- Check your token is valid
- Token format: `Authorization: Bearer YOUR_TOKEN`
- Tokens expire after 7 days (default)

### "File upload failed"
- Check file size (max 100MB)
- Verify file is audio format (AAC, MP3, FLAC)
- Check MinIO is running: `docker-compose ps`

### Database errors
```bash
# Restart PostgreSQL
docker-compose restart postgres

# Check logs
docker-compose logs postgres
```

## Environment URLs

### Development (default)
- Backend: `http://localhost:8080`
- MinIO: `http://localhost:9001`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`

### Production (with nginx)
- Backend: `https://music.yourdomain.com`

## Configuration

Edit `.env` file:
```env
# IMPORTANT: Change in production!
JWT_SECRET=your-secure-secret-key-here

# Database
DB_PASSWORD=secure-password

# File upload limit (bytes)
MAX_UPLOAD_SIZE=104857600  # 100MB
```

## Next Steps

1. **Read Full Documentation**
   - [README.md](README.md) - Complete overview
   - [API.md](API.md) - Detailed API docs
   - [TESTING.md](TESTING.md) - Test suite
   - [DEPLOYMENT.md](DEPLOYMENT.md) - Production deployment

2. **Secure Your Installation**
   - Change default passwords in `.env`
   - Generate strong JWT secret
   - Set up HTTPS with nginx

3. **Build Android App**
   - Use the API to build your client
   - ExoPlayer for streaming
   - Retrofit for API calls

4. **Customize**
   - Add more features
   - Adjust resource limits
   - Configure preferences

## Performance Tips

- **AAC files**: Most efficient format (good quality, small size)
- **Bitrate**: 128-256 kbps is perfect for streaming
- **Metadata**: Use proper ID3 tags for best experience

## Getting Help

- Check logs: `docker-compose logs -f`
- Read [TESTING.md](TESTING.md) for common scenarios
- Review [README.md](README.md) for detailed info

## Clean Up (Remove Everything)

```bash
# Stop and remove containers
docker-compose down

# Remove data volumes (WARNING: Deletes all music and data!)
docker-compose down -v

# Remove images
docker rmi musicstream:latest
```

---

**You're all set! 🎵**

Your music streaming backend is now running. Upload your music library and start enjoying your personal streaming service!

For Android app development, see the API endpoints above and use ExoPlayer with Retrofit for a smooth experience.
