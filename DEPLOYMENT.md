# Production Deployment Guide

Complete guide for deploying the Music Streaming Backend to production on Linux.

## Server Requirements

### Minimum Specifications
- **CPU**: 2 cores (ARM or x86_64)
- **RAM**: 2GB minimum, 4GB recommended
- **Storage**: 20GB+ (depends on music library size)
- **OS**: Linux (Ubuntu 22.04, Debian 11, or similar)
- **Network**: Static IP or Dynamic DNS

### Software Prerequisites
- Docker 24.0+
- Docker Compose 2.0+
- Nginx (if you want reverse proxy)
- Optional: Certbot for HTTPS

## Initial Setup

### 1. Server Preparation

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo apt install docker-compose-plugin

# Logout and login to apply group changes
exit
```

### 2. Clone Project

```bash
# Create directory
mkdir -p /opt/musicstream
cd /opt/musicstream

# Copy all files to this directory
# (transfer via scp, git, or your preferred method)
```

### 3. Configure Environment

```bash
# Create .env from template
cp .env.example .env

# Edit .env with secure values
nano .env
```

**Critical settings to change:**
```env
# Generate a secure JWT secret (32+ characters)
JWT_SECRET=<generate-random-string-here>

# Set strong database password
DB_PASSWORD=<strong-password>

# Set MinIO credentials
MINIO_ACCESS_KEY=<random-access-key>
MINIO_SECRET_KEY=<long-random-secret>

# Production host
SERVER_HOST=0.0.0.0
```

**Generate secure secrets:**
```bash
# JWT Secret
openssl rand -base64 32

# Database password
openssl rand -base64 24

# MinIO credentials
openssl rand -base64 32
```

### 4. Update Docker Compose for Production

Edit [docker-compose.yml](docker-compose.yml):

```yaml
# Add restart policy to all services
restart: always

# Limit resources (optional)
deploy:
  resources:
    limits:
      cpus: '1.0'
      memory: 512M
    reservations:
      cpus: '0.5'
      memory: 256M
```

### 5. Start Services

```bash
# Start services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

## Nginx Configuration

### Basic Reverse Proxy

Create `/etc/nginx/sites-available/musicstream`:

```nginx
upstream musicstream_backend {
    server localhost:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name music.yourdomain.com;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header X-Content-Type-Options "nosniff" always;

    # Max upload size
    client_max_body_size 100M;
    client_body_timeout 300s;

    location / {
        proxy_pass http://musicstream_backend;
        proxy_http_version 1.1;

        # Connection headers
        proxy_set_header Connection "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Disable buffering for streaming
        proxy_buffering off;
        proxy_cache off;

        # Timeouts
        proxy_connect_timeout 300s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
    }
}
```

Enable site:
```bash
sudo ln -s /etc/nginx/sites-available/musicstream /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### HTTPS with Let's Encrypt

```bash
# Install Certbot
sudo apt install certbot python3-certbot-nginx

# Get certificate
sudo certbot --nginx -d music.yourdomain.com

# Auto-renewal is configured automatically
# Test renewal
sudo certbot renew --dry-run
```

Updated nginx config after HTTPS:
```nginx
server {
    listen 443 ssl http2;
    server_name music.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/music.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/music.yourdomain.com/privkey.pem;

    # SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # ... rest of configuration
}

# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name music.yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

## Firewall Configuration

```bash
# UFW (Ubuntu/Debian)
sudo ufw allow 22/tcp     # SSH
sudo ufw allow 80/tcp     # HTTP
sudo ufw allow 443/tcp    # HTTPS
sudo ufw enable

# Verify
sudo ufw status
```

## Systemd Service (Optional)

Create `/etc/systemd/system/musicstream.service`:

```ini
[Unit]
Description=Music Streaming Backend
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/musicstream
ExecStart=/usr/bin/docker-compose up -d
ExecStop=/usr/bin/docker-compose down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
```

