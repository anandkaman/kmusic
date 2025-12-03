# Setup and Deployment Checklist

Use this checklist to ensure everything is configured correctly.

## ✅ Pre-Deployment Checklist

### Development Environment
- [ ] Docker Desktop installed and running
- [ ] Git installed (optional, for version control)
- [ ] Text editor/IDE ready (VS Code recommended)
- [ ] Sample AAC music files ready for testing

### Initial Setup
- [ ] All project files present in `d:\streaming\`
- [ ] `.env` file created from `.env.example`
- [ ] JWT_SECRET changed to random 32+ character string
- [ ] Database passwords updated in `.env`
- [ ] MinIO credentials updated in `.env`

### First Run
- [ ] Run `docker-compose up -d`
- [ ] Wait 30 seconds for services to initialize
- [ ] Test health endpoint: `curl http://localhost:8080/health`
- [ ] Check all containers running: `docker-compose ps`
- [ ] Verify no errors in logs: `docker-compose logs`

### Testing
- [ ] Register a test user
- [ ] Login and receive JWT token
- [ ] Upload a test music file
- [ ] Stream the uploaded file
- [ ] Create a test playlist
- [ ] Add track to playlist
- [ ] Test search functionality
- [ ] Verify play history recording

## 🚀 Production Deployment Checklist

### Server Preparation
- [ ] Linux server ready (Ubuntu 22.04+ or Debian 11+)
- [ ] Docker installed on server
- [ ] Docker Compose installed
- [ ] Server has adequate storage for music library
- [ ] Server has static IP or DDNS configured
- [ ] SSH access configured

### Security Configuration
- [ ] Generate strong JWT secret: `openssl rand -base64 32`
- [ ] Generate strong database password: `openssl rand -base64 24`
- [ ] Generate strong MinIO credentials
- [ ] Update all secrets in `.env` file
- [ ] Disable default ports if exposing to internet
- [ ] Configure firewall (UFW/iptables)
  - [ ] Allow SSH (22)
  - [ ] Allow HTTP (80)
  - [ ] Allow HTTPS (443)
  - [ ] Block direct access to 5432, 6379, 9000

### File Transfer
- [ ] Copy all project files to `/opt/musicstream/`
- [ ] Set correct file permissions
- [ ] Verify `.env` file copied correctly
- [ ] Verify docker-compose.yml present

### Service Startup
- [ ] Start services: `docker-compose up -d`
- [ ] Check all containers healthy: `docker-compose ps`
- [ ] Test health endpoint from server
- [ ] Check logs for errors: `docker-compose logs -f`

### Nginx Configuration
- [ ] Nginx installed: `sudo apt install nginx`
- [ ] Create site configuration in `/etc/nginx/sites-available/`
- [ ] Enable site: `ln -s /etc/nginx/sites-available/musicstream /etc/nginx/sites-enabled/`
- [ ] Test configuration: `sudo nginx -t`
- [ ] Reload nginx: `sudo systemctl reload nginx`
- [ ] Test access through nginx

### HTTPS Setup
- [ ] Domain name configured and pointing to server
- [ ] Certbot installed: `sudo apt install certbot python3-certbot-nginx`
- [ ] Obtain certificate: `sudo certbot --nginx -d music.yourdomain.com`
- [ ] Test auto-renewal: `sudo certbot renew --dry-run`
- [ ] Verify HTTPS access works
- [ ] Verify HTTP redirects to HTTPS

### Database Optimization
- [ ] Verify migrations ran successfully
- [ ] Check all tables created: `docker exec -it music_postgres psql -U musicuser -d musicstream -c '\dt'`
- [ ] Verify indexes created
- [ ] Test database connection from backend

### Backup Configuration
- [ ] Create backup script at `/opt/musicstream/backup.sh`
- [ ] Make script executable: `chmod +x backup.sh`
- [ ] Create backup directory: `mkdir -p /opt/musicstream/backups`
- [ ] Test backup script manually
- [ ] Add to cron: `0 2 * * * /opt/musicstream/backup.sh`
- [ ] Verify backup runs and creates files

### Monitoring Setup
- [ ] Create monitoring script
- [ ] Add to cron (every 5 minutes)
- [ ] Set up log rotation
- [ ] Configure email/webhook alerts (optional)
- [ ] Test monitoring alerts

### Performance Tuning
- [ ] Adjust PostgreSQL settings for your server size
- [ ] Configure nginx worker processes
- [ ] Set appropriate resource limits in docker-compose.yml
- [ ] Enable nginx gzip compression
- [ ] Configure HTTP/2 in nginx

## 📱 Android App Development Checklist

### API Integration
- [ ] Retrofit setup complete
- [ ] API service interface defined
- [ ] Authentication interceptor implemented
- [ ] Token storage configured (EncryptedSharedPreferences)
- [ ] Network error handling implemented

### Player Implementation
- [ ] ExoPlayer dependency added
- [ ] Media session configured
- [ ] Streaming URL builder implemented
- [ ] Background playback working
- [ ] Notification controls working
- [ ] Lock screen controls working

### Core Features
- [ ] Login/Register screens
- [ ] Track listing with search
- [ ] Playlist management
- [ ] Now playing screen
- [ ] Queue management
- [ ] Offline mode (download support)

