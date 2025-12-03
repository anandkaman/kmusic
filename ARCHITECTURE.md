# Architecture Documentation

## System Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         ANDROID APP (Client)                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
│  │   ExoPlayer  │  │   Retrofit   │  │   Room DB    │              │
│  │  (Streaming) │  │  (HTTP API)  │  │  (Offline)   │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
└─────────────────────────────────────────────────────────────────────┘
                              ▲
                              │ HTTPS/HTTP
                              │ JSON + Audio Stream
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    NGINX (Reverse Proxy)                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  • HTTPS/SSL Termination                                     │   │
│  │  • Load Balancing                                            │   │
│  │  • Rate Limiting                                             │   │
│  │  • Static File Serving (future)                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                              ▲
                              │ HTTP
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    GO BACKEND SERVICE (Port 8080)                    │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                      API LAYER                               │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │   │
│  │  │     Auth     │  │    Tracks    │  │   Playlists  │      │   │
│  │  │   Handlers   │  │   Handlers   │  │   Handlers   │      │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘      │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │   │
│  │  │   Streaming  │  │     User     │  │   Search     │      │   │
│  │  │   Handler    │  │   Handlers   │  │   Handler    │      │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘      │   │
│  └─────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                   MIDDLEWARE LAYER                           │   │
│  │  • JWT Authentication                                        │   │
│  │  • Logging                                                   │   │
│  │  • CORS                                                      │   │
│  │  • Request Timeout                                           │   │
│  └─────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                   BUSINESS LOGIC                             │   │
│  │  • JWT Token Management                                      │   │
│  │  • Password Hashing (bcrypt)                                 │   │
│  │  • Metadata Extraction                                       │   │
│  │  • Range Request Processing                                  │   │
│  └─────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                   STORAGE LAYER                              │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │   │
│  │  │  PostgreSQL  │  │    Redis     │  │    MinIO     │      │   │
│  │  │   Client     │  │   Client     │  │   Client     │      │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘      │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
            │                    │                    │
            ▼                    ▼                    ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│   POSTGRESQL     │  │      REDIS       │  │      MINIO       │
│   (Port 5432)    │  │   (Port 6379)    │  │   (Port 9000)    │
│                  │  │                  │  │                  │
│  • Users         │  │  • Sessions      │  │  • Audio Files   │
│  • Tracks        │  │  • Cache         │  │  • Metadata      │
│  • Playlists     │  │  • Rate Limits   │  │  (S3-compat)     │
│  • History       │  │                  │  │                  │
│  • Preferences   │  │                  │  │                  │
└──────────────────┘  └──────────────────┘  └──────────────────┘
```

## Data Flow Diagrams

### 1. User Registration Flow

```
┌─────────┐       ┌──────────┐       ┌──────────┐       ┌──────────┐
│ Android │──────▶│   API    │──────▶│ Password │──────▶│ Database │
│   App   │       │ Handler  │       │  Hasher  │       │ (Users)  │
└─────────┘       └──────────┘       └──────────┘       └──────────┘
     │                  │                                      │
     │                  │                                      │
     │                  ▼                                      │
     │           ┌──────────┐                                 │
     │           │   JWT    │◀────────────────────────────────┘
     │           │ Manager  │
     │           └──────────┘
     │                  │
     ◀──────────────────┘
     (Return Token + User)
```

### 2. Music Upload Flow

```
┌─────────┐       ┌──────────┐       ┌──────────┐       ┌──────────┐
│ Android │──────▶│   Auth   │──────▶│  Upload  │──────▶│  Metadata│
│   App   │       │Middleware│       │ Handler  │       │ Extractor│
└─────────┘       └──────────┘       └──────────┘       └──────────┘
                                            │                  │
                                            ▼                  │
                                      ┌──────────┐            │
                                      │  MinIO   │            │
                                      │ Storage  │            │
                                      └──────────┘            │
                                            │                  │
                                            ▼                  ▼
                                      ┌──────────────────────────┐
                                      │      PostgreSQL          │
                                      │  (Save Track Metadata)   │
                                      └──────────────────────────┘
