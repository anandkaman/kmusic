# Admin Quick Reference

Quick guide for managing the music streaming backend as an administrator.

## Super Admin Login

```bash
Username: kaman
Password: Johnedoms2@
```

This account is created automatically on first startup and has upload permissions.

---

## Quick Commands

### Start the Backend

```bash
cd d:\streaming
docker-compose up -d
```

### View Logs

```bash
# All services
docker-compose logs -f

# Backend only
docker-compose logs -f music_backend

# Last 100 lines
docker-compose logs --tail=100 music_backend
```

### Stop the Backend

```bash
docker-compose down
```

### Restart After Changes

```bash
docker-compose restart music_backend
```

---

## Managing Music

### Upload Track (via API)

```bash
# Login first
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"kaman","password":"Johnedoms2@"}' \
  | jq -r '.token')

# Upload track
curl -X POST http://localhost:8080/api/tracks/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/song.aac"
```

### List All Tracks

```bash
curl http://localhost:8080/api/tracks \
  -H "Authorization: Bearer $TOKEN"
```

### Delete Track

```bash
# Get track ID from list
curl -X DELETE http://localhost:8080/api/tracks/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Search Tracks

```bash
curl "http://localhost:8080/api/search?q=beatles" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Managing Users

### List All Users (Database)

```bash
docker exec -it music_postgres psql -U musicuser -d musicstream -c \
  "SELECT id, username, email, role, created_at FROM users;"
```

### Make User an Admin

```bash
docker exec -it music_postgres psql -U musicuser -d musicstream -c \
  "UPDATE users SET role = 'admin' WHERE username = 'someuser';"
```

### View User Activity

```bash
# Play history for a user
docker exec -it music_postgres psql -U musicuser -d musicstream -c \
  "SELECT u.username, t.title, t.artist, ph.played_at
   FROM play_history ph
   JOIN users u ON ph.user_id = u.id
   JOIN tracks t ON ph.track_id = t.id
   ORDER BY ph.played_at DESC LIMIT 20;"
```

---

## Database Management

### Backup Database

```bash
# Create backup
docker exec music_postgres pg_dump -U musicuser musicstream > backup_$(date +%Y%m%d).sql

# Or with compression
docker exec music_postgres pg_dump -U musicuser musicstream | gzip > backup_$(date +%Y%m%d).sql.gz
```

### Restore Database

```bash
# From backup
cat backup_20240115.sql | docker exec -i music_postgres psql -U musicuser musicstream

# From compressed
gunzip -c backup_20240115.sql.gz | docker exec -i music_postgres psql -U musicuser musicstream
```

### View Database Size

```bash
docker exec -it music_postgres psql -U musicuser -d musicstream -c \
  "SELECT pg_size_pretty(pg_database_size('musicstream'));"
```

### Database Stats

```bash
docker exec -it music_postgres psql -U musicuser -d musicstream -c \
  "SELECT
     (SELECT COUNT(*) FROM users) as total_users,
     (SELECT COUNT(*) FROM tracks) as total_tracks,
     (SELECT COUNT(*) FROM playlists) as total_playlists,
     (SELECT COUNT(*) FROM play_history) as total_plays;"
```

---

## Storage Management

### Check MinIO Storage

Access MinIO Console:
- URL: http://localhost:9001
- Username: minioadmin
- Password: minioadmin123

### View Uploaded Files (MinIO CLI)

```bash
# Install MinIO client
docker exec music_minio mc alias set local http://localhost:9000 minioadmin minioadmin123

# List files
docker exec music_minio mc ls local/music-files
```

### Check Disk Usage

```bash
# Docker volumes
docker system df -v

# Specific volume
docker volume inspect music_minio_data
```

---

## Monitoring

### Check Service Health

```bash
# API health
curl http://localhost:8080/health

# All services status
docker-compose ps
```

### Resource Usage

```bash
# Real-time stats
docker stats

# Specific service
docker stats music_backend
```

### Check Database Connections

```bash
docker exec -it music_postgres psql -U musicuser -d musicstream -c \
  "SELECT count(*) FROM pg_stat_activity WHERE datname = 'musicstream';"
```

---

## Troubleshooting

### Backend Won't Start

```bash
# Check logs
docker-compose logs music_backend

# Check database connection
docker-compose exec music_backend ping postgres

# Restart fresh
docker-compose down
docker-compose up -d
```

### Upload Fails

```bash
# Check MinIO
docker-compose ps minio

# Check MinIO logs
docker-compose logs minio

# Check disk space
df -h
```

### Database Errors