### UI/UX
- [ ] Material Design 3 implemented
- [ ] Dark/Light theme support
- [ ] Album art display
- [ ] Loading states
- [ ] Error states
- [ ] Empty states

### Testing
- [ ] Unit tests for repository layer
- [ ] Integration tests for API
- [ ] UI tests for critical flows
- [ ] Test on multiple devices
- [ ] Test on different Android versions

## 🔧 Maintenance Checklist (Monthly)

### Updates
- [ ] Check for Go dependency updates
- [ ] Update Docker images: `docker-compose pull`
- [ ] Rebuild services: `docker-compose up -d --build`
- [ ] Test all functionality after updates

### Backups
- [ ] Verify backups are running
- [ ] Test restore process
- [ ] Archive old backups
- [ ] Verify backup storage has space

### Monitoring
- [ ] Review logs for errors
- [ ] Check disk space: `df -h`
- [ ] Check database size
- [ ] Review play statistics
- [ ] Check resource usage: `docker stats`

### Security
- [ ] Review nginx access logs
- [ ] Check for failed login attempts
- [ ] Update SSL certificates if needed
- [ ] Review and rotate JWT secrets if needed

### Performance
- [ ] Check database query performance
- [ ] Review slow query logs
- [ ] Optimize indexes if needed
- [ ] Clean up old play history (if needed)

## 📊 Testing Checklist

### Functional Testing
- [ ] User registration works
- [ ] Login returns valid token
- [ ] Token authentication works
- [ ] File upload succeeds
- [ ] Metadata extraction correct
- [ ] Streaming works (full file)
- [ ] Range requests work (partial)
- [ ] Playlist CRUD operations
- [ ] Search returns results
- [ ] Play history records
- [ ] Statistics accurate
- [ ] Preferences save/load

### Error Testing
- [ ] Invalid credentials rejected
- [ ] Missing auth token returns 401
- [ ] Invalid token returns 401
- [ ] File too large rejected
- [ ] Non-existent resource returns 404
- [ ] Duplicate username rejected
- [ ] Invalid input validated

### Performance Testing
- [ ] Upload 10MB file completes in <30s
- [ ] API responds in <100ms
- [ ] Search responds in <200ms
- [ ] Concurrent streams work (10+ users)
- [ ] Memory usage stable under load
- [ ] No memory leaks after extended use

### Security Testing
- [ ] Passwords are hashed
- [ ] SQL injection prevented
- [ ] XSS attacks prevented
- [ ] CORS configured correctly
- [ ] HTTPS enforces TLS 1.2+
- [ ] Tokens expire correctly
- [ ] User can only access own resources

## 🎯 Go-Live Checklist

### Final Verification
- [ ] All services healthy
- [ ] SSL certificate valid
- [ ] Domain resolves correctly
- [ ] All features working
- [ ] Backups configured and tested
- [ ] Monitoring in place
- [ ] Documentation updated

### User Onboarding
- [ ] Admin account created
- [ ] Test user accounts created
- [ ] Sample playlists created
- [ ] User guide written (optional)
- [ ] Share access details with family

### Post-Launch
- [ ] Monitor logs for 24 hours
- [ ] Check for any errors
- [ ] Verify backups running
- [ ] Test from external network
- [ ] Collect user feedback

## 📝 Documentation Checklist

### Project Documentation
- [x] README.md complete
- [x] API.md complete
- [x] TESTING.md complete
- [x] DEPLOYMENT.md complete
- [x] QUICKSTART.md complete
- [x] This checklist

### Code Documentation
- [x] Main functions commented
- [x] Complex logic explained
- [x] API endpoints documented
- [x] Configuration options documented

### Operational Documentation
- [ ] Backup procedures documented
- [ ] Restore procedures tested and documented
- [ ] Troubleshooting guide created
- [ ] Contact information for support

## 🎉 Success Criteria

You've successfully deployed when:
- ✅ Backend responds to health checks
- ✅ Users can register and login
- ✅ Music files upload successfully
- ✅ Streaming works smoothly
- ✅ HTTPS is configured and working
- ✅ Backups are running automatically
- ✅ You can access from your Android app
- ✅ Multiple family members can use simultaneously
- ✅ No critical errors in logs

## 🚨 Red Flags (Stop and Fix)

Stop deployment if you see:
- ❌ Database connection errors
- ❌ MinIO upload failures
- ❌ Memory leaks (increasing memory usage)
- ❌ Constant CPU at 100%
- ❌ Disk space running out
- ❌ SSL certificate errors
- ❌ Authentication bypass possible
- ❌ Data loss on restart

## 📞 Support Resources

- Project README: [README.md](README.md)
- API Reference: [API.md](API.md)
- Testing Guide: [TESTING.md](TESTING.md)
- Deployment Guide: [DEPLOYMENT.md](DEPLOYMENT.md)
- Quick Start: [QUICKSTART.md](QUICKSTART.md)

## 🎓 Learning Path

1. ✅ Read QUICKSTART.md
2. ✅ Run locally with Docker
3. ✅ Test API with curl/Postman
4. ✅ Read API.md for Android integration
5. ✅ Deploy to production server
6. ✅ Configure HTTPS
7. ✅ Build Android app
8. ✅ Iterate and improve

---

**Print this checklist and check off items as you complete them!**

Good luck with your music streaming platform! 🎵
