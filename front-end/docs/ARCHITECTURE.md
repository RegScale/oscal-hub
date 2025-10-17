# OSCAL CLI Web Interface - Architecture

Detailed architectural design for the OSCAL CLI web-based frontend.

## Table of Contents

- [System Overview](#system-overview)
- [Architectural Layers](#architectural-layers)
- [Technology Decisions](#technology-decisions)
- [Component Design](#component-design)
- [Data Flow](#data-flow)
- [Security Architecture](#security-architecture)
- [Performance Considerations](#performance-considerations)
- [Scalability](#scalability)

## System Overview

The OSCAL CLI Web Interface transforms the command-line tool into an accessible web application while maintaining the same core functionality. The architecture prioritizes:

1. **Direct Integration**: No subprocess calls - uses OSCAL libraries directly
2. **Single Deployment**: Embedded frontend in Spring Boot JAR
3. **Real-time Feedback**: WebSocket for long-running operations
4. **API-First**: RESTful API that can be used independently

## Architectural Layers

### 1. Presentation Layer (Next.js Frontend)

```
┌─────────────────────────────────────────────────────┐
│           Next.js 14+ Application (App Router)       │
├─────────────────────────────────────────────────────┤
│  App Routes (Pages)                                 │
│  ├── app/page.tsx (Dashboard)                      │
│  ├── app/validate/page.tsx                         │
│  ├── app/convert/page.tsx                          │
│  ├── app/resolve/page.tsx                          │
│  ├── app/batch/page.tsx                            │
│  └── app/history/page.tsx                          │
├─────────────────────────────────────────────────────┤
│  ShadCN UI Components (Dark Mode)                  │
│  ├── components/ui/* (ShadCN primitives)           │
│  ├── components/file-uploader.tsx                  │
│  ├── components/code-editor.tsx (Monaco)           │
│  ├── components/validation-results.tsx             │
│  ├── components/format-selector.tsx                │
│  └── components/progress-indicator.tsx             │
├─────────────────────────────────────────────────────┤
│  State Management (Zustand + RSC)                  │
│  ├── stores/file-store.ts                          │
│  ├── stores/operation-store.ts                     │
│  ├── stores/websocket-store.ts                     │
│  └── React Server Components for data fetching     │
├─────────────────────────────────────────────────────┤
│  API Layer (Next.js Server Actions + Client)       │
│  ├── app/actions/validate.ts (Server Actions)      │
│  ├── app/actions/convert.ts                        │
│  ├── lib/api-client.ts (fetch wrapper)             │
│  └── lib/websocket.ts                              │
└─────────────────────────────────────────────────────┘
```

### 2. API Layer (Spring Boot)

```
┌─────────────────────────────────────────────────────┐
│              Spring Boot Application                 │
├─────────────────────────────────────────────────────┤
│  REST Controllers                                   │
│  ├── ValidationController   (@PostMapping)          │
│  ├── ConversionController   (@PostMapping)          │
│  ├── ProfileController      (@PostMapping)          │
│  ├── BatchController        (@PostMapping)          │
│  └── StatusController       (@GetMapping)           │
├─────────────────────────────────────────────────────┤
│  WebSocket Controllers                              │
│  ├── OperationProgressHandler                       │
│  └── NotificationHandler                             │
├─────────────────────────────────────────────────────┤
│  Exception Handling                                 │
│  ├── GlobalExceptionHandler                         │
│  ├── ValidationExceptionHandler                     │
│  └── FileUploadExceptionHandler                     │
└─────────────────────────────────────────────────────┘
```

### 3. Service Layer

```
┌─────────────────────────────────────────────────────┐
│              Business Logic Services                 │
├─────────────────────────────────────────────────────┤
│  Core Services                                      │
│  ├── ValidationService                              │
│  │   └── Uses OscalBindingContext                  │
│  ├── ConversionService                              │
│  │   └── Format detection & transformation         │
│  ├── ProfileResolutionService                       │
│  │   └── Uses ProfileResolver                       │
│  └── BatchOperationService                          │
│      └── Orchestrates multiple operations           │
├─────────────────────────────────────────────────────┤
│  Supporting Services                                │
│  ├── FileStorageService                             │
│  │   └── Temporary file management                 │
│  ├── OperationHistoryService                        │
│  │   └── Track completed operations                │
│  └── NotificationService                             │
│      └── WebSocket message broadcasting             │
└─────────────────────────────────────────────────────┘
```

### 4. Integration Layer

```
┌─────────────────────────────────────────────────────┐
│           OSCAL CLI Core Integration                 │
├─────────────────────────────────────────────────────┤
│  Adapters/Wrappers                                  │
│  ├── OscalValidationAdapter                         │
│  │   └── Wraps AbstractOscalValidationSubcommand   │
│  ├── OscalConversionAdapter                         │
│  │   └── Wraps AbstractOscalConvertSubcommand      │
│  └── ProfileResolverAdapter                         │
│      └── Wraps ResolveSubcommand logic              │
├─────────────────────────────────────────────────────┤
│  Direct Library Usage (Reused from CLI)            │
│  ├── OscalBindingContext                            │
│  ├── IBoundLoader                                   │
│  ├── ISerializer                                    │
│  ├── ProfileResolver                                │
│  └── Model classes (Catalog, Profile, etc.)        │
└─────────────────────────────────────────────────────┘
```

## Technology Decisions

### Backend: Spring Boot

**Why Spring Boot?**
- **Same ecosystem**: Already using Java & Maven
- **Direct integration**: Can use OSCAL libraries without subprocess calls
- **Mature**: Well-tested, extensive documentation
- **Embedded server**: Tomcat included, no external server needed
- **Easy deployment**: Single executable JAR
- **WebSocket support**: Built-in for real-time updates

**Alternatives considered:**
- ❌ Node.js wrapper: Would require subprocess calls to Java CLI
- ❌ Python Flask: Would need to reimplement or call subprocess
- ❌ Go: Would need complete reimplementation

### Frontend: Next.js 14+ with ShadCN UI

**Why Next.js?**
- **React framework**: Built on React with added benefits
- **App Router**: Modern routing with server components
- **Performance**: Automatic optimization, code splitting
- **Server Actions**: Simplified API integration
- **SEO Ready**: Server-side rendering capabilities (if needed)
- **Production Ready**: Battle-tested by major companies

**Why ShadCN UI?**
- **Customizable**: Copy components into your codebase, full control
- **Beautiful dark mode**: Built-in dark mode support
- **Accessible**: Built on Radix UI primitives (WCAG compliant)
- **Tailwind CSS**: Utility-first styling, highly customizable
- **Modern**: Latest React patterns, server components compatible
- **No bloat**: Only install components you use

**Why Dark Mode Only?**
- **Developer focused**: OSCAL users are typically technical
- **Reduced complexity**: No theme switching logic needed
- **Better for code**: Easier to read code/errors in dark themes
- **Modern aesthetic**: Professional, developer-friendly appearance

**Why TypeScript?**
- Type safety reduces bugs
- Better IDE support with Next.js
- Self-documenting code
- Easier refactoring

### Code Editor: Monaco Editor

**Why Monaco?**
- Powers VS Code
- Excellent syntax highlighting
- Built-in diff viewer
- Error marker support
- Line-by-line error display
- Auto-completion support

### State Management: Zustand + React Server Components

**Why Zustand?**
- Simpler API than Redux, less boilerplate
- Excellent TypeScript support
- Perfect for client-side state
- Works seamlessly with Next.js App Router
- Easy to test

**React Server Components (RSC):**
- Use for initial data fetching
- Reduces client-side JavaScript
- Automatic code splitting
- Better performance

**Pattern:**
- Server Components: Initial page loads, static data
- Client Components: Interactive UI, real-time updates
- Zustand: Client state (file uploads, WebSocket, UI state)

## Component Design

### Backend Components

#### 1. ValidationController

```java
@RestController
@RequestMapping("/api/validate")
public class ValidationController {

    @Autowired
    private ValidationService validationService;

    @PostMapping
    public ResponseEntity<ValidationResult> validate(
        @RequestParam("file") MultipartFile file,
        @RequestParam("modelType") OscalModelType modelType) {

        ValidationResult result = validationService.validate(file, modelType);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch")
    public ResponseEntity<String> validateBatch(
        @RequestParam("files") List<MultipartFile> files,
        @RequestParam("modelType") OscalModelType modelType) {

        String operationId = validationService.validateBatchAsync(files, modelType);
        return ResponseEntity.accepted().body(operationId);
    }
}
```

#### 2. ValidationService

```java
@Service
public class ValidationService {

    @Autowired
    private OscalBindingContext bindingContext;

    @Autowired
    private NotificationService notificationService;

    public ValidationResult validate(MultipartFile file, OscalModelType modelType) {
        try {
            // Use existing OSCAL CLI validation logic
            IBoundLoader loader = bindingContext.newBoundLoader();
            loader.enableFeature(DeserializationFeature.DESERIALIZE_VALIDATE_CONSTRAINTS);

            Format format = detectFormat(file);
            Class<?> oscalClass = modelType.getOscalClass();

            IDocumentNodeItem nodeItem = loader.loadAsNodeItem(
                format,
                toInputSource(file)
            );

            // Collect validation results
            return buildValidationResult(nodeItem);

        } catch (Exception e) {
            return ValidationResult.error(e);
        }
    }

    @Async
    public String validateBatchAsync(List<MultipartFile> files, OscalModelType modelType) {
        String operationId = UUID.randomUUID().toString();

        files.forEach(file -> {
            ValidationResult result = validate(file, modelType);
            notificationService.sendProgress(operationId, result);
        });

        return operationId;
    }
}
```

#### 3. ConversionService

```java
@Service
public class ConversionService {

    @Autowired
    private OscalBindingContext bindingContext;

    public ConversionResult convert(
        MultipartFile file,
        Format sourceFormat,
        Format targetFormat,
        OscalModelType modelType) {

        try {
            // Load document
            IBoundLoader loader = bindingContext.newBoundLoader();
            IDocumentNodeItem document = loader.loadAsNodeItem(
                sourceFormat,
                toInputSource(file)
            );

            // Serialize to target format
            ISerializer<?> serializer = bindingContext.newSerializer(
                targetFormat,
                modelType.getOscalClass()
            );

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            serializer.serialize(document.getValue(), output);

            return ConversionResult.success(
                output.toByteArray(),
                targetFormat
            );

        } catch (Exception e) {
            return ConversionResult.error(e);
        }
    }
}
```

### Frontend Components

#### 1. FileUploader Component (ShadCN + react-dropzone)

```typescript
'use client'

import { useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import { Upload } from 'lucide-react'
import { cn } from '@/lib/utils'

interface FileUploaderProps {
  onFilesSelected: (files: File[]) => void
  accept?: string
  multiple?: boolean
  maxSize?: number
}

export function FileUploader({
  onFilesSelected,
  accept = '.xml,.json,.yaml,.yml',
  multiple = false,
  maxSize = 10 * 1024 * 1024 // 10MB
}: FileUploaderProps) {
  const onDrop = useCallback((acceptedFiles: File[]) => {
    onFilesSelected(acceptedFiles)
  }, [onFilesSelected])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'text/*': ['.xml', '.json', '.yaml', '.yml'] },
    multiple,
    maxSize
  })

  return (
    <div
      {...getRootProps()}
      className={cn(
        'border-2 border-dashed rounded-lg p-12 text-center cursor-pointer',
        'transition-colors duration-200',
        'hover:border-primary hover:bg-accent/50',
        isDragActive && 'border-primary bg-accent/50'
      )}
    >
      <input {...getInputProps()} />
      <Upload className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
      {isDragActive ? (
        <p className="text-lg">Drop files here...</p>
      ) : (
        <div>
          <p className="text-lg font-medium">
            Drag & drop OSCAL files, or click to browse
          </p>
          <p className="text-sm text-muted-foreground mt-2">
            Accepts: {accept} • Max size: {maxSize / 1024 / 1024}MB
          </p>
        </div>
      )}
    </div>
  )
}
```

#### 2. ValidationResultsPanel Component (ShadCN Card + Badge)

```typescript
'use client'

import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { CheckCircle2, XCircle, AlertTriangle } from 'lucide-react'
import { cn } from '@/lib/utils'

interface ValidationError {
  line: number
  column: number
  message: string
  severity: 'error' | 'warning'
  path: string
}

interface ValidationResultsPanelProps {
  result: {
    valid: boolean
    errors?: ValidationError[]
    warnings?: ValidationError[]
  }
  onErrorClick: (line: number) => void
}

export function ValidationResultsPanel({
  result,
  onErrorClick
}: ValidationResultsPanelProps) {
  return (
    <Card className="bg-card">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          {result.valid ? (
            <>
              <CheckCircle2 className="h-5 w-5 text-green-500" />
              Document is Valid
            </>
          ) : (
            <>
              <XCircle className="h-5 w-5 text-destructive" />
              Validation Errors ({result.errors?.length || 0})
            </>
          )}
        </CardTitle>
      </CardHeader>

      <CardContent className="space-y-2">
        {result.errors?.map((error, index) => (
          <Alert
            key={index}
            variant="destructive"
            className="cursor-pointer hover:bg-destructive/20"
            onClick={() => onErrorClick(error.line)}
          >
            <XCircle className="h-4 w-4" />
            <AlertDescription>
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium">Line {error.line}, Column {error.column}</p>
                  <p className="text-sm mt-1">{error.message}</p>
                  <p className="text-xs text-muted-foreground mt-1">{error.path}</p>
                </div>
                <Badge variant="destructive">Error</Badge>
              </div>
            </AlertDescription>
          </Alert>
        ))}

        {result.warnings?.map((warning, index) => (
          <Alert key={index} variant="default" className="border-yellow-500/50">
            <AlertTriangle className="h-4 w-4 text-yellow-500" />
            <AlertDescription>
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium">Line {warning.line}, Column {warning.column}</p>
                  <p className="text-sm mt-1">{warning.message}</p>
                </div>
                <Badge variant="outline" className="border-yellow-500 text-yellow-500">
                  Warning
                </Badge>
              </div>
            </AlertDescription>
          </Alert>
        ))}
      </CardContent>
    </Card>
  )
}
```

## Data Flow

### Validation Flow

```
User → Upload File → Frontend
                       ↓
              FormData (POST /api/validate)
                       ↓
              ValidationController
                       ↓
              ValidationService
                       ↓
         OscalBindingContext.validate()
                       ↓
              ValidationResult
                       ↓
              JSON Response → Frontend
                       ↓
              Display Results → User
```

### Async Batch Operation Flow

```
User → Upload Files → Frontend
                        ↓
            POST /api/batch/validate
                        ↓
            BatchController (returns operationId)
                        ↓
            202 Accepted → Frontend
                        ↓
    Frontend subscribes to WebSocket: /ws/operations/{operationId}
                        ↓
            BatchService processes files
                        ↓
    For each file: ValidationService.validate()
                        ↓
    WebSocket messages with progress
                        ↓
            Frontend updates UI in real-time
                        ↓
            All files complete
                        ↓
            Final status message → Frontend
                        ↓
            Display summary → User
```

## Security Architecture

### Authentication (Optional - Phase 4)

```
┌──────────────────────────────────────────────────┐
│  Spring Security + JWT Authentication            │
├──────────────────────────────────────────────────┤
│  1. User logs in with credentials                │
│  2. Server validates and issues JWT token        │
│  3. Frontend stores token in localStorage        │
│  4. All API requests include: Authorization:     │
│     Bearer {token}                                │
│  5. Server validates token on each request       │
└──────────────────────────────────────────────────┘
```

### File Upload Security

1. **Size limits**: Max 10MB per file, 50MB per batch
2. **Type validation**: Only accept .xml, .json, .yaml, .yml
3. **Content validation**: Validate file content before processing
4. **Temporary storage**: Auto-delete files after processing
5. **Sanitization**: Sanitize filenames to prevent directory traversal

### CORS Configuration

```java
@Configuration
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

## Performance Considerations

### Backend Optimizations

1. **Async Processing**: Long operations run asynchronously
   ```java
   @Async
   public CompletableFuture<ValidationResult> validateAsync(...)
   ```

2. **Thread Pool Configuration**:
   ```java
   @Configuration
   @EnableAsync
   public class AsyncConfig {
       @Bean
       public Executor taskExecutor() {
           ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
           executor.setCorePoolSize(4);
           executor.setMaxPoolSize(8);
           executor.setQueueCapacity(100);
           return executor;
       }
   }
   ```

3. **File Caching**: Cache parsed OSCAL documents for repeated operations

4. **Connection Pooling**: Reuse HTTP connections

### Frontend Optimizations

1. **Code Splitting**: Lazy load routes
   ```typescript
   const ValidatePage = lazy(() => import('./pages/ValidatePage'));
   ```

2. **Virtual Scrolling**: For large file lists and error lists

3. **Debouncing**: Debounce editor changes for real-time validation

4. **Web Workers**: Offload heavy processing (syntax highlighting, diff calculation)

5. **Progressive Loading**: Show UI immediately, load data progressively

## Scalability

### Horizontal Scaling

```
       Load Balancer
            │
    ┌───────┼───────┐
    │       │       │
  App1    App2    App3
    │       │       │
    └───────┼───────┘
            │
     Shared File Storage
     (NFS / S3 / Azure Blob)
```

For horizontal scaling:
1. Use external file storage (not local filesystem)
2. Use Redis for session storage
3. Use message queue (RabbitMQ/Kafka) for batch operations
4. Implement sticky sessions for WebSocket connections

### Vertical Scaling

- Increase JVM heap size: `-Xmx4g`
- Increase thread pool sizes
- Optimize garbage collection settings

### Caching Strategy

```java
@Cacheable(value = "catalogs", key = "#uri")
public Catalog loadCatalog(URI uri) {
    // Expensive operation
}

@CacheEvict(value = "catalogs", allEntries = true)
@Scheduled(fixedDelay = 3600000) // 1 hour
public void evictAllCaches() {
    // Periodic cache cleanup
}
```

## Deployment Architecture

### Single Instance Deployment

```
┌─────────────────────────────────────┐
│         Docker Container             │
│  ┌───────────────────────────────┐  │
│  │  Spring Boot Application      │  │
│  │  - Embedded Tomcat            │  │
│  │  - React Frontend (static)    │  │
│  │  - OSCAL CLI Libraries        │  │
│  └───────────────────────────────┘  │
│                                      │
│  Volumes:                            │
│  - /tmp/uploads (temporary files)   │
│  - /app/logs (application logs)     │
└─────────────────────────────────────┘
```

### Production Deployment

```
       HTTPS (443)
            │
      Nginx/Apache
            │
       Application Server(s)
            │
    Spring Boot App (8080)
            │
       File Storage
     (Local or Cloud)
```

## Monitoring & Observability

### Metrics to Track

1. **Application Metrics**:
   - Request rate (requests/sec)
   - Response time (p50, p95, p99)
   - Error rate
   - Active WebSocket connections

2. **Business Metrics**:
   - Validations performed
   - Conversions completed
   - Profile resolutions
   - File formats processed

3. **System Metrics**:
   - CPU usage
   - Memory usage
   - Disk I/O
   - Network I/O

### Implementation

```java
@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

@Service
public class ValidationService {

    @Timed(value = "validation.time", description = "Time taken to validate")
    public ValidationResult validate(...) {
        // Implementation
    }
}
```

## Error Handling Strategy

### Layered Error Handling

1. **Controller Layer**: Catch and transform exceptions
2. **Service Layer**: Business logic exceptions
3. **Integration Layer**: OSCAL library exceptions
4. **Global Handler**: Catch-all for unexpected errors

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        ValidationException ex) {

        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(ex.getMessage(), ex.getErrors()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
        Exception ex) {

        log.error("Unexpected error", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("An unexpected error occurred"));
    }
}
```

## Future Enhancements

1. **Persistent Storage**: Save documents and history to database
2. **User Accounts**: Multi-user support with workspaces
3. **Collaboration**: Real-time collaborative editing
4. **Version Control**: Track document changes over time
5. **Templates**: Predefined OSCAL templates
6. **Plugins**: Extensible validation rules
7. **Cloud Storage**: Integration with S3, Azure Blob, GCS
8. **CI/CD Integration**: Webhook support for pipelines
