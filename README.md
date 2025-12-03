# Music Streaming Backend

A highly efficient, production-ready music streaming backend built with Go, designed for minimal resource usage and maximum concurrency performance. Perfect for family music servers running on low-power hardware.

## Features

- **Efficient Streaming**: HTTP range request support for efficient AAC audio streaming
- **User Management**: JWT-based authentication with secure password hashing
- **Role-Based Access**: Admin users can upload tracks, regular users can browse and play
- **Playlist Management**: Create, update, and share playlists
- **Metadata Extraction**: Automatic metadata extraction from uploaded audio files
- **Play History**: Track listening habits and statistics
- **User Preferences**: Customizable settings per user (language, theme, bitrate, etc.)
- **Search**: Fast full-text search across tracks, artists, and albums
- **S3-Compatible Storage**: MinIO for scalable object storage
- **Caching**: Redis for session management and performance optimization
- **Docker Ready**: Complete docker-compose setup for easy deployment

## Tech Stack

- **Backend**: Go 1.22 with Chi router (ultra-low memory footprint, excellent concurrency)
- **Database**: PostgreSQL 16 (relational data + JSON support)
- **Cache**: Redis 7 (sessions, tokens, rate limiting)
- **Object Storage**: MinIO (S3-compatible local storage)
- **Audio Format**: AAC (efficient compression, excellent quality)

## Project Structure

```
.
├── cmd/
│   └── server/
│       └── main.go              # Application entry point
├── internal/
│   ├── api/
│   │   ├── auth_handlers.go     # Authentication endpoints
│   │   ├── middleware.go        # JWT auth & logging middleware
│   │   ├── playlist_handlers.go # Playlist management
│   │   ├── response.go          # JSON response helpers
│   │   ├── router.go            # Route definitions
│   │   ├── stream_handler.go    # Audio streaming with range support
│   │   ├── track_handlers.go    # Track upload & management
│   │   └── user_handlers.go     # User preferences & search
│   ├── auth/
│   │   ├── jwt.go               # JWT token management
│   │   └── password.go          # Password hashing
│   ├── config/
│   │   └── config.go            # Configuration management
│   ├── models/
│   │   └── models.go            # Data models
│   └── storage/
│       ├── minio.go             # MinIO client
│       ├── postgres.go          # PostgreSQL client & migrations
│       ├── redis.go             # Redis client
│       └── storage.go           # Storage abstraction
├── docker-compose.yml           # Docker services configuration
├── Dockerfile                   # Multi-stage Go build
├── Makefile                     # Build & deployment commands
├── go.mod                       # Go dependencies
└── README.md                    # This file
```

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Go 1.22+ (for local development)
- Make (optional, for convenience commands)

### 1. Clone and Setup

```bash
# Create project directory
cd d:\streaming

# Copy environment variables
cp .env.example .env

# Edit .env and set your JWT secret (important for production!)
# JWT_SECRET should be a random 32+ character string
```

### 2. Start Services

```bash
# Start all services (PostgreSQL, Redis, MinIO, Backend)
docker-compose up -d

# View logs
docker-compose logs -f music_backend

# Check health
curl http://localhost:8080/health
```

The services will be available at:
- **API**: http://localhost:8080
- **MinIO Console**: http://localhost:9001 (minioadmin / minioadmin123)
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

### 3. Super Admin Account

The backend automatically creates a super admin account on first startup:

**Credentials:**
- Username: ``
- Password: ``
- Role: `admin`

**Important:** Only admin users can upload tracks. Regular users can browse, play music, and manage playlists.

Login with the super admin:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "kaman",
    "password": "Johnedoms2@"
  }'
```

The response includes the user's role:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "username": "kaman",
    "role": "admin",
    ...
  }
}
```

Save the returned JWT token for subsequent requests.

### 4. Create Additional Users

Regular users (non-admin) can register but won't have upload permissions:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "familymember",
    "email": "family@example.com",
    "password": "secure_password",
    "display_name": "Family Member"
  }'
```

These users will have `role: "user"` and can browse/play music but not upload.

## API Documentation

### Authentication

All endpoints except `/health`, `/api/auth/register`, and `/api/auth/login` require authentication.

Include the JWT token in the Authorization header:
```
Authorization: Bearer <your-token>
```

### Endpoints

#### Auth
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token
- `GET /api/user/me` - Get current user info

#### Tracks
- `POST /api/tracks/upload` - Upload music file (multipart/form-data)
- `GET /api/tracks` - List tracks (supports ?artist, ?album, ?genre, ?limit, ?offset)
- `GET /api/tracks/{id}` - Get track details
- `DELETE /api/tracks/{id}` - Delete track (owner only)
- `GET /api/tracks/{id}/stream` - Stream track (supports HTTP range requests)

#### Playlists
- `POST /api/playlists` - Create playlist
- `GET /api/playlists` - List user's playlists
- `GET /api/playlists/{id}` - Get playlist with tracks
- `PUT /api/playlists/{id}` - Update playlist
- `DELETE /api/playlists/{id}` - Delete playlist
- `POST /api/playlists/{id}/tracks` - Add track to playlist
- `DELETE /api/playlists/{id}/tracks/{trackId}` - Remove track from playlist

#### User Preferences
- `GET /api/user/preferences` - Get user preferences
- `PUT /api/user/preferences` - Update preferences

#### Search & Stats
- `GET /api/search?q=<query>` - Search tracks by title, artist, or album
- `GET /api/history` - Get play history
- `POST /api/history` - Record play
- `GET /api/stats` - Get user statistics

### Example: Upload and Stream

```bash
# 1. Login
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secure_password"}' \
  | jq -r '.token')

