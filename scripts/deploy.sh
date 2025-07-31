#!/bin/bash

# Free Roulette Spin System Deployment Script
# Usage: ./scripts/deploy.sh [environment]
# Environments: dev, prod

set -e

ENVIRONMENT=${1:-dev}
PROJECT_NAME="free-roulette-spin-system"

echo "🚀 Starting deployment for environment: $ENVIRONMENT"

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo "📋 Checking prerequisites..."
if ! command_exists docker; then
    echo "❌ Docker is not installed"
    exit 1
fi

if ! command_exists docker-compose; then
    echo "❌ Docker Compose is not installed"
    exit 1
fi

# Check Docker daemon
if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker daemon is not running"
    exit 1
fi

echo "✅ Prerequisites check passed"

# Create necessary directories
echo "📁 Creating directories..."
mkdir -p logs
mkdir -p backups
mkdir -p docker/ssl

# Set environment-specific variables
case $ENVIRONMENT in
    "dev")
        COMPOSE_FILES="-f docker-compose.yml -f docker-compose.override.yml"
        ;;
    "prod")
        COMPOSE_FILES="-f docker-compose.yml -f docker-compose.prod.yml"
        if [ ! -f .env.prod ]; then
            echo "❌ .env.prod file not found. Please create it from .env.example"
            exit 1
        fi
        export $(cat .env.prod | grep -v '^#' | xargs)
        ;;
    *)
        echo "❌ Unknown environment: $ENVIRONMENT"
        echo "Available environments: dev, prod"
        exit 1
        ;;
esac

# Build application
echo "🔨 Building application..."
if [ "$ENVIRONMENT" = "prod" ]; then
    # Build with tests for production
    mvn clean package -B
else
    # Skip tests for development
    mvn clean package -DskipTests -B
fi

# Stop existing containers
echo "🛑 Stopping existing containers..."
docker-compose $COMPOSE_FILES down

# Pull latest images
echo "📥 Pulling latest images..."
docker-compose $COMPOSE_FILES pull

# Start services
echo "🚀 Starting services..."
docker-compose $COMPOSE_FILES up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 30

# Health checks
echo "🏥 Running health checks..."

# Check API health
echo "Checking API health..."
for i in {1..30}; do
    if curl -f http://localhost:8080/api/actuator/health >/dev/null 2>&1; then
        echo "✅ API is healthy"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ API health check failed"
        docker-compose $COMPOSE_FILES logs roulette-api
        exit 1
    fi
    sleep 2
done

# Check database health
echo "Checking database health..."
if docker exec roulette-postgres pg_isready -U casino_user >/dev/null 2>&1; then
    echo "✅ Database is healthy"
else
    echo "❌ Database health check failed"
    docker-compose $COMPOSE_FILES logs postgres
    exit 1
fi

# Check Redis health
echo "Checking Redis health..."
if docker exec roulette-redis redis-cli ping >/dev/null 2>&1; then
    echo "✅ Redis is healthy"
else
    echo "❌ Redis health check failed"
    docker-compose $COMPOSE_FILES logs redis
    exit 1
fi

# Run integration tests
if [ "$ENVIRONMENT" = "dev" ]; then
    echo "🧪 Running integration tests..."
    mvn test -Dtest="*IntegrationTest" -B
fi

# Display service status
echo "📊 Service status:"
docker-compose $COMPOSE_FILES ps

echo ""
echo "🎉 Deployment completed successfully!"
echo ""
echo "📍 Service URLs:"
echo "   API: http://localhost:8080/api"
echo "   API Health: http://localhost:8080/api/actuator/health"
echo "   API Docs: http://localhost:8080/api/swagger-ui.html"
if [ "$ENVIRONMENT" = "dev" ]; then
    echo "   Prometheus: http://localhost:9090"
    echo "   Grafana: http://localhost:3000 (admin/admin123)"
fi
echo ""
echo "📝 To view logs: docker-compose $COMPOSE_FILES logs -f"
echo "🛑 To stop: docker-compose $COMPOSE_FILES down"