```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Verify database exists
docker exec music_postgres psql -U musicuser -l

# Test connection
docker exec music_postgres psql -U musicuser -d musicstream -c "SELECT 1;"
```

### Can't Login as Admin

```bash
# Verify admin user exists
docker exec -it music_postgres psql -U musicuser -d musicstream -c \
  "SELECT username, role FROM users WHERE username = 'kaman';"

# If missing, restart backend (will recreate)
docker-compose restart music_backend
```

---

## Maintenance Tasks

### Weekly Tasks

1. **Check Logs for Errors**
   ```bash
   docker-compose logs --since 7d music_backend | grep -i error
   ```

2. **Backup Database**
   ```bash
   ./backup.sh  # If you created the backup script
   ```

3. **Check Disk Space**
   ```bash
   df -h
   docker system df
   ```

### Monthly Tasks

1. **Update Docker Images**
   ```bash
   docker-compose pull
   docker-compose up -d
   ```

2. **Clean Old Docker Resources**
   ```bash
   docker system prune -a
   ```

3. **Review User Activity**
   ```bash
   # See most active users
   docker exec -it music_postgres psql -U musicuser -d musicstream -c \
     "SELECT u.username, COUNT(*) as plays
      FROM play_history ph
      JOIN users u ON ph.user_id = u.id
      GROUP BY u.username
      ORDER BY plays DESC;"
   ```

---

## Security

### Change Admin Password

Edit [cmd/server/main.go](cmd/server/main.go) line 99:
```go
password := "NewSecurePassword123!"
```

Then rebuild and restart:
```bash
docker-compose build music_backend
docker-compose up -d music_backend
```

### View Failed Login Attempts

```bash
docker-compose logs music_backend | grep "invalid credentials"
```

### Enable HTTPS (Production)

See [DEPLOYMENT.md](DEPLOYMENT.md) for full guide.

---

## Configuration

### Environment Variables

Edit `.env` file:
```env
# IMPORTANT: Change these in production!
JWT_SECRET=your-random-32-char-secret
DB_PASSWORD=secure-database-password
MINIO_ACCESS_KEY=your-minio-access-key
MINIO_SECRET_KEY=your-minio-secret-key

# Adjust upload limit (in bytes)
MAX_UPLOAD_SIZE=104857600  # 100MB
```

After changes:
```bash
docker-compose down
docker-compose up -d
```

---

## Useful Queries

### Top 10 Most Played Tracks

```sql
docker exec -it music_postgres psql -U musicuser -d musicstream -c \
  "SELECT title, artist, play_count
   FROM tracks
   ORDER BY play_count DESC
   LIMIT 10;"
```

### Tracks by Upload Date

```sql
docker exec -it music_postgres psql -U musicuser -d musicstream -c \
  "SELECT title, artist, created_at
   FROM tracks
   ORDER BY created_at DESC
   LIMIT 20;"
```

### User Statistics

```sql
docker exec -it music_postgres psql -U musicuser -d musicstream -c \
  "SELECT
     u.username,
     COUNT(DISTINCT p.id) as playlists,
     COUNT(DISTINCT ph.id) as plays
   FROM users u
   LEFT JOIN playlists p ON u.id = p.user_id
   LEFT JOIN play_history ph ON u.id = ph.user_id
   GROUP BY u.username;"
```

---

## Emergency Procedures

### System is Slow

1. Check resource usage: `docker stats`
2. Check database: `docker exec -it music_postgres psql -U musicuser -d musicstream -c "SELECT * FROM pg_stat_activity;"`
3. Restart services: `docker-compose restart`

### Data Corruption

1. Stop services: `docker-compose down`
2. Restore from backup: `cat backup.sql | docker exec -i music_postgres psql -U musicuser musicstream`
3. Start services: `docker-compose up -d`

### Complete Reset (WARNING: Deletes All Data)

```bash
docker-compose down -v  # Removes volumes
docker-compose up -d    # Fresh start
```

---

## Contact & Support

- Documentation: [README.md](README.md)
- API Reference: [API.md](API.md)
- Testing Guide: [TESTING.md](TESTING.md)
- Deployment: [DEPLOYMENT.md](DEPLOYMENT.md)
- Android Integration: [ANDROID_INTEGRATION.md](ANDROID_INTEGRATION.md)

---

**Quick Help:**
- Health check: `curl http://localhost:8080/health`
- View logs: `docker-compose logs -f music_backend`
- Restart: `docker-compose restart music_backend`
- Backup: `docker exec music_postgres pg_dump -U musicuser musicstream > backup.sql`