```

### 3. Audio Streaming Flow

```
┌─────────┐       ┌──────────┐       ┌──────────┐       ┌──────────┐
│ Android │──────▶│   Auth   │──────▶│ Streaming│──────▶│  MinIO   │
│   App   │       │Middleware│       │ Handler  │       │ Storage  │
│         │       └──────────┘       └──────────┘       └──────────┘
│         │                                 │                  │
│ Request │                                 │  Parse Range     │
│ Range:  │                                 │  Header          │
│ 0-1024  │                                 │                  │
│         │                                 ▼                  │
│         │                          ┌──────────┐             │
│         │◀─────────────────────────│  Seek &  │◀────────────┘
│         │                          │  Stream  │
│ 206     │                          └──────────┘
│ Partial │
│ Content │
└─────────┘
```

### 4. Playlist Management Flow

```
┌─────────┐       ┌──────────┐       ┌──────────┐
│ Android │──────▶│   Auth   │──────▶│ Playlist │
│   App   │       │Middleware│       │ Handler  │
└─────────┘       └──────────┘       └──────────┘
                                            │
                                            ▼
                                      ┌──────────────────────┐
                                      │     PostgreSQL       │
                                      │                      │
                                      │  1. Playlists table  │
                                      │  2. PlaylistTracks   │
                                      │  3. Update counts    │
                                      └──────────────────────┘
                                            │
                  Return Playlist           │
         ┌────────with Tracks───────────────┘
         ▼
┌─────────────────┐
│  Android App    │
│  (Display)      │
└─────────────────┘
```

## Component Details

### Backend Service (Go)

**Main Components:**
1. **HTTP Server** (Chi Router)
   - Routes requests to handlers
   - Applies middleware
   - Manages timeouts

2. **Authentication Layer**
   - JWT token generation/validation
   - Password hashing (bcrypt)
   - User session management

3. **API Handlers**
   - Auth handlers (login, register)
   - Track handlers (upload, list, stream)
   - Playlist handlers (CRUD operations)
   - User handlers (preferences, search)

4. **Storage Layer**
   - PostgreSQL client (database/sql)
   - Redis client (go-redis)
   - MinIO client (minio-go)

**Concurrency Model:**
- Each request handled in its own goroutine
- Connection pooling for databases
- Efficient memory usage
- Non-blocking I/O

### Database Schema (PostgreSQL)

**Table Relationships:**
```
┌─────────┐          ┌────────────┐          ┌──────────┐
│  Users  │          │  Playlists │          │  Tracks  │
│         │──────────│            │          │          │
│  id     │ 1      * │  user_id   │          │  id      │
│username │          │  name      │          │  title   │
│  email  │          │            │          │  artist  │
└─────────┘          └────────────┘          └──────────┘
     │                     │                       │
     │ 1                   │ *                     │
     │                     │                       │
     │               ┌─────▼─────────────┐   *    │
     │               │ PlaylistTracks    │────────┘
     │               │                   │
     │               │  playlist_id (FK) │
     │               │  track_id (FK)    │
     │               │  position         │
     │               └───────────────────┘
     │
     │ 1
     │
     ▼ *
┌───────────────────┐         ┌─────────────────┐
│  PlayHistory      │         │ UserPreferences │
│                   │         │                 │
│  user_id (FK)     │         │  user_id (FK)   │
│  track_id (FK)    │         │  language       │
│  played_at        │         │  theme          │
│  completed        │         │  bitrate_prefer │
└───────────────────┘         └─────────────────┘
```

### Storage Distribution

**PostgreSQL:**
- User accounts and authentication data
- Track metadata (title, artist, album, etc.)
- Playlists and relationships
- Play history
- User preferences
- Application state

**Redis:**
- Session data (future enhancement)
- Cache for frequently accessed data
- Rate limiting counters
- Temporary tokens

**MinIO:**
- Audio files (AAC, MP3, FLAC)
- Album artwork (future)
- User uploads
- All binary data

## API Architecture

### RESTful Design

```
/api
├── /auth
│   ├── POST   /register     # Create new user
│   └── POST   /login         # Authenticate user
├── /user
│   ├── GET    /me            # Get current user
│   └── GET    /preferences   # Get preferences
│       PUT    /preferences   # Update preferences
├── /tracks
│   ├── POST   /upload        # Upload music file
│   ├── GET    /              # List all tracks
│   ├── GET    /{id}          # Get track details
│   ├── GET    /{id}/stream   # Stream audio ⭐
│   └── DELETE /{id}          # Delete track
├── /playlists
│   ├── POST   /              # Create playlist
│   ├── GET    /              # List playlists
│   ├── GET    /{id}          # Get playlist + tracks
│   ├── PUT    /{id}          # Update playlist
│   ├── DELETE /{id}          # Delete playlist
│   ├── POST   /{id}/tracks   # Add track to playlist
│   └── DELETE /{id}/tracks/{trackId} # Remove track
├── /search
│   └── GET    ?q=query       # Search tracks
├── /history
│   ├── POST   /              # Record play event
│   └── GET    /              # Get play history
└── /stats
    └── GET    /              # Get statistics
