@echo off
echo ========================================
echo Music Streaming Backend - Quick Start
echo ========================================
echo.

REM Check if Docker is running
docker version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Docker is not running!
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo [1/4] Checking environment file...
if not exist .env (
    echo Creating .env from .env.example...
    copy .env.example .env
    echo.
    echo WARNING: Please edit .env and set a secure JWT_SECRET!
    echo Press any key to continue...
    pause >nul
)

echo [2/4] Starting Docker services...
docker-compose up -d

echo.
echo [3/4] Waiting for services to be ready (30 seconds)...
timeout /t 30 /nobreak >nul

echo.
echo [4/4] Checking health...
curl -s http://localhost:8080/health
echo.

echo.
echo ========================================
echo Services are running!
echo ========================================
echo.
echo API:          http://localhost:8080
echo MinIO:        http://localhost:9001
echo.
echo To view logs:     docker-compose logs -f music_backend
echo To stop:          docker-compose down
echo.
echo Next steps:
echo 1. Test the API (see TESTING.md)
echo 2. Upload some music files
echo 3. Configure nginx (see README.md)
echo.
pause
