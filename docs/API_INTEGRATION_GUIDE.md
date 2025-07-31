# Free Roulette Spin System - API Integration Guide

## Overview

The Free Roulette Spin System API provides a comprehensive set of endpoints for managing user rewards, roulette gameplay, and letter collection mechanics in a casino platform. This guide covers integration requirements, authentication, error handling, and usage examples.

## Table of Contents

1. [Authentication](#authentication)
2. [Base URLs](#base-urls)
3. [Request/Response Format](#requestresponse-format)
4. [Error Handling](#error-handling)
5. [Rate Limiting](#rate-limiting)
6. [API Endpoints](#api-endpoints)
7. [Integration Examples](#integration-examples)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)

## Authentication

### User ID Header

All API endpoints require authentication via the `X-User-Id` header:

```http
X-User-Id: 12345
```

**Requirements:**
- Must be a positive integer
- Must correspond to a valid user in the main casino system
- Required for all endpoints

**Example:**
```bash
curl -X GET "http://localhost:8080/api/missions" \
  -H "X-User-Id: 12345" \
  -H "Content-Type: application/json"
```

## Base URLs

| Environment | Base URL |
|-------------|----------|
| Development | `http://localhost:8080/api` |
| Staging | `https://staging-api.casino.com/roulette` |
| Production | `https://api.casino.com/roulette` |

## Request/Response Format

### Content Type
All requests and responses use `application/json` content type.

### Standard Response Format

**Success Response:**
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... }
}
```

**Error Response:**
```json
{
  "success": false,
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Error Handling

### HTTP Status Codes

| Status Code | Description |
|-------------|-------------|
| 200 | Success |
| 400 | Bad Request - Invalid input parameters |
| 401 | Unauthorized - Missing or invalid authentication |
| 422 | Unprocessable Entity - Business logic error |
| 500 | Internal Server Error |

### Common Error Codes

| Error Code | Description | HTTP Status |
|------------|-------------|-------------|
| `INVALID_USER_ID` | User ID is invalid or missing | 400 |
| `INSUFFICIENT_SPINS` | User has no available spins | 400 |
| `MISSION_NOT_AVAILABLE` | Mission cannot be claimed | 422 |
| `INSUFFICIENT_LETTERS` | Not enough letters for word bonus | 422 |
| `INVALID_DEPOSIT_AMOUNT` | Deposit amount is invalid | 400 |
| `INTERNAL_ERROR` | Unexpected server error | 500 |

## Rate Limiting

Some endpoints have rate limiting to prevent abuse:

- **Roulette Spin**: 10 requests per minute per user
- **Mission Claim**: 5 requests per minute per user
- **Deposit Processing**: 3 requests per minute per user

Rate limit headers are included in responses:
```http
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 1642248000
```

## API Endpoints

### Missions

#### Get Available Missions
```http
GET /api/missions
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "Small Deposit Mission",
    "description": "Deposit $50-$99 to earn 1 free spin",
    "spinsAvailable": 1,
    "canClaim": true,
    "claimsUsed": 5,
    "maxClaims": 50
  }
]
```

#### Claim Mission Reward
```http
POST /api/missions/{missionId}/claim
```

**Response:**
```json
{
  "success": true,
  "message": "Mission reward claimed successfully",
  "missionId": 1
}
```

### Roulette

#### Spin Roulette
```http
POST /api/roulette/spin
```

**Response (Cash Win):**
```json
{
  "type": "CASH",
  "value": "5.00",
  "cashWon": 5.00,
  "letterWon": null,
  "remainingSpins": 2
}
```

**Response (Letter Win):**
```json
{
  "type": "LETTER",
  "value": "H",
  "cashWon": null,
  "letterWon": "H",
  "remainingSpins": 2
}
```

#### Get Roulette Configuration
```http
GET /api/roulette/slots
```

**Response:**
```json
[
  {
    "id": 1,
    "type": "CASH",
    "value": "1.00",
    "weight": 30,
    "active": true
  },
  {
    "id": 2,
    "type": "LETTER",
    "value": "H",
    "weight": 20,
    "active": true
  }
]
```

### Letters

#### Get Letter Collection
```http
GET /api/letters/collection
```

**Response:**
```json
[
  {
    "letter": "H",
    "count": 2
  },
  {
    "letter": "A",
    "count": 1
  }
]
```

#### Get Available Words
```http
GET /api/letters/words
```

**Response:**
```json
[
  {
    "id": 1,
    "word": "HAPPY",
    "requiredLetters": {
      "H": 1,
      "A": 1,
      "P": 2,
      "Y": 1
    },
    "rewardAmount": 10.00,
    "canClaim": true
  }
]
```

#### Claim Word Bonus
```http
POST /api/letters/words/{wordId}/claim
```

**Response:**
```json
{
  "success": true,
  "message": "Word bonus claimed successfully",
  "wordId": 1
}
```

### Deposits

#### Process Deposit
```http
POST /api/deposits
```

**Request:**
```json
{
  "amount": 100.00
}
```

**Response:**
```json
{
  "success": true,
  "message": "Deposit processed successfully",
  "depositAmount": 100.00,
  "newBalance": 250.00,
  "eligibleMissions": 2,
  "missionIds": [1, 2]
}
```

#### Get Deposit History
```http
GET /api/deposits/history
```

**Response:**
```json
{
  "deposits": [
    {
      "id": 1,
      "userId": 12345,
      "transactionType": "DEPOSIT",
      "amount": 100.00,
      "description": "User deposit",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "totalDeposited": 100.00,
  "depositCount": 1
}
```

## Integration Examples

### Complete User Flow Example

```javascript
// 1. Check user's available missions
const missions = await fetch('/api/missions', {
  headers: { 'X-User-Id': '12345' }
}).then(r => r.json());

// 2. Process a deposit
const deposit = await fetch('/api/deposits', {
  method: 'POST',
  headers: {
    'X-User-Id': '12345',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ amount: 100.00 })
}).then(r => r.json());

// 3. Claim mission rewards
for (const mission of missions.filter(m => m.canClaim)) {
  await fetch(`/api/missions/${mission.id}/claim`, {
    method: 'POST',
    headers: { 'X-User-Id': '12345' }
  });
}

// 4. Spin the roulette
const spinResult = await fetch('/api/roulette/spin', {
  method: 'POST',
  headers: { 'X-User-Id': '12345' }
}).then(r => r.json());

// 5. Check letter collection
const letters = await fetch('/api/letters/collection', {
  headers: { 'X-User-Id': '12345' }
}).then(r => r.json());

// 6. Check available words and claim bonuses
const words = await fetch('/api/letters/words', {
  headers: { 'X-User-Id': '12345' }
}).then(r => r.json());

for (const word of words.filter(w => w.canClaim)) {
  await fetch(`/api/letters/words/${word.id}/claim`, {
    method: 'POST',
    headers: { 'X-User-Id': '12345' }
  });
}
```

### Error Handling Example

```javascript
async function spinRoulette(userId) {
  try {
    const response = await fetch('/api/roulette/spin', {
      method: 'POST',
      headers: { 'X-User-Id': userId.toString() }
    });
    
    if (!response.ok) {
      const error = await response.json();
      
      switch (error.error) {
        case 'INSUFFICIENT_SPINS':
          showMessage('You need more spins! Complete missions to earn spins.');
          break;
        case 'INVALID_USER_ID':
          showMessage('Please log in again.');
          break;
        default:
          showMessage('Something went wrong. Please try again.');
      }
      return null;
    }
    
    return await response.json();
    
  } catch (error) {
    console.error('Network error:', error);
    showMessage('Connection error. Please check your internet connection.');
    return null;
  }
}
```

## Testing

### API Testing Tools

**Swagger UI:**
- Development: `http://localhost:8080/swagger-ui.html`
- Interactive API documentation and testing

**Postman Collection:**
```json
{
  "info": {
    "name": "Free Roulette Spin System API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080/api"
    },
    {
      "key": "userId",
      "value": "12345"
    }
  ]
}
```

### Test User Setup

For testing, you can use these test user IDs:
- `12345` - User with available spins and missions
- `67890` - New user for first deposit testing
- `11111` - User with letter collection

## Troubleshooting

### Common Issues

**1. Authentication Errors**
```
Error: INVALID_USER_ID
Solution: Ensure X-User-Id header is present and contains a valid positive integer
```

**2. Insufficient Spins**
```
Error: INSUFFICIENT_SPINS
Solution: User needs to complete missions or make deposits to earn spins
```

**3. Mission Not Available**
```
Error: MISSION_NOT_AVAILABLE
Solution: Check mission eligibility and claim limits
```

**4. Rate Limiting**
```
Error: Too Many Requests
Solution: Implement exponential backoff and respect rate limits
```

### Debug Mode

Enable debug logging by setting the environment variable:
```bash
LOGGING_LEVEL_COM_CASINO=DEBUG
```

### Health Check

Check API health:
```http
GET /api/actuator/health
```

### Support

For integration support:
- Email: dev@casino.com
- Documentation: https://docs.casino.com/roulette-api
- Status Page: https://status.casino.com

## Changelog

### Version 1.0.0
- Initial API release
- Mission management endpoints
- Roulette gameplay endpoints
- Letter collection endpoints
- Deposit processing endpoints
- Comprehensive error handling
- Rate limiting implementation