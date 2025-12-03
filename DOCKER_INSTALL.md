# Docker Installation Guide

This guide will help you install and run the Music Streaming Backend using Docker on any server.

## Prerequisites

- Docker Engine 20.10 or higher
- Docker Compose 2.0 or higher
- Git (for cloning the repository)

### Install Docker (if not already installed)

**On Ubuntu/Debian:**
```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

**On CentOS/RHEL:**
```bash
sudo yum install -y docker docker-compose
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER
```

**Verify installation:**
```bash
docker --version
docker-compose --version
```

## Installation Steps

### 1. Clone the Repository

```bash
git clone <YOUR_REPOSITORY_URL>
cd streaming
```

### 2. Configure Environment (Optional)

The application uses default environment variables defined in `docker-compose.yml`. For custom configuration:

```bash
cp .env.example .env
nano .env  # Edit as needed
```

**Important:** For production, change the `JWT_SECRET` in `docker-compose.yml` to a secure random string.

### 3. Create Music Directory

```bash
mkdir -p music_files
```

### 4. Start the Application

```bash
# Start all services in detached mode
docker-compose up -d
```

This will start:
- PostgreSQL database (port 5432)
- Redis cache (port 6379)
- MinIO object storage (port 9000, console 9001)
- Music Streaming Backend (port 8080)

### 5. Verify Services are Running

```bash
# Check container status
docker-compose ps

# View logs
docker-compose logs -f music_backend
```

### 6. Access the Application

- **Backend API:** http://YOUR_SERVER_IP:8080
- **MinIO Console:** http://YOUR_SERVER_IP:9001
  - Username: `minioadmin`
  - Password: `minioadmin123`

## First-Time Setup

### Create MinIO Bucket

The application expects a bucket named `music-files`. You can create it via:

1. **MinIO Console UI:**
   - Go to http://YOUR_SERVER_IP:9001
   - Login with credentials above
   - Click "Buckets" → "Create Bucket"
   - Name: `music-files`
   - Click "Create"

2. **Or using MinIO Client:**
```bash
# Install mc (MinIO Client)
docker run --rm --network streaming_music_network minio/mc alias set myminio http://minio:9000 minioadmin minioadmin123
docker run --rm --network streaming_music_network minio/mc mb myminio/music-files
```

### Test the API

```bash
# Health check
curl http://localhost:8080/health

# Register a user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"admin@example.com","password":"admin123"}'
```

## Managing the Application

### Stop Services

```bash
docker-compose stop
```

### Start Services

```bash
docker-compose start
```

### Restart Services

```bash
docker-compose restart
```

### Stop and Remove Containers

```bash
docker-compose down
```

### Stop and Remove Everything (including data)

```bash
# WARNING: This will delete all data!
docker-compose down -v
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f music_backend
docker-compose logs -f postgres
docker-compose logs -f redis
docker-compose logs -f minio
```

### Update Application

```bash
# Pull latest changes
git pull

# Rebuild and restart
docker-compose down
docker-compose up -d --build
```

## Production Deployment

### Security Recommendations

1. **Change default passwords** in `docker-compose.yml`:
   - `POSTGRES_PASSWORD`
   - `MINIO_ROOT_PASSWORD`
   - `JWT_SECRET` (use a random 32+ character string)

2. **Use a reverse proxy** (nginx/Caddy) with SSL:
```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

3. **Firewall Configuration:**
```bash
# Allow only necessary ports
sudo ufw allow 22    # SSH
sudo ufw allow 80    # HTTP
sudo ufw allow 443   # HTTPS
sudo ufw enable
```

4. **Limit exposed ports** - Modify `docker-compose.yml` to not expose database ports externally:
```yaml
# Remove or comment out:
# ports:
#   - "5432:5432"  # PostgreSQL
#   - "6379:6379"  # Redis
```

### Backup Data

```bash
# Backup volumes
docker run --rm -v streaming_postgres_data:/data -v $(pwd):/backup ubuntu tar czf /backup/postgres_backup.tar.gz -C /data .
docker run --rm -v streaming_minio_data:/data -v $(pwd):/backup ubuntu tar czf /backup/minio_backup.tar.gz -C /data .

# Restore volumes
docker run --rm -v streaming_postgres_data:/data -v $(pwd):/backup ubuntu tar xzf /backup/postgres_backup.tar.gz -C /data
```

## Troubleshooting

### Container won't start

```bash
# Check logs
docker-compose logs music_backend

# Check if ports are already in use
sudo netstat -tulpn | grep -E '8080|5432|6379|9000'
```

### Permission Issues

```bash
# Fix music_files directory permissions
sudo chown -R $USER:$USER music_files
chmod -R 755 music_files
```

### Database Connection Issues

```bash
# Check if postgres is ready
docker-compose exec postgres pg_isready

# Restart services in order
docker-compose restart postgres
sleep 5
docker-compose restart music_backend
```

### Reset Everything

```bash
docker-compose down -v
docker-compose up -d
```

## Resource Requirements

**Minimum:**
- 1 CPU core
- 2 GB RAM
- 20 GB disk space

**Recommended:**
- 2+ CPU cores
- 4 GB RAM
- 50+ GB disk space (depending on music library size)

## Monitoring

### Check resource usage

```bash
docker stats
```

### Check disk usage

```bash
docker system df
```

## Support

For issues, refer to:
- API Documentation: `API.md`
- Architecture: `ARCHITECTURE.md`
- Testing Guide: `TESTING.md`
- Admin Guide: `ADMIN_GUIDE.md`