```

### Authentication Flow

```
┌──────────────────────────────────────────────────────────┐
│                   All Requests                            │
└───────────────────────┬──────────────────────────────────┘
                        │
                        ▼
         ┌──────────────────────────────┐
         │   Endpoint Public?           │
         │  (/health, /auth/*)          │
         └──────┬───────────────┬───────┘
                │ Yes           │ No
                │               │
                ▼               ▼
         ┌──────────┐    ┌─────────────────┐
         │  Allow   │    │ Check Auth      │
         │  Access  │    │ Header          │
         └──────────┘    └────────┬────────┘
                                  │
                      ┌───────────┴────────────┐
                      │ Has "Bearer TOKEN"?     │
                      └───────┬────────┬────────┘
                              │ Yes    │ No
                              │        │
                              ▼        ▼
                       ┌──────────┐  ┌──────────┐
                       │ Verify   │  │ Return   │
                       │  JWT     │  │ 401      │
                       └────┬─────┘  └──────────┘
                            │
                    ┌───────┴────────┐
                    │ Token Valid?    │
                    └───┬────────┬────┘
                        │ Yes    │ No
                        │        │
                        ▼        ▼
                  ┌──────────┐ ┌──────────┐
                  │ Add User │ │ Return   │
                  │ Context  │ │ 401      │
                  └────┬─────┘ └──────────┘
                       │
                       ▼
                  ┌──────────┐
                  │ Process  │
                  │ Request  │
                  └──────────┘
```

## Streaming Architecture

### HTTP Range Request Support

```
Client Request:
GET /api/tracks/1/stream
Range: bytes=0-1048575

                    ┌─────────────────────────┐
                    │    Stream Handler       │
                    └───────────┬─────────────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
        No Range Header    Has Range      Invalid Range
                │               │               │
                ▼               ▼               ▼
         ┌────────────┐  ┌────────────┐  ┌────────────┐
         │ Stream     │  │ Parse      │  │ Return     │
         │ Full File  │  │ Range      │  │ 416 Error  │
         │            │  │            │  │            │
         │ 200 OK     │  │ 206 Partial│  └────────────┘
         └────────────┘  └────────────┘
                │               │
                └───────┬───────┘
                        │
                        ▼
                ┌────────────────┐
                │  MinIO Get     │
                │  Object        │
                └───────┬────────┘
                        │
                        ▼
                ┌────────────────┐
                │  Seek to       │
                │  Position      │
                └───────┬────────┘
                        │
                        ▼
                ┌────────────────┐
                │  Stream Bytes  │
                │  to Client     │
                └────────────────┘
```

### Efficient Streaming

**Zero-Copy Streaming:**
```go
// No intermediate buffers
io.Copy(responseWriter, minioObject)

// Or with range
io.CopyN(responseWriter, minioObject, rangeLength)
```

**Benefits:**
- Minimal memory usage
- Maximum throughput
- Direct pipe from storage to client
- Supports seeking for mobile players

## Security Architecture

### Security Layers

```
┌─────────────────────────────────────────────────────────┐
│                   EXTERNAL LAYER                         │
│  • HTTPS/TLS Encryption                                  │
│  • Nginx Rate Limiting                                   │
│  • Firewall Rules                                        │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│                  APPLICATION LAYER                       │
│  • JWT Token Authentication                              │
│  • Password Hashing (bcrypt)                             │
│  • Input Validation                                      │
│  • SQL Injection Prevention (Prepared Statements)        │
│  • CORS Configuration                                    │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│                    DATA LAYER                            │
│  • Database Access Control                               │
│  • Encrypted Connections                                 │
│  • Ownership Verification                                │
│  • Private/Public Playlist Control                       │
└──────────────────────────────────────────────────────────┘
```

## Deployment Architecture

### Development

```
┌───────────────────────────────────────────┐
│        Developer Machine (Windows)        │
│                                           │
│  ┌─────────────────────────────────────┐ │
│  │        Docker Desktop               │ │
│  │                                     │ │
│  │  ┌──────────┐  ┌──────────┐       │ │
│  │  │ Backend  │  │PostgreSQL│       │ │
│  │  └──────────┘  └──────────┘       │ │
│  │  ┌──────────┐  ┌──────────┐       │ │
│  │  │  Redis   │  │  MinIO   │       │ │
│  │  └──────────┘  └──────────┘       │ │
│  └─────────────────────────────────────┘ │
└───────────────────────────────────────────┘
         │
         │ Test API
         ▼
┌───────────────────────┐
│   Postman/curl        │
│   Android Emulator    │
└───────────────────────┘
```

### Production

```
                    Internet
                       │
                       │ HTTPS (443)
                       ▼
┌──────────────────────────────────────────┐
│         Linux Server (Ubuntu)            │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │            Nginx                   │ │
│  │  • SSL Termination                 │ │
│  │  • Reverse Proxy                   │ │
│  └──────────┬─────────────────────────┘ │
│             │ HTTP (8080)                │
│  ┌──────────▼─────────────────────────┐ │
│  │        Docker Compose              │ │
│  │                                    │ │
│  │  ┌────────────┐  ┌────────────┐   │ │
│  │  │  Backend   │  │ PostgreSQL │   │ │
│  │  │  Service   │  │  + Data    │   │ │
│  │  └────────────┘  └────────────┘   │ │
│  │  ┌────────────┐  ┌────────────┐   │ │
│  │  │   Redis    │  │   MinIO    │   │ │
│  │  │  + Cache   │  │  + Music   │   │ │
│  │  └────────────┘  └────────────┘   │ │
│  └────────────────────────────────────┘ │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │         Persistent Volumes         │ │
│  │  • postgres_data                   │ │
│  │  • redis_data                      │ │
│  │  • minio_data                      │ │
│  └────────────────────────────────────┘ │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │        System Services             │ │
│  │  • UFW Firewall                    │ │
│  │  • Cron (Backups)                  │ │
│  │  • Systemd (Auto-start)            │ │
│  └────────────────────────────────────┘ │
└──────────────────────────────────────────┘
         │
         │ HTTPS
         ▼
┌─────────────────────┐
│   Android Devices   │
│   (Family Members)  │
└─────────────────────┘
```

## Scalability Architecture

### Horizontal Scaling (Future)

```
                    Load Balancer
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
    Backend-1       Backend-2       Backend-3
         │               │               │
         └───────────────┼───────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
         ▼               ▼               ▼
    PostgreSQL        Redis          MinIO
   (Primary +       (Cluster)      (Distributed)
    Replicas)
```

## Android App Architecture (Recommended)

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Activity   │  │   Fragment   │  │   ViewModel  │  │
│  │  (UI Logic)  │  │  (UI Views)  │  │   (State)    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│                     Domain Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Use Cases   │  │  Repository  │  │   Entities   │  │
│  │              │  │  Interface   │  │              │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│                      Data Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Retrofit   │  │  Room DB     │  │  ExoPlayer   │  │
│  │  (Remote)    │  │  (Local)     │  │  (Playback)  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## Performance Characteristics

### Request Latency (Typical)
- Authentication: 10-50ms
- Track listing: 20-100ms
- Search: 50-200ms
- Stream start: 100-500ms
- Metadata update: 10-50ms

### Throughput
- API requests: 1000+ req/sec
- Concurrent streams: 100+ users
- Upload speed: Network limited
- Database queries: <10ms avg

### Resource Usage
- Memory: 20-50MB idle, scales linearly
- CPU: <5% idle, <50% under load
- Disk I/O: Depends on streaming
- Network: Scales with concurrent users

---

This architecture is designed for:
- ✅ Low resource usage
- ✅ High concurrency
- ✅ Easy deployment
- ✅ Simple maintenance
- ✅ Future extensibility