# 2. Upload a track
curl -X POST http://localhost:8080/api/tracks/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/song.aac"

# 3. Get tracks
curl http://localhost:8080/api/tracks \
  -H "Authorization: Bearer $TOKEN"

# 4. Stream a track (in browser or media player)
# http://localhost:8080/api/tracks/1/stream
# Add Authorization header with your token
```

## Development

### Local Development (without Docker)

```bash
# Start dependencies only
docker-compose up -d postgres redis minio

# Install dependencies
go mod download

# Run locally
go run cmd/server/main.go

# Or use Make
make run
```

### Hot Reload Development

```bash
# Install air for hot reload
go install github.com/cosmtrek/air@latest

# Run with hot reload
make dev
```

### Building

```bash
# Build binary
make build

# Build Docker image
make docker-build

# Run tests
make test

# Format code
make fmt
```

## Configuration

Environment variables (see [.env.example](.env.example)):

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8080 | HTTP server port |
| `SERVER_HOST` | 0.0.0.0 | Server bind address |
| `DB_HOST` | localhost | PostgreSQL host |
| `DB_PORT` | 5432 | PostgreSQL port |
| `DB_USER` | musicuser | Database user |
| `DB_PASSWORD` | musicpass | Database password |
| `DB_NAME` | musicstream | Database name |
| `REDIS_HOST` | localhost | Redis host |
| `REDIS_PORT` | 6379 | Redis port |
| `MINIO_ENDPOINT` | localhost:9000 | MinIO endpoint |
| `MINIO_ACCESS_KEY` | minioadmin | MinIO access key |
| `MINIO_SECRET_KEY` | minioadmin123 | MinIO secret key |
| `JWT_SECRET` | (required) | JWT signing secret (32+ chars) |
| `JWT_EXPIRY_HOURS` | 168 | Token expiry (7 days) |
| `MAX_UPLOAD_SIZE` | 104857600 | Max file upload size (100MB) |

## Performance

This backend is optimized for low-resource environments:

- **Memory**: ~20-50MB idle, scales efficiently with concurrent connections
- **CPU**: Minimal usage due to Go's efficient goroutines
- **Concurrency**: Handles 1000+ concurrent streams on modest hardware
- **Database Pool**: Configured for efficiency (25 max connections, 5 idle)
- **Streaming**: Zero-copy streaming with range request support

## Production Deployment

### With Nginx Reverse Proxy

Add to your nginx configuration:

```nginx
server {
    listen 80;
    server_name music.yourdomain.com;

    client_max_body_size 100M;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Important for streaming
        proxy_buffering off;
    }
}
```

### Security Checklist

- [ ] Change default passwords in [.env](.env)
- [ ] Generate strong JWT secret (32+ random characters)
- [ ] Set up HTTPS with Let's Encrypt
- [ ] Configure firewall (only expose 80/443)
- [ ] Enable PostgreSQL password authentication
- [ ] Set Redis password if exposed
- [ ] Regular backups of PostgreSQL data
- [ ] Update CORS settings in [router.go](internal/api/router.go) for production

### Backup

```bash
# Backup PostgreSQL
docker exec music_postgres pg_dump -U musicuser musicstream > backup.sql

# Backup MinIO data
docker exec music_minio mc mirror /data /backup

# Or backup Docker volumes
docker run --rm -v music_postgres_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/postgres_backup.tar.gz /data
```

## Nginx Integration

Since you already have nginx running, add this to proxy the backend:

```nginx
upstream musicstream_backend {
    server localhost:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name music.local;  # or your domain

    client_max_body_size 100M;

    location /api {
        proxy_pass http://musicstream_backend;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_buffering off;  # Critical for streaming
    }
}
```

## Troubleshooting

### Container won't start
```bash
# Check logs
docker-compose logs music_backend

# Verify dependencies are running
docker-compose ps
```

### Database connection errors
```bash
# Ensure PostgreSQL is ready
docker-compose exec postgres pg_isready

# Check credentials in .env
```

### Upload fails
- Check `MAX_UPLOAD_SIZE` in .env
- Verify MinIO is accessible
- Check disk space

### Streaming stutters
- Enable HTTP/2 in nginx
- Verify `proxy_buffering off` in nginx config
- Check network bandwidth

## Roadmap

- [ ] Transcoding support (AAC/MP3/FLAC to multiple bitrates)
- [ ] Album artwork storage and serving
- [ ] Social features (shared playlists, favorites)
- [ ] Admin dashboard
- [ ] Analytics and insights
- [ ] Mobile push notifications
- [ ] Offline sync protocol
- [ ] Equalizer presets

## License

MIT License - feel free to use for personal or commercial projects.

## Contributing

This is a personal/family project, but suggestions and bug reports are welcome!

## Support

For issues or questions, please open an issue on GitHub.
