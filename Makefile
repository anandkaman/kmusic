.PHONY: build run dev docker-build docker-up docker-down clean test

# Build the Go binary
build:
	go build -o musicstream ./cmd/server

# Run the application locally
run: build
	./musicstream

# Run with hot reload (requires air: go install github.com/cosmtrek/air@latest)
dev:
	air

# Build Docker image
docker-build:
	docker build -t musicstream:latest .

# Start all services with docker-compose
docker-up:
	docker-compose up -d

# Stop all services
docker-down:
	docker-compose down

# Stop all services and remove volumes
docker-clean:
	docker-compose down -v

# View logs
logs:
	docker-compose logs -f music_backend

# Clean build artifacts
clean:
	rm -f musicstream
	rm -rf vendor/

# Run tests
test:
	go test -v ./...

# Install dependencies
deps:
	go mod download
	go mod tidy

# Database migrations (manual)
migrate-up:
	@echo "Migrations are auto-run on startup"

# Format code
fmt:
	go fmt ./...

# Lint code (requires golangci-lint)
lint:
	golangci-lint run

# Generate go.sum
tidy:
	go mod tidy