Enable service:
```bash
sudo systemctl daemon-reload
sudo systemctl enable musicstream
sudo systemctl start musicstream
sudo systemctl status musicstream
```

## Backup Strategy

### 1. Database Backup Script

Create `/opt/musicstream/backup.sh`:

```bash
#!/bin/bash

BACKUP_DIR="/opt/musicstream/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup PostgreSQL
docker exec music_postgres pg_dump -U musicuser musicstream | \
  gzip > $BACKUP_DIR/db_$DATE.sql.gz

# Backup MinIO data (optional - files are large)
# docker exec music_minio mc mirror /data $BACKUP_DIR/minio_$DATE/

# Keep only last 30 days
find $BACKUP_DIR -name "db_*.sql.gz" -mtime +30 -delete

echo "Backup completed: $DATE"
```

Make executable and add to cron:
```bash
chmod +x /opt/musicstream/backup.sh

# Add to crontab (daily at 2 AM)
crontab -e
# Add line:
0 2 * * * /opt/musicstream/backup.sh >> /var/log/musicstream-backup.log 2>&1
```

### 2. Restore from Backup

```bash
# Restore database
gunzip -c /opt/musicstream/backups/db_20240115_020000.sql.gz | \
  docker exec -i music_postgres psql -U musicuser musicstream
```

## Monitoring

### Basic Monitoring Script

Create `/opt/musicstream/monitor.sh`:

```bash
#!/bin/bash

# Check if containers are running
if ! docker ps | grep -q "music_backend"; then
    echo "ERROR: music_backend is not running!"
    # Send alert (email, webhook, etc.)
    docker-compose -f /opt/musicstream/docker-compose.yml up -d music_backend
fi

# Check API health
if ! curl -sf http://localhost:8080/health > /dev/null; then
    echo "ERROR: API health check failed!"
    # Send alert
fi

# Check disk space
DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ $DISK_USAGE -gt 80 ]; then
    echo "WARNING: Disk usage is at ${DISK_USAGE}%"
    # Send alert
fi
```

Add to cron (every 5 minutes):
```bash
*/5 * * * * /opt/musicstream/monitor.sh >> /var/log/musicstream-monitor.log 2>&1
```

### Docker Stats

```bash
# Real-time stats
docker stats

# Set up persistent monitoring with Prometheus/Grafana (advanced)
```

## Performance Tuning

### 1. PostgreSQL

Create `/opt/musicstream/postgres-custom.conf`:

```conf
# Memory settings for 4GB RAM server
shared_buffers = 1GB
effective_cache_size = 3GB
maintenance_work_mem = 256MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200
work_mem = 10MB
min_wal_size = 1GB
max_wal_size = 4GB
```

Update docker-compose.yml:
```yaml
postgres:
  volumes:
    - ./postgres-custom.conf:/etc/postgresql/postgresql.conf
  command: postgres -c config_file=/etc/postgresql/postgresql.conf
```

### 2. Nginx

Add to nginx config:
```nginx
# Worker processes
worker_processes auto;
worker_connections 2048;

# Enable gzip
gzip on;
gzip_vary on;
gzip_types application/json;

# Buffer sizes
proxy_buffer_size 4k;
proxy_buffers 8 4k;
proxy_busy_buffers_size 8k;
```

### 3. Linux Kernel

Add to `/etc/sysctl.conf`:
```conf
# Increase max open files
fs.file-max = 65536

# Network tuning
net.core.somaxconn = 1024
net.ipv4.tcp_max_syn_backlog = 2048
```

Apply:
```bash
sudo sysctl -p
```

## Scaling

### Horizontal Scaling

For multiple backend instances behind load balancer:

```yaml
# docker-compose.yml
music_backend:
  deploy:
    replicas: 3
```

Update nginx upstream:
```nginx
upstream musicstream_backend {
    least_conn;
    server localhost:8080;
    server localhost:8081;
    server localhost:8082;
    keepalive 32;
}
```

### Database Optimization

