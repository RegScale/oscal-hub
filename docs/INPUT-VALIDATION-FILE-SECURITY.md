# Input Validation & File Security

**Date**: 2025-10-26
**Status**: Production Ready
**Priority**: CRITICAL (SEC-03)

## Overview

This document describes the input validation and file security measures implemented to protect the OSCAL Tools platform from malicious file uploads, path traversal attacks, MIME type spoofing, and other file-based security threats.

## Table of Contents

1. [Security Features](#security-features)
2. [Implementation Details](#implementation-details)
3. [Configuration](#configuration)
4. [Usage Examples](#usage-examples)
5. [Testing](#testing)
6. [Troubleshooting](#troubleshooting)
7. [Best Practices](#best-practices)

## Security Features

### 1. File Type Whitelisting

**Purpose**: Prevent upload of executable files or other dangerous file types

**Implementation**:
- Whitelist of allowed file extensions: `json`, `xml`, `yaml`, `yml`
- Whitelist of allowed MIME types: `application/json`, `application/xml`, `text/xml`, `application/x-yaml`, `text/yaml`, `text/plain`
- Rejec all other file types

**Configuration**: `FileValidationConfig.java`
```java
private Set<String> allowedFileExtensions = new HashSet<>(Arrays.asList(
    "json", "xml", "yaml", "yml"
));
```

### 2. Magic Number Validation

**Purpose**: Prevent MIME type spoofing by validating actual file content

**How it works**:
- Reads the first few bytes of uploaded files
- Compares against known file signatures (magic numbers)
- Rejects files where content doesn't match declared type

**Supported Image Types** (for logos):
- PNG: `89 50 4E 47`
- JPEG: `FF D8 FF`
- GIF: `47 49 46 38`
- SVG: Validated separately (XML-based)

**Example**:
```java
// User uploads file with .png extension
// System reads first 4 bytes: FF D8 FF E0
// Expected for PNG: 89 50 4E 47
// Result: REJECTED - File is actually JPEG, not PNG
```

### 3. Filename Sanitization

**Purpose**: Prevent path traversal and special character attacks

**Protection Against**:
- Path traversal: `../../../etc/passwd`
- Null bytes: `file.txt\0.exe`
- Special characters: `file:name`, `file<>name`, `file|name`
- Hidden files: `.hidden`
- Overly long filenames: > 255 characters

**Forbidden Characters**:
```java
"..","  "/", "\\", ":", "*", "?", "\"", "<", ">", "|", "\0"
```

**Sanitization Process**:
1. Trim whitespace
2. Check for forbidden characters (reject if found)
3. Remove leading/trailing dots
4. Limit to 255 characters
5. Verify filename isn't empty after sanitization

### 4. File Size Limits

**Purpose**: Prevent denial-of-service attacks via large file uploads

**Limits**:
- **OSCAL Documents**: 10 MB (configurable)
- **Base64 Logos**: 2 MB (configurable)
- **HTTP Requests**: 10 MB (Spring Boot multipart limit)

**Rationale**:
- OSCAL documents are text-based and typically < 1 MB
- Logos should be optimized for web use (< 200 KB ideal)
- Generous limits allow legitimate use while preventing abuse

### 5. Base64 Image Validation

**Purpose**: Validate user-uploaded logos for profile customization

**Validation Steps**:
1. **Format validation**: Must be data URL (`data:image/TYPE;base64,DATA`)
2. **MIME type validation**: Must be allowed image type
3. **Size validation**: Must be under 2 MB limit
4. **Base64 decoding**: Verify data is valid Base64
5. **Magic number validation**: Verify image signature matches MIME type
6. **SVG-specific validation**: Check for dangerous content (scripts, iframes)

**Rejected Content in SVG**:
- `<script>` tags
- `javascript:` URLs
- Event handlers (`onclick`, `onload`, etc.)
- `<iframe>`, `<embed>`, `<object>` tags

### 6. Content Format Validation

**Purpose**: Verify file content matches declared format

**Checks**:
- **JSON files**: Must start with `{` or `[`
- **XML files**: Must start with `<` or `<?xml`
- **YAML files**: Warn if appears to be JSON/XML

### 7. Virus Scanning Integration (Optional)

**Purpose**: Scan uploaded files for malware

**Status**: Hooks implemented, integration optional

**Configuration**:
```properties
file.validation.enable-virus-scanning=true
file.validation.virus-scanning-url=http://localhost:3310/scan
```

**Supported Services**:
- ClamAV REST API
- VirusTotal API
- Other custom scanning services

## Implementation Details

### Core Classes

#### FileValidationConfig.java

Configuration class for all validation settings.

**Location**: `back-end/src/main/java/.../config/FileValidationConfig.java`

**Key Properties**:
```java
@ConfigurationProperties(prefix = "file.validation")
public class FileValidationConfig {
    private long maxFileSize = 10 * 1024 * 1024;        // 10 MB
    private long maxLogoSize = 2 * 1024 * 1024;         // 2 MB
    private Set<String> allowedFileExtensions;           // json, xml, yaml
    private Set<String> allowedMimeTypes;                // application/json, etc.
    private Set<String> allowedImageTypes;               // image/png, image/jpeg, etc.
    private boolean enableMagicNumberValidation = true;
    private boolean enableVirusScanning = false;
}
```

#### FileValidationService.java

Service implementing all validation logic.

**Location**: `back-end/src/main/java/.../service/FileValidationService.java`

**Key Methods**:

| Method | Purpose |
|--------|---------|
| `sanitizeFilename(String)` | Sanitize and validate filename |
| `validateFileExtension(String)` | Check extension against whitelist |
| `validateFileSize(String, long)` | Enforce size limits |
| `validateBase64Logo(String)` | Comprehensive logo validation |
| `validateImageMagicNumber(byte[], String)` | Verify file signature |
| `validateOscalFile(String, String)` | Complete OSCAL file validation |
| `scanForViruses(byte[], String)` | Integration hook for virus scanning |

### Controller Integration

#### AuthController.java (Logo Upload)

**Endpoint**: `POST /api/auth/logo`

**Before**:
```java
// Minimal validation
if (!logo.startsWith("data:image/")) {
    return ResponseEntity.badRequest().body("Invalid logo");
}
```

**After**:
```java
// Comprehensive validation
fileValidationService.validateBase64Logo(logo);
// Throws IllegalArgumentException with detailed message if invalid
```

#### FileController.java (OSCAL Document Upload)

**Endpoint**: `POST /api/files`

**Before**:
```java
// No validation
SavedFile savedFile = fileStorageService.saveFile(...);
```

**After**:
```java
// Comprehensive validation
fileValidationService.validateOscalFile(request.getContent(), fileName);
SavedFile savedFile = fileStorageService.saveFile(...);
```

## Configuration

### Application Properties

**File**: `back-end/src/main/resources/application.properties`

```properties
# File Validation Configuration
file.validation.max-file-size=10485760                    # 10 MB
file.validation.max-logo-size=2097152                      # 2 MB
file.validation.enable-magic-number-validation=true
file.validation.enable-virus-scanning=false
file.validation.virus-scanning-url=
```

### Environment Variables

**File**: `.env` (copy from `.env.example`)

```bash
# Maximum file size for OSCAL documents (10 MB)
FILE_VALIDATION_MAX_SIZE=10485760

# Maximum size for Base64 logos (2 MB)
FILE_VALIDATION_MAX_LOGO_SIZE=2097152

# Enable magic number validation
FILE_VALIDATION_MAGIC_NUMBER=true

# Virus scanning (requires external service)
FILE_VALIDATION_VIRUS_SCAN=false
FILE_VALIDATION_VIRUS_SCAN_URL=
```

### Customization

**Adjusting File Size Limits**:
```properties
# Increase to 50 MB for large OSCAL documents
file.validation.max-file-size=52428800

# Increase to 5 MB for high-res logos
file.validation.max-logo-size=5242880
```

**Adding Allowed File Types**:
```java
// In FileValidationConfig.java
private Set<String> allowedFileExtensions = new HashSet<>(Arrays.asList(
    "json", "xml", "yaml", "yml", "pdf"  // Added PDF
));
```

**Disabling Magic Number Validation** (not recommended):
```properties
file.validation.enable-magic-number-validation=false
```

## Usage Examples

### Valid File Upload (JSON)

```bash
curl -X POST http://localhost:8080/api/files \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "{\"catalog\": {\"uuid\": \"...\", \"metadata\": {...}}}",
    "fileName": "my-catalog.json",
    "format": "JSON",
    "modelType": "CATALOG"
  }'
```

**Response**:
```json
{
  "fileId": "123e4567-e89b-12d3-a456-426614174000",
  "fileName": "my-catalog.json",
  "format": "JSON",
  "modelType": "CATALOG",
  "createdAt": "2025-10-26T12:00:00Z"
}
```

### Invalid File Upload (Forbidden Extension)

```bash
curl -X POST http://localhost:8080/api/files \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "malicious content",
    "fileName": "malicious.exe",
    "format": "JSON"
  }'
```

**Response** (400 Bad Request):
```json
{
  "error": "File extension '.exe' is not allowed. Allowed extensions: [json, xml, yaml, yml]"
}
```

### Invalid File Upload (Path Traversal)

```bash
curl -X POST http://localhost:8080/api/files \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "content",
    "fileName": "../../../etc/passwd.json"
  }'
```

**Response** (400 Bad Request):
```json
{
  "error": "Filename contains forbidden characters: .."
}
```

### Invalid File Upload (Size Exceeded)

```bash
# Attempting to upload 15 MB file (limit is 10 MB)
curl -X POST http://localhost:8080/api/files \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d @large-file.json
```

**Response** (400 Bad Request):
```json
{
  "error": "File size (15728640 bytes) exceeds maximum allowed size (10485760 bytes)"
}
```

### Valid Logo Upload

```bash
curl -X POST http://localhost:8080/api/auth/logo \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "logo": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA..."
  }'
```

**Response**:
```json
{
  "message": "Logo uploaded successfully",
  "logo": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA..."
}
```

### Invalid Logo Upload (Wrong MIME Type)

```bash
curl -X POST http://localhost:8080/api/auth/logo \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "logo": "data:application/pdf;base64,JVBERi0xLj..."
  }'
```

**Response** (400 Bad Request):
```json
{
  "error": "Image type 'application/pdf' is not allowed. Allowed types: [image/png, image/jpeg, image/jpg, image/gif, image/svg+xml]"
}
```

### Invalid Logo Upload (MIME Type Spoofing)

```bash
# File claims to be PNG but is actually JPEG
curl -X POST http://localhost:8080/api/auth/logo \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "logo": "data:image/png;base64,/9j/4AAQSkZJRgABAQEA..."
  }'
```

**Response** (400 Bad Request):
```json
{
  "error": "File signature does not match declared type 'image/png'. Possible file type spoofing detected."
}
```

## Testing

### Unit Tests

Create test cases for each validation method:

```java
@Test
public void testValidateFilename_PathTraversal() {
    assertThrows(IllegalArgumentException.class, () -> {
        fileValidationService.sanitizeFilename("../../../etc/passwd");
    });
}

@Test
public void testValidateFileExtension_Forbidden() {
    assertThrows(IllegalArgumentException.class, () -> {
        fileValidationService.validateFileExtension("malicious.exe");
    });
}

@Test
public void testValidateBase64Logo_InvalidMimeType() {
    String logo = "data:application/pdf;base64,JVBERi0x...";
    assertThrows(IllegalArgumentException.class, () -> {
        fileValidationService.validateBase64Logo(logo);
    });
}

@Test
public void testValidateImageMagicNumber_Spoofed() {
    // JPEG bytes with PNG MIME type
    byte[] jpegBytes = new byte[]{(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0};
    assertThrows(IllegalArgumentException.class, () -> {
        fileValidationService.validateImageMagicNumber(jpegBytes, "image/png");
    });
}
```

### Integration Tests

Test full file upload workflow:

```java
@Test
@WithMockUser
public void testUploadFile_ValidJson() throws Exception {
    FileUploadRequest request = new FileUploadRequest(
        "{\"catalog\": {\"uuid\": \"test\"}}",
        OscalFormat.JSON,
        OscalModelType.CATALOG,
        "test-catalog.json"
    );

    mockMvc.perform(post("/api/files")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fileName").value("test-catalog.json"));
}

@Test
@WithMockUser
public void testUploadFile_ForbiddenExtension() throws Exception {
    FileUploadRequest request = new FileUploadRequest(
        "content",
        OscalFormat.JSON,
        OscalModelType.CATALOG,
        "malicious.exe"
    );

    mockMvc.perform(post("/api/files")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value(containsString("not allowed")));
}
```

### Manual Testing

**Test Path Traversal Protection**:
```bash
# Should be rejected
curl -X POST http://localhost:8080/api/files \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"test","fileName":"../../../etc/passwd.json"}'
```

**Test Magic Number Validation**:
```bash
# Create JPEG file with .png extension
echo -e '\xFF\xD8\xFF\xE0' > fake.png
base64 fake.png

# Try to upload as PNG (should be rejected)
curl -X POST http://localhost:8080/api/auth/logo \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"logo":"data:image/png;base64,/9j/4AAQ..."}'
```

## Troubleshooting

### Problem: Valid Files Rejected

**Symptom**: Legitimate OSCAL files are being rejected

**Possible Causes**:
1. File size exceeds limit
2. File extension not in whitelist
3. Content format doesn't match extension

**Solution**:
```bash
# Check file size
ls -lh my-catalog.json

# Verify file is valid JSON/XML/YAML
cat my-catalog.json | jq .   # For JSON
xmllint my-catalog.xml        # For XML
yamllint my-catalog.yaml      # For YAML

# Increase size limit if needed
FILE_VALIDATION_MAX_SIZE=52428800  # 50 MB
```

### Problem: Logo Upload Fails

**Symptom**: Logo upload returns "Invalid Base64 data"

**Possible Causes**:
1. Data URL format incorrect
2. Base64 padding incorrect
3. Non-Base64 characters in data

**Solution**:
```javascript
// Ensure correct data URL format
const dataUrl = `data:${mimeType};base64,${base64Data}`;

// Verify Base64 is valid
try {
    atob(base64Data);  // Should not throw
} catch (e) {
    console.error("Invalid Base64:", e);
}

// Check for newlines or spaces
const cleanBase64 = base64Data.replace(/[\r\n\s]/g, '');
```

### Problem: SVG Upload Rejected

**Symptom**: SVG files are rejected with "SVG contains forbidden content"

**Cause**: SVG contains JavaScript or other dangerous content

**Solution**:
```bash
# Check SVG for forbidden patterns
grep -i "<script" my-logo.svg
grep -i "javascript:" my-logo.svg
grep -i "on[a-z]*=" my-logo.svg

# Remove JavaScript from SVG
sed -i '/<script/d' my-logo.svg
sed -i 's/javascript://g' my-logo.svg

# Use sanitized SVG tools
npm install -g svgo
svgo --multipass my-logo.svg
```

### Problem: False Positive - Magic Number Mismatch

**Symptom**: Valid files rejected due to magic number mismatch

**Cause**: File has different header than standard (e.g., JPEG with EXIF vs JFIF header)

**Solution**:
```properties
# Disable magic number validation (not recommended)
file.validation.enable-magic-number-validation=false

# Or, report issue to add additional magic numbers
```

## Best Practices

### Development

1. **Always test with malicious inputs**: Try path traversal, oversized files, wrong MIME types
2. **Use realistic test data**: Test with actual OSCAL documents, not just "hello world"
3. **Test error messages**: Ensure error messages are helpful but don't leak sensitive info
4. **Check logs**: Review validation rejections in logs for false positives

### Deployment

1. **Enable all validation**: Magic number validation should be enabled in production
2. **Monitor rejection rates**: Track how many uploads are rejected to tune limits
3. **Set appropriate limits**: Balance security with legitimate use cases
4. **Consider virus scanning**: For high-security environments, integrate antivirus
5. **Regular updates**: Update magic number database for new file types

### Security

1. **Defense in depth**: File validation is one layer, combine with other security measures
2. **Fail securely**: Reject files when in doubt, log for investigation
3. **Input sanitization everywhere**: Never trust client-provided filenames or MIME types
4. **Regular audits**: Review validation logic for new attack vectors
5. **Security headers**: Combine with CSP to prevent uploaded content from executing

### Performance

1. **Early validation**: Check size/extension before processing content
2. **Stream large files**: Don't load entire file into memory for validation
3. **Cache validation results**: For repeated uploads of same file
4. **Async virus scanning**: Don't block uploads waiting for virus scan results

## Related Security Features

- **Rate Limiting**: Prevents brute-force file upload attacks (`docs/RATE-LIMITING.md`)
- **Authentication**: Ensures only authenticated users can upload (`docs/SECRETS-MANAGEMENT.md`)
- **HTTPS/TLS**: Encrypts file uploads in transit (`docs/TLS-CONFIGURATION.md`)
- **Security Headers**: Prevents uploaded content from executing (`docs/SECURITY-HEADERS.md`)

## Compliance

### OWASP Top 10 2021

- **A01:2021 Broken Access Control**: Filename sanitization prevents path traversal
- **A03:2021 Injection**: Input validation prevents malicious file content
- **A04:2021 Insecure Design**: Whitelist approach vs blacklist
- **A05:2021 Security Misconfiguration**: Secure defaults, validation enabled by default

### CWE Mitigations

- **CWE-22**: Path Traversal (filename sanitization)
- **CWE-434**: Unrestricted Upload of File with Dangerous Type (file type whitelist)
- **CWE-400**: Uncontrolled Resource Consumption (file size limits)
- **CWE-601**: URL Redirection to Untrusted Site (data URL validation)

## References

- **OWASP File Upload Cheat Sheet**: https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html
- **File Signatures (Magic Numbers)**: https://en.wikipedia.org/wiki/List_of_file_signatures
- **SVG Security**: https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
- **Spring Boot File Upload**: https://spring.io/guides/gs/uploading-files/

## Related Documentation

- `docs/PRODUCTION-SECURITY-HARDENING-PLAN.md` - Overall security roadmap
- `docs/RATE-LIMITING.md` - Prevent brute-force attacks
- `docs/SECURITY-HEADERS.md` - HTTP security headers
- `docs/TLS-CONFIGURATION.md` - HTTPS/TLS setup
