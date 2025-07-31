#!/bin/bash

# Free Roulette Spin System Deployment Validation Script
# This script validates that the deployment meets all requirements

set -e

echo "🔍 Starting deployment validation..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0
WARNINGS=0

# Function to print test results
print_result() {
    local status=$1
    local message=$2
    
    case $status in
        "PASS")
            echo -e "${GREEN}✅ PASS${NC}: $message"
            ((PASSED++))
            ;;
        "FAIL")
            echo -e "${RED}❌ FAIL${NC}: $message"
            ((FAILED++))
            ;;
        "WARN")
            echo -e "${YELLOW}⚠️  WARN${NC}: $message"
            ((WARNINGS++))
            ;;
    esac
}

# Test 1: Docker and Docker Compose availability
echo ""
echo "🐳 Testing Docker environment..."
if command -v docker >/dev/null 2>&1; then
    print_result "PASS" "Docker is installed"
else
    print_result "FAIL" "Docker is not installed"
fi

if command -v docker-compose >/dev/null 2>&1; then
    print_result "PASS" "Docker Compose is installed"
else
    print_result "FAIL" "Docker Compose is not installed"
fi

if docker info >/dev/null 2>&1; then
    print_result "PASS" "Docker daemon is running"
else
    print_result "FAIL" "Docker daemon is not running"
fi

# Test 2: Required files exist
echo ""
echo "📁 Testing required files..."
required_files=(
    "Dockerfile"
    "docker-compose.yml"
    "docker-compose.override.yml"
    "docker-compose.prod.yml"
    ".env.example"
    "docker/init-db.sql"
    "docker/nginx.conf"
    "docker/prometheus.yml"
    "src/main/resources/application-docker.yml"
    "src/main/resources/application-prod.yml"
    "scripts/deploy.sh"
    "scripts/test-deployment.sh"
    "docs/DEPLOYMENT.md"
)

for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        print_result "PASS" "Required file exists: $file"
    else
        print_result "FAIL" "Required file missing: $file"
    fi
done

# Test 3: Configuration validation
echo ""
echo "⚙️ Testing configuration files..."

# Check if application.yml has required properties
if grep -q "spring:" src/main/resources/application.yml; then
    print_result "PASS" "Base application.yml has Spring configuration"
else
    print_result "FAIL" "Base application.yml missing Spring configuration"
fi

# Check Docker Compose file structure
if grep -q "roulette-api:" docker-compose.yml; then
    print_result "PASS" "Docker Compose has API service"
else
    print_result "FAIL" "Docker Compose missing API service"
fi

if grep -q "postgres:" docker-compose.yml; then
    print_result "PASS" "Docker Compose has PostgreSQL service"
else
    print_result "FAIL" "Docker Compose missing PostgreSQL service"
fi

if grep -q "redis:" docker-compose.yml; then
    print_result "PASS" "Docker Compose has Redis service"
else
    print_result "FAIL" "Docker Compose missing Redis service"
fi

# Test 4: Build validation
echo ""
echo "🔨 Testing build process..."
if mvn clean compile -q -B >/dev/null 2>&1; then
    print_result "PASS" "Maven build compilation successful"
else
    print_result "FAIL" "Maven build compilation failed"
fi

# Test 5: Container startup validation
echo ""
echo "🚀 Testing container startup..."

# Start containers in background
echo "Starting containers for validation..."
docker-compose up -d >/dev/null 2>&1

# Wait for startup
sleep 30

# Check if containers are running
containers=("roulette-api" "roulette-postgres" "roulette-redis")
for container in "${containers[@]}"; do
    if docker ps | grep -q "$container"; then
        print_result "PASS" "Container $container is running"
    else
        print_result "FAIL" "Container $container is not running"
    fi
done

# Test 6: Service health validation
echo ""
echo "🏥 Testing service health..."

# API health check
if curl -f -s http://localhost:8080/api/actuator/health >/dev/null 2>&1; then
    print_result "PASS" "API health endpoint responding"
else
    print_result "FAIL" "API health endpoint not responding"
fi

# Database health check
if docker exec roulette-postgres pg_isready -U casino_user >/dev/null 2>&1; then
    print_result "PASS" "PostgreSQL is ready"