```sql
-- Add indexes for frequently queried fields
CREATE INDEX CONCURRENTLY idx_tracks_artist_title ON tracks(artist, title);
CREATE INDEX CONCURRENTLY idx_tracks_album ON tracks(album);

-- Analyze tables
ANALYZE tracks;
ANALYZE playlists;
ANALYZE play_history;
```

## Security Hardening

### 1. Docker Security

```bash
# Run containers as non-root user
# Update Dockerfile to add:
# USER 1000:1000
```

### 2. Rate Limiting (Nginx)

```nginx
# Define rate limit zone
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;

# Apply to locations
location /api {
    limit_req zone=api_limit burst=20 nodelay;
    # ... rest of config
}
```

### 3. Fail2Ban

```bash
# Install
sudo apt install fail2ban

# Create filter for failed logins
sudo nano /etc/fail2ban/filter.d/musicstream.conf
```

```ini
[Definition]
failregex = invalid credentials.*from <HOST>
ignoreregex =
```

### 4. Database Security

```sql
-- Revoke unnecessary permissions
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO musicuser;

-- Enable SSL connections
-- Update postgresql.conf:
ssl = on
ssl_cert_file = '/path/to/server.crt'
ssl_key_file = '/path/to/server.key'
```

## Updates & Maintenance

### Update Application

```bash
# Pull latest code
cd /opt/musicstream
git pull  # or your update method

# Rebuild image
docker-compose build music_backend

# Restart with zero downtime (if using multiple replicas)
docker-compose up -d --no-deps --build music_backend
```

### Update Dependencies

```bash
# Update Go dependencies
docker-compose exec music_backend go get -u ./...
docker-compose exec music_backend go mod tidy

# Rebuild
docker-compose build music_backend
docker-compose up -d music_backend
```

### Database Migrations

For schema changes, update `internal/storage/postgres.go` Migrate() function and restart:

```bash
docker-compose restart music_backend
# Migrations run automatically on startup
```

## Troubleshooting

### Check Logs

```bash
# All logs
docker-compose logs -f

# Specific service
docker-compose logs -f music_backend

# Last 100 lines
docker-compose logs --tail=100 music_backend

# Save logs to file
docker-compose logs music_backend > debug.log
```

### Debug Mode

Add to docker-compose.yml:
```yaml
music_backend:
  environment:
    - DEBUG=true
```

### Database Issues

```bash
# Connect to database
docker exec -it music_postgres psql -U musicuser musicstream

# Check connections
SELECT * FROM pg_stat_activity;

# Check table sizes
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Performance Issues

```bash
# Check resource usage
docker stats

# Check slow queries (PostgreSQL)
docker exec music_postgres psql -U musicuser musicstream -c \
  "SELECT query, mean_exec_time FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;"
```

## Cost Optimization

### Storage Optimization

```bash
# Clean up old Docker images
docker image prune -a

# Clean up volumes (careful!)
docker volume prune

# Compress old database backups
gzip /opt/musicstream/backups/*.sql
```

### Minimal Resource Configuration

For very low-power servers:

```yaml
music_backend:
  deploy:
    resources:
      limits:
        cpus: '0.5'
        memory: 256M
```

## Disaster Recovery

### Full System Recovery

1. Fresh Linux installation
2. Install Docker
3. Restore backup files
4. Run:

```bash
cd /opt/musicstream
docker-compose up -d postgres redis minio

# Wait for databases to be ready
sleep 30

# Restore database
gunzip -c backups/db_latest.sql.gz | \
  docker exec -i music_postgres psql -U musicuser musicstream

# Start backend
docker-compose up -d music_backend
```

## Next Steps

After deployment:
1. ✓ Test all endpoints with production URL
2. ✓ Upload initial music library
3. ✓ Set up monitoring alerts
4. ✓ Configure backups
5. ✓ Document for other family members
6. ✓ Start building Android app

## Support & Resources

- [README.md](README.md) - Main documentation
- [API.md](API.md) - API reference
- [TESTING.md](TESTING.md) - Testing guide
- GitHub Issues - Report problems
