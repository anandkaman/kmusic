# Build stage
FROM golang:1.22-alpine AS builder

# Install build dependencies
RUN apk add --no-cache git gcc musl-dev

WORKDIR /app

# Copy go mod file
COPY go.mod ./

# Copy source code (needed for go mod tidy)
COPY . .

# Generate go.sum and download dependencies
RUN go mod tidy && go mod download

# Build the application
RUN CGO_ENABLED=1 GOOS=linux go build -a -installsuffix cgo -ldflags="-w -s" -o musicstream ./cmd/server

# Runtime stage
FROM alpine:latest

# Install runtime dependencies
RUN apk --no-cache add ca-certificates ffmpeg tzdata

WORKDIR /root/

# Copy binary from builder
COPY --from=builder /app/musicstream .

# Create directory for music files
RUN mkdir -p /music_files

# Expose port
EXPOSE 8080

# Run the application
CMD ["./musicstream"]
