# OSCAL CLI Web API Specification

Complete REST API specification for the OSCAL CLI Web Interface.

## Table of Contents

- [Overview](#overview)
- [Base URL](#base-url)
- [Authentication](#authentication)
- [Common Response Formats](#common-response-formats)
- [Endpoints](#endpoints)
  - [Validation](#validation-api)
  - [Conversion](#conversion-api)
  - [Profile Resolution](#profile-resolution-api)
  - [Batch Operations](#batch-operations-api)
  - [Status & Health](#status--health-api)
- [WebSocket Events](#websocket-events)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)

## Overview

The OSCAL CLI Web API provides RESTful endpoints for all OSCAL operations:
- Document validation
- Format conversion (XML ↔ JSON ↔ YAML)
- Profile resolution
- Batch processing

All endpoints accept and return JSON (except file downloads).

## Base URL

```
http://localhost:8080/api
```

For production:
```
https://your-domain.com/api
```

## Authentication

### Phase 1-3: No Authentication
Open API for initial development and testing.

### Phase 4: JWT Authentication (Optional)

**Login**:
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password123"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600,
  "user": {
    "id": "123",
    "username": "user@example.com",
    "role": "user"
  }
}
```

**Using the token**:
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Common Response Formats

### Success Response

```json
{
  "success": true,
  "data": {
    // Response data
  },
  "timestamp": "2025-10-15T10:30:00Z"
}
```

### Error Response

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Document validation failed",
    "details": [
      {
        "line": 42,
        "column": 15,
        "message": "Missing required element 'title'",
        "severity": "error"
      }
    ]
  },
  "timestamp": "2025-10-15T10:30:00Z"
}
```

## Endpoints

## Validation API

### Validate OSCAL Document

Validates an OSCAL document against its schema and constraints.

```http
POST /api/validate
Content-Type: multipart/form-data
```

**Parameters**:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| file | File | Yes | OSCAL document file |
| modelType | String | Yes | OSCAL model type: `catalog`, `profile`, `component-definition`, `ssp`, `assessment-plan`, `assessment-results`, `poam` |
| format | String | No | File format: `xml`, `json`, `yaml`. Auto-detected if omitted. |

**Request Example**:
```bash
curl -X POST http://localhost:8080/api/validate \
  -F "file=@catalog.xml" \
  -F "modelType=catalog"
```

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "valid": true,
    "modelType": "catalog",
    "format": "xml",
    "fileSize": 15234,
    "validationTime": 125,
    "message": "Document is valid"
  }
}
```

**Validation Failed Response** (200 OK - validation ran successfully but found errors):
```json
{
  "success": true,
  "data": {
    "valid": false,
    "modelType": "catalog",
    "format": "xml",
    "errors": [
      {
        "line": 42,
        "column": 15,
        "path": "/catalog/metadata/title",
        "message": "Missing required element 'title'",
        "severity": "error",
        "code": "REQUIRED_ELEMENT_MISSING"
      },
      {
        "line": 156,
        "column": 8,
        "path": "/catalog/control[0]/id",
        "message": "Invalid UUID format",
        "severity": "error",
        "code": "INVALID_UUID"
      }
    ],
    "warnings": [
      {
        "line": 203,
        "column": 12,
        "path": "/catalog/control[5]/description",
        "message": "Description should not be empty",
        "severity": "warning",
        "code": "EMPTY_ELEMENT"
      }
    ],
    "errorCount": 2,
    "warningCount": 1
  }
}
```

**Error Response** (400 Bad Request):
```json
{
  "success": false,
  "error": {
    "code": "INVALID_FILE_TYPE",
    "message": "Unsupported file type. Expected XML, JSON, or YAML."
  }
}
```

---

### Validate with Content

Alternative endpoint that accepts file content as JSON.

```http
POST /api/validate/content
Content-Type: application/json
```

**Request Body**:
```json
{
  "content": "<catalog xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\">...</catalog>",
  "modelType": "catalog",
  "format": "xml"
}
```

---

## Conversion API

### Convert OSCAL Document

Converts an OSCAL document between XML, JSON, and YAML formats.

```http
POST /api/convert
Content-Type: multipart/form-data
```

**Parameters**:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| file | File | Yes | OSCAL document file |
| modelType | String | Yes | OSCAL model type |
| sourceFormat | String | No | Source format (auto-detected) |
| targetFormat | String | Yes | Target format: `xml`, `json`, `yaml` |
| download | Boolean | No | If true, returns file download. Default: false |

**Request Example**:
```bash
curl -X POST http://localhost:8080/api/convert \
  -F "file=@catalog.xml" \
  -F "modelType=catalog" \
  -F "targetFormat=json" \
  -F "download=false"
```

**Success Response - JSON** (download=false):
```json
{
  "success": true,
  "data": {
    "convertedContent": "{\"catalog\": {...}}",
    "sourceFormat": "xml",
    "targetFormat": "json",
    "originalSize": 15234,
    "convertedSize": 12890,
    "conversionTime": 87
  }
}
```

**Success Response - File Download** (download=true):
```http
HTTP/1.1 200 OK
Content-Type: application/json
Content-Disposition: attachment; filename="catalog.json"

{
  "catalog": {
    "uuid": "...",
    ...
  }
}
```

**Error Response** (400 Bad Request):
```json
{
  "success": false,
  "error": {
    "code": "CONVERSION_ERROR",
    "message": "Failed to convert document: Invalid XML structure"
  }
}
```

---

### Convert with Content

```http
POST /api/convert/content
Content-Type: application/json
```

**Request Body**:
```json
{
  "content": "<catalog>...</catalog>",
  "modelType": "catalog",
  "sourceFormat": "xml",
  "targetFormat": "json"
}
```

---

## Profile Resolution API

### Resolve Profile

Resolves an OSCAL Profile into a resolved Catalog.

```http
POST /api/profile/resolve
Content-Type: multipart/form-data
```

**Parameters**:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| file | File | Yes | OSCAL Profile file |
| targetFormat | String | Yes | Output format: `xml`, `json`, `yaml` |
| download | Boolean | No | If true, returns file download |

**Request Example**:
```bash
curl -X POST http://localhost:8080/api/profile/resolve \
  -F "file=@profile.json" \
  -F "targetFormat=json"
```

**Success Response**:
```json
{
  "success": true,
  "data": {
    "resolvedCatalog": "{\"catalog\": {...}}",
    "targetFormat": "json",
    "profileSize": 5234,
    "catalogSize": 45678,
    "controlsSelected": 156,
    "resolutionTime": 342,
    "importsProcessed": [
      {
        "href": "https://raw.githubusercontent.com/.../catalog.json",
        "controlsImported": 156
      }
    ]
  }
}
```

**Error Response** (400 Bad Request):
```json
{
  "success": false,
  "error": {
    "code": "PROFILE_RESOLUTION_ERROR",
    "message": "Failed to resolve profile: Unable to load imported catalog from https://example.com/catalog.json",
    "details": [
      {
        "href": "https://example.com/catalog.json",
        "error": "Connection timeout"
      }
    ]
  }
}
```

---

## Batch Operations API

### Validate Multiple Files

Validates multiple OSCAL documents in a single request.

```http
POST /api/batch/validate
Content-Type: multipart/form-data
```

**Parameters**:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| files | File[] | Yes | Array of OSCAL documents |
| modelType | String | Yes | OSCAL model type for all files |

**Request Example**:
```bash
curl -X POST http://localhost:8080/api/batch/validate \
  -F "files=@catalog1.xml" \
  -F "files=@catalog2.json" \
  -F "files=@catalog3.yaml" \
  -F "modelType=catalog"
```

**Immediate Response** (202 Accepted):
```json
{
  "success": true,
  "data": {
    "operationId": "batch-valid-abc123",
    "status": "processing",
    "totalFiles": 3,
    "message": "Batch operation started. Connect to WebSocket for progress updates.",
    "websocketUrl": "ws://localhost:8080/ws/operations/batch-valid-abc123"
  }
}
```

**WebSocket Progress Updates**: See [WebSocket Events](#websocket-events)

---

### Convert Multiple Files

Converts multiple files to a target format.

```http
POST /api/batch/convert
Content-Type: multipart/form-data
```

**Parameters**:

| Name | Type | Required | Description |
|------|------|----------|-------------|
| files | File[] | Yes | Array of OSCAL documents |
| modelType | String | Yes | OSCAL model type |
| targetFormat | String | Yes | Target format |

**Response**: Similar to batch validate (202 Accepted with operationId)

---

### Get Batch Operation Status

Check the status of a batch operation.

```http
GET /api/batch/status/{operationId}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "operationId": "batch-valid-abc123",
    "status": "completed",
    "totalFiles": 3,
    "processedFiles": 3,
    "successCount": 2,
    "errorCount": 1,
    "startTime": "2025-10-15T10:30:00Z",
    "endTime": "2025-10-15T10:30:15Z",
    "results": [
      {
        "filename": "catalog1.xml",
        "status": "success",
        "valid": true
      },
      {
        "filename": "catalog2.json",
        "status": "success",
        "valid": true
      },
      {
        "filename": "catalog3.yaml",
        "status": "error",
        "valid": false,
        "errors": [...]
      }
    ]
  }
}
```

---

### Download Batch Results

Download all converted files as a ZIP archive.

```http
GET /api/batch/download/{operationId}
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/zip
Content-Disposition: attachment; filename="batch-results.zip"

[ZIP file contents]
```

---

## Status & Health API

### Health Check

Check if the API is running.

```http
GET /api/health
```

**Response**:
```json
{
  "status": "UP",
  "version": "1.0.0",
  "timestamp": "2025-10-15T10:30:00Z"
}
```

---

### System Info

Get system information and capabilities.

```http
GET /api/info
```

**Response**:
```json
{
  "success": true,
  "data": {
    "version": "1.0.0",
    "oscalVersion": "1.1.2",
    "supportedFormats": ["xml", "json", "yaml"],
    "supportedModels": [
      "catalog",
      "profile",
      "component-definition",
      "ssp",
      "assessment-plan",
      "assessment-results",
      "poam"
    ],
    "maxFileSize": 10485760,
    "maxBatchSize": 10,
    "features": {
      "validation": true,
      "conversion": true,
      "profileResolution": true,
      "batchOperations": true,
      "authentication": false
    }
  }
}
```

---

## WebSocket Events

For real-time updates on long-running operations, connect to the WebSocket endpoint.

### Connection

```
ws://localhost:8080/ws/operations/{operationId}
```

### Event Types

#### Progress Update

Sent periodically during operation.

```json
{
  "type": "progress",
  "operationId": "batch-valid-abc123",
  "totalFiles": 10,
  "processedFiles": 3,
  "currentFile": "catalog3.xml",
  "percentComplete": 30,
  "timestamp": "2025-10-15T10:30:05Z"
}
```

#### File Complete

Sent when a single file is processed.

```json
{
  "type": "file_complete",
  "operationId": "batch-valid-abc123",
  "filename": "catalog3.xml",
  "status": "success",
  "result": {
    "valid": true,
    "processingTime": 125
  },
  "timestamp": "2025-10-15T10:30:05Z"
}
```

#### Error

Sent when an error occurs processing a file.

```json
{
  "type": "error",
  "operationId": "batch-valid-abc123",
  "filename": "catalog5.xml",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Document validation failed",
    "details": [...]
  },
  "timestamp": "2025-10-15T10:30:08Z"
}
```

#### Complete

Sent when entire operation is complete.

```json
{
  "type": "complete",
  "operationId": "batch-valid-abc123",
  "status": "completed",
  "totalFiles": 10,
  "successCount": 8,
  "errorCount": 2,
  "totalTime": 1542,
  "downloadUrl": "/api/batch/download/batch-valid-abc123",
  "timestamp": "2025-10-15T10:30:15Z"
}
```

---

## Error Handling

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_FILE_TYPE` | 400 | Unsupported file format |
| `FILE_TOO_LARGE` | 413 | File exceeds max size limit |
| `INVALID_MODEL_TYPE` | 400 | Unknown OSCAL model type |
| `VALIDATION_ERROR` | 200 | Validation completed with errors |
| `CONVERSION_ERROR` | 400 | Failed to convert document |
| `PROFILE_RESOLUTION_ERROR` | 400 | Failed to resolve profile |
| `OPERATION_NOT_FOUND` | 404 | Batch operation not found |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected server error |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |

### Error Response Format

All errors follow this structure:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": [
      // Optional array of detailed error information
    ]
  },
  "timestamp": "2025-10-15T10:30:00Z"
}
```

---

## Rate Limiting

To prevent abuse, the API implements rate limiting:

- **Anonymous users**: 100 requests per hour
- **Authenticated users** (Phase 4): 1000 requests per hour

Rate limit headers are included in all responses:

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1634299200
```

When rate limit is exceeded:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 3600

{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded. Try again in 3600 seconds.",
    "retryAfter": 3600
  }
}
```

---

## OpenAPI / Swagger Documentation

Interactive API documentation will be available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI 3.0 specification:

```
http://localhost:8080/api-docs
```

---

## Examples

### Complete Validation Example (Node.js)

```javascript
const FormData = require('form-data');
const fs = require('fs');
const axios = require('axios');

async function validateCatalog(filePath) {
  const form = new FormData();
  form.append('file', fs.createReadStream(filePath));
  form.append('modelType', 'catalog');

  const response = await axios.post(
    'http://localhost:8080/api/validate',
    form,
    { headers: form.getHeaders() }
  );

  if (response.data.success && response.data.data.valid) {
    console.log('✓ Document is valid!');
  } else {
    console.log('✗ Validation errors:');
    response.data.data.errors.forEach(err => {
      console.log(`  Line ${err.line}: ${err.message}`);
    });
  }
}

validateCatalog('./my-catalog.xml');
```

### Complete Batch Operation Example (JavaScript)

```javascript
// Upload files for batch validation
async function batchValidate(files) {
  const form = new FormData();
  files.forEach(file => form.append('files', file));
  form.append('modelType', 'catalog');

  const response = await fetch('http://localhost:8080/api/batch/validate', {
    method: 'POST',
    body: form
  });

  const data = await response.json();
  const operationId = data.data.operationId;

  // Connect to WebSocket for progress updates
  const ws = new WebSocket(data.data.websocketUrl);

  ws.onmessage = (event) => {
    const message = JSON.parse(event.data);

    switch (message.type) {
      case 'progress':
        console.log(`Progress: ${message.percentComplete}%`);
        break;

      case 'file_complete':
        console.log(`Completed: ${message.filename}`);
        break;

      case 'complete':
        console.log('All files processed!');
        console.log(`Success: ${message.successCount}, Errors: ${message.errorCount}`);
        ws.close();
        break;
    }
  };
}
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | TBD | Initial API release |

---

## Future API Endpoints (Planned)

- `POST /api/catalog/search` - Search controls in a catalog
- `POST /api/profile/preview` - Preview profile resolution without full processing
- `POST /api/compare` - Compare two OSCAL documents
- `POST /api/merge` - Merge multiple OSCAL documents
- `GET /api/templates` - List available OSCAL templates
- `POST /api/generate` - Generate OSCAL document from template