else
    print_result "FAIL" "PostgreSQL is not ready"
fi

# Redis health check
if docker exec roulette-redis redis-cli ping >/dev/null 2>&1; then
    print_result "PASS" "Redis is responding"
else
    print_result "FAIL" "Redis is not responding"
fi

# Test 7: API endpoint validation
echo ""
echo "🔌 Testing API endpoints..."

# Test missions endpoint
if curl -f -s -H "X-User-ID: 12345" http://localhost:8080/api/missions >/dev/null 2>&1; then
    print_result "PASS" "Missions API endpoint working"
else
    print_result "FAIL" "Missions API endpoint not working"
fi

# Test roulette slots endpoint
if curl -f -s http://localhost:8080/api/roulette/slots >/dev/null 2>&1; then
    print_result "PASS" "Roulette slots API endpoint working"
else
    print_result "FAIL" "Roulette slots API endpoint not working"
fi

# Test 8: Database schema validation
echo ""
echo "🗄️ Testing database schema..."

# Check if tables exist
tables=("users" "deposit_missions" "user_mission_progress" "roulette_slots" "letter_collections" "letter_words" "transaction_logs")
for table in "${tables[@]}"; do
    if docker exec roulette-postgres psql -U casino_user -d casino_roulette -c "\dt $table" | grep -q "$table"; then
        print_result "PASS" "Database table exists: $table"
    else
        print_result "FAIL" "Database table missing: $table"
    fi
done

# Test 9: Default data validation
echo ""
echo "📊 Testing default data..."

# Check if default missions exist
mission_count=$(docker exec roulette-postgres psql -U casino_user -d casino_roulette -t -c "SELECT COUNT(*) FROM deposit_missions;" | tr -d ' ')
if [ "$mission_count" -gt 0 ]; then
    print_result "PASS" "Default missions data exists ($mission_count missions)"
else
    print_result "FAIL" "No default missions data found"
fi

# Check if default roulette slots exist
slot_count=$(docker exec roulette-postgres psql -U casino_user -d casino_roulette -t -c "SELECT COUNT(*) FROM roulette_slots;" | tr -d ' ')
if [ "$slot_count" -gt 0 ]; then
    print_result "PASS" "Default roulette slots data exists ($slot_count slots)"
else
    print_result "FAIL" "No default roulette slots data found"
fi

# Test 10: Security validation
echo ""
echo "🔒 Testing security configuration..."

# Check if non-root user is used in container
if docker exec roulette-api whoami | grep -q "casino"; then
    print_result "PASS" "Container runs as non-root user"
else
    print_result "WARN" "Container might be running as root user"
fi

# Check if health check is configured
if docker inspect roulette-api | grep -q "Healthcheck"; then
    print_result "PASS" "Container has health check configured"
else
    print_result "WARN" "Container health check not configured"
fi

# Test 11: Performance validation
echo ""
echo "⚡ Testing basic performance..."

# Measure API response time
start_time=$(date +%s%N)
curl -s -H "X-User-ID: 12345" http://localhost:8080/api/missions >/dev/null
end_time=$(date +%s%N)
response_time=$(( (end_time - start_time) / 1000000 ))

if [ "$response_time" -lt 1000 ]; then
    print_result "PASS" "API response time acceptable (${response_time}ms)"
elif [ "$response_time" -lt 3000 ]; then
    print_result "WARN" "API response time slow (${response_time}ms)"
else
    print_result "FAIL" "API response time too slow (${response_time}ms)"
fi

# Cleanup
echo ""
echo "🧹 Cleaning up test containers..."
docker-compose down >/dev/null 2>&1

# Final report
echo ""
echo "📋 Validation Summary:"
echo "================================"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${YELLOW}Warnings: $WARNINGS${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo "================================"

if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}🎉 All critical validations passed!${NC}"
    if [ "$WARNINGS" -gt 0 ]; then
        echo -e "${YELLOW}⚠️  Please review warnings above${NC}"
    fi
    exit 0
else
    echo -e "${RED}❌ $FAILED validation(s) failed. Please fix issues before deployment.${NC}"
    exit 1
fi