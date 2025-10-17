# OSCAL CLI Web Interface - Implementation Roadmap

Detailed phased implementation plan for building the OSCAL CLI web interface with **Next.js**, **ShadCN UI**, and **Spring Boot API**.

## Table of Contents

- [Overview](#overview)
- [Phase 1: Foundation](#phase-1-foundation-weeks-1-3)
- [Phase 2: Core Features](#phase-2-core-features-weeks-4-6)
- [Phase 3: Advanced Features](#phase-3-advanced-features-weeks-7-9)
- [Phase 4: Polish & Deploy](#phase-4-polish--deploy-weeks-10-12)
- [Post-Launch](#post-launch)
- [Resource Requirements](#resource-requirements)

## Overview

**Total Timeline**: 12 weeks (3 months)
**Team Size**: 2-3 developers (1 backend, 1-2 frontend)
**Deployment Target**: Single JAR with embedded frontend + optional Docker container

### Success Criteria

- All CLI operations accessible via web interface
- Sub-second response for validation/conversion (< 1MB files)
- Batch operations support up to 10 files
- WCAG 2.1 AA accessibility compliance
- Mobile-responsive design
- < 100ms API response time (p95)

---

## Phase 1: Foundation (Weeks 1-3)

**Goal**: Set up development environment, basic project structure, and first working feature (validation)

### Week 1: Project Setup

#### Backend (Spring Boot)

- [ ] Create Spring Boot 3.x project with Maven
- [ ] Configure project structure:
  ```
  front-end/api/
  ├── src/main/java/gov/nist/secauto/oscal/web/
  │   ├── config/          # Configuration classes
  │   ├── controller/      # REST controllers
  │   ├── service/         # Business logic
  │   ├── adapter/         # OSCAL CLI integration
  │   ├── model/           # DTOs, request/response objects
  │   └── exception/       # Exception handling
  ├── src/main/resources/
  │   ├── application.yml
  │   └── log4j2.xml
  └── pom.xml
  ```
- [ ] Add dependencies:
  - Spring Web
  - Spring WebSocket
  - OSCAL CLI libraries (existing)
  - Lombok (optional)
  - JUnit 5
- [ ] Configure CORS for local development
- [ ] Set up logging (Log4j2)
- [ ] Create first REST controller (HealthController)
- [ ] Test: `GET /api/health` returns 200 OK

#### Frontend (Next.js)

- [ ] Initialize Next.js 14 project with TypeScript:
  ```bash
  npx create-next-app@latest oscal-cli-ui --typescript --tailwind --app
  ```
- [ ] Install ShadCN UI:
  ```bash
  npx shadcn-ui@latest init
  ```
- [ ] Configure dark mode only in `tailwind.config.ts`
- [ ] Install additional dependencies:
  ```bash
  npm install zustand react-dropzone @monaco-editor/react lucide-react
  ```
- [ ] Set up project structure:
  ```
  front-end/ui/
  ├── app/
  │   ├── layout.tsx       # Root layout
  │   ├── page.tsx         # Dashboard
  │   └── globals.css      # Global styles
  ├── components/
  │   ├── ui/              # ShadCN components
  │   └── layout/          # Layout components
  ├── lib/
  │   ├── utils.ts
  │   ├── api-client.ts
  │   └── stores/
  └── types/
      └── oscal.ts
  ```
- [ ] Create basic layout with header/footer
- [ ] Test: Dashboard page renders with dark theme

### Week 2: First API Endpoint - Validation

#### Backend

- [ ] Create `ValidationController` with `/api/validate` endpoint
- [ ] Create `ValidationService`:
  - Integrate with existing OSCAL CLI validation logic
  - Use `OscalBindingContext` directly (no subprocess)
  - Return structured validation results
- [ ] Create DTOs:
  - `ValidationRequest` (file, modelType, format)
  - `ValidationResult` (valid, errors[], warnings[])
  - `ValidationError` (line, column, message, severity, path)
- [ ] Add file upload handling (MultipartFile)
- [ ] Add format detection logic
- [ ] Implement error handling (GlobalExceptionHandler)
- [ ] Write unit tests for ValidationService
- [ ] Test with curl/Postman

#### Frontend

- [ ] Create `/app/validate/page.tsx`
- [ ] Implement ShadCN components:
  - `components/ui/card.tsx`
  - `components/ui/button.tsx`
  - `components/ui/select.tsx`
  - `components/ui/badge.tsx`
  - `components/ui/alert.tsx`
- [ ] Create custom components:
  - `components/file-uploader.tsx` (with react-dropzone)
  - `components/model-type-selector.tsx`
- [ ] Create API client (`lib/api-client.ts`)
- [ ] Implement file upload to `/api/validate`
- [ ] Display loading state during validation
- [ ] Test: Upload XML catalog, see validation success

### Week 3: Validation Results Display

#### Backend

- [ ] Enhance validation error messages with line numbers
- [ ] Add validation timing metrics
- [ ] Implement proper HTTP status codes
- [ ] Add API documentation comments

#### Frontend

- [ ] Install and configure Monaco Editor
- [ ] Create `components/code-editor.tsx`:
  - Dark theme configuration
  - Read-only mode for display
  - Error marker integration
  - Syntax highlighting (XML, JSON, YAML)
- [ ] Create `components/validation-results.tsx`:
  - Success state with checkmark
  - Error list with line numbers
  - Clickable errors that jump to line
  - Warning display
- [ ] Implement two-column layout:
  - Left: File content in Monaco Editor
  - Right: Validation results
- [ ] Add error click-to-line functionality
- [ ] Style with ShadCN dark theme
- [ ] Test: Validate invalid document, click error, editor jumps to line

**Phase 1 Deliverable**: Working validation feature accessible at `/validate`

---

## Phase 2: Core Features (Weeks 4-6)

**Goal**: Add conversion and profile resolution features, implement WebSocket for real-time updates

### Week 4: Conversion API & UI

#### Backend

- [ ] Create `ConversionController` with `/api/convert` endpoint
- [ ] Create `ConversionService`:
  - Use existing conversion logic from CLI
  - Support XML ↔ JSON ↔ YAML
  - Auto-detect source format
  - Return converted content as string
- [ ] Add file size validation (max 10MB)
- [ ] Add format validation
- [ ] Write tests for all format combinations
- [ ] Add download endpoint for converted files

#### Frontend

- [ ] Create `/app/convert/page.tsx`
- [ ] Implement side-by-side editor layout:
  - Left: Source file editor
  - Right: Converted file editor (read-only)
- [ ] Create `components/format-selector.tsx`
- [ ] Add conversion direction indicator (XML → JSON)
- [ ] Implement "Copy to Clipboard" button
- [ ] Implement "Download" button
- [ ] Add real-time conversion preview (debounced)
- [ ] Test: Convert catalog.xml to JSON, download result

### Week 5: Profile Resolution

#### Backend

- [ ] Create `ProfileController` with `/api/profile/resolve` endpoint
- [ ] Create `ProfileResolutionService`:
  - Use existing `ProfileResolver` from CLI
  - Handle remote catalog imports
  - Return resolved catalog
  - Track resolution progress
- [ ] Add timeout handling for remote fetches
- [ ] Add caching for frequently-accessed catalogs
- [ ] Handle resolution errors gracefully
- [ ] Add resolution summary (controls selected, imports processed)

#### Frontend

- [ ] Create `/app/resolve/page.tsx`
- [ ] Create `components/progress-indicator.tsx`:
  - Multi-step progress display
  - Current step highlighting
  - Estimated time remaining
- [ ] Implement resolution configuration:
  - Output format selector
  - Advanced options (collapsible)
- [ ] Display resolution progress:
  - Loading profile
  - Resolving imports
  - Selecting controls
  - Applying modifications
- [ ] Show resolution summary:
  - Controls selected count
  - Parameters modified
  - Imports processed
  - Resolution time
- [ ] Add resolved catalog preview
- [ ] Test: Resolve NIST 800-53 HIGH baseline profile

### Week 6: WebSocket Integration

#### Backend

- [ ] Configure Spring WebSocket support
- [ ] Create `WebSocketConfig`
- [ ] Create `OperationProgressHandler`:
  - Send progress updates
  - Send completion notifications
  - Handle connection lifecycle
- [ ] Create `NotificationService`:
  - Broadcast messages to specific operation IDs
  - Track active connections
- [ ] Integrate WebSocket into existing services
- [ ] Test with WebSocket client

#### Frontend

- [ ] Create `lib/websocket.ts`:
  - Connect to WebSocket endpoint
  - Subscribe to operation updates
  - Reconnection logic
  - Type-safe message handling
- [ ] Create Zustand store for WebSocket state
- [ ] Add real-time progress updates to:
  - Validation (for large files)
  - Conversion (for large files)
  - Profile resolution (always)
- [ ] Display WebSocket connection status
- [ ] Test: Long-running operation shows real-time progress

**Phase 2 Deliverable**: Validation, conversion, and profile resolution all working with real-time feedback

---

## Phase 3: Advanced Features (Weeks 7-9)

**Goal**: Batch operations, operation history, enhanced UX

### Week 7: Batch Operations Backend

#### Backend

- [ ] Create `BatchController`:
  - `/api/batch/validate` - POST
  - `/api/batch/convert` - POST
  - `/api/batch/status/{id}` - GET
  - `/api/batch/download/{id}` - GET (ZIP)
- [ ] Create `BatchOperationService`:
  - Generate unique operation IDs
  - Process files asynchronously (@Async)
  - Track progress for each file
  - Aggregate results
  - Create ZIP archives for downloads
- [ ] Configure thread pool for async operations:
  ```java
  @Configuration
  @EnableAsync
  public class AsyncConfig {
      @Bean
      public Executor taskExecutor() {
          ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
          executor.setCorePoolSize(4);
          executor.setMaxPoolSize(8);
          return executor;
      }
  }
  ```
- [ ] Implement WebSocket progress for batch:
  - Overall progress
  - Per-file status
  - Completion notification
- [ ] Add operation cleanup (auto-delete after 1 hour)
- [ ] Write tests for batch operations

### Week 8: Batch Operations Frontend

#### Frontend

- [ ] Create `/app/batch/page.tsx`:
  - Operation type selector (validate/convert)
  - Model type selector
  - Multi-file uploader
  - Configuration panel
- [ ] Enhance `FileUploader` for multiple files:
  - Show file list with individual remove buttons
  - Display total size
  - Limit to 10 files
- [ ] Create `components/batch-progress.tsx`:
  - Overall progress bar
  - Per-file status list with icons:
    - ⏸ Waiting (gray)
    - ⏳ Processing (blue, animated)
    - ✓ Success (green)
    - ✗ Error (red)
  - Real-time WebSocket updates
- [ ] Create `/app/batch/results/[id]/page.tsx`:
  - Detailed results for each file
  - Download individual results
  - Download all as ZIP
  - Summary statistics
- [ ] Implement batch operation flow:
  - Upload files → Start → Watch progress → View results
- [ ] Test: Batch validate 10 catalogs, all succeed

### Week 9: Operation History & Dashboard Enhancements

#### Backend

- [ ] Create `HistoryController`:
  - `/api/history` - GET (paginated)
  - `/api/history/{id}` - GET (detail)
  - `/api/history/{id}` - DELETE
- [ ] Create `OperationHistoryService`:
  - Store operation records in memory (or database)
  - Track: operation type, timestamp, files, results, duration
  - Pagination support
  - Search/filter by type, date, status
- [ ] Add operation retention policy (7 days)

#### Frontend

- [ ] Create `/app/history/page.tsx`:
  - Paginated list of past operations
  - Filters: operation type, date range, status
  - Search by filename
  - Actions: View details, Repeat, Delete
- [ ] Create `components/operation-history-item.tsx`:
  - Operation type badge
  - Status icon
  - Timestamp (relative: "2 minutes ago")
  - Quick action buttons
- [ ] Enhance Dashboard (`app/page.tsx`):
  - Recent operations list (last 5)
  - Quick stats: operations today, success rate
  - Quick action cards with hover effects
- [ ] Add "Repeat Operation" feature:
  - Load previous settings
  - Pre-fill form
  - One-click re-run
- [ ] Test: Run operations, view in history, repeat an operation

**Phase 3 Deliverable**: Full-featured web interface with batch operations and history

---

## Phase 4: Polish & Deploy (Weeks 10-12)

**Goal**: Production-ready application with documentation, testing, and deployment

### Week 10: Testing & Bug Fixes

#### Backend

- [ ] Achieve > 80% code coverage:
  - Unit tests for all services
  - Integration tests for controllers
  - Mock OSCAL operations where needed
- [ ] Load testing with JMeter:
  - 100 concurrent users
  - Various file sizes
  - Identify bottlenecks
- [ ] Fix identified bugs
- [ ] Optimize slow operations
- [ ] Add rate limiting (optional)

#### Frontend

- [ ] Write component tests (Jest + React Testing Library):
  - FileUploader
  - ValidationResults
  - CodeEditor integration
  - All ShadCN component usage
- [ ] E2E tests with Playwright:
  - Complete validation flow
  - Complete conversion flow
  - Batch operation flow
- [ ] Accessibility audit:
  - Run axe-core
  - Fix all critical issues
  - Verify keyboard navigation
  - Test with screen reader
- [ ] Cross-browser testing:
  - Chrome, Firefox, Safari, Edge
  - Fix any rendering issues
- [ ] Mobile testing:
  - Test responsive layouts
  - Fix touch interactions
- [ ] Performance optimization:
  - Analyze bundle size
  - Implement code splitting
  - Lazy load Monaco Editor
  - Optimize images

### Week 11: Documentation & Deployment Setup

#### Backend

- [ ] Add Swagger/OpenAPI documentation:
  - Install springdoc-openapi
  - Annotate all endpoints
  - Generate API docs at `/swagger-ui.html`
- [ ] Write deployment guide:
  - JVM requirements
  - Environment variables
  - Configuration options
  - Monitoring setup
- [ ] Create Docker image:
  ```dockerfile
  FROM eclipse-temurin:11-jre
  COPY target/oscal-cli-web.jar app.jar
  EXPOSE 8080
  ENTRYPOINT ["java", "-jar", "/app.jar"]
  ```
- [ ] Create docker-compose.yml for easy local setup
- [ ] Add health check endpoints:
  - `/actuator/health`
  - `/actuator/metrics`
- [ ] Configure production logging

#### Frontend

- [ ] Build and optimize for production:
  ```bash
  npm run build
  ```
- [ ] Embed frontend in Spring Boot:
  - Copy Next.js build output to `src/main/resources/static`
  - Configure Spring to serve static files
  - Set up fallback routing for SPA
- [ ] Test embedded deployment
- [ ] Write user documentation:
  - Getting Started guide
  - Feature tutorials with screenshots
  - Troubleshooting section
  - API usage examples
- [ ] Create video walkthrough (optional)

### Week 12: Final Polish & Launch

- [ ] Security hardening:
  - CSRF protection
  - File upload validation
  - Input sanitization
  - HTTPS enforcement (production)
- [ ] Performance baseline:
  - Document API response times
  - Document batch operation times
  - Set up monitoring (if deploying to cloud)
- [ ] Final testing:
  - Run full E2E test suite
  - Manual testing of all features
  - Load testing with realistic scenarios
- [ ] Create release notes
- [ ] Tag release: `v1.0.0-web`
- [ ] Deploy to staging environment
- [ ] User acceptance testing
- [ ] Deploy to production
- [ ] Announce release

**Phase 4 Deliverable**: Production-ready web interface with full documentation

---

## Post-Launch

### Immediate Post-Launch (Weeks 13-14)

- [ ] Monitor error rates and performance
- [ ] Gather user feedback
- [ ] Create GitHub issues for enhancement requests
- [ ] Hot-fix any critical bugs
- [ ] Update documentation based on feedback

### Future Enhancements (Phase 5+)

#### High Priority
- [ ] **Authentication & Authorization**:
  - User accounts
  - JWT authentication
  - Role-based access control
  - API key management

- [ ] **Persistent Storage**:
  - Database for operation history
  - Save favorite operations
  - Document templates

- [ ] **Advanced Features**:
  - Document comparison (diff viewer)
  - Catalog search and filter
  - Custom validation rules
  - Schema generation

#### Medium Priority
- [ ] **Collaboration**:
  - Share operations with team
  - Comments on documents
  - Real-time collaborative editing

- [ ] **Integrations**:
  - GitHub integration (validate on PR)
  - CI/CD webhooks
  - Cloud storage (S3, Azure Blob, GCS)
  - Slack/Teams notifications

- [ ] **Enhanced UX**:
  - Command palette (⌘K)
  - Keyboard shortcuts
  - Undo/redo
  - Auto-save drafts
  - Dark theme variations

#### Low Priority
- [ ] **Enterprise Features**:
  - SSO integration
  - Audit logging
  - Custom branding
  - On-premise deployment guide

---

## Resource Requirements

### Development Team

- **1 Backend Developer** (Java/Spring Boot):
  - Weeks 1-12: Full-time
  - Skills: Spring Boot, OSCAL, REST APIs, WebSocket

- **1-2 Frontend Developers** (Next.js/React):
  - Weeks 1-12: Full-time
  - Skills: Next.js, TypeScript, ShadCN UI, Tailwind CSS

- **0.5 DevOps Engineer** (optional):
  - Weeks 11-12: Part-time
  - Skills: Docker, CI/CD, Cloud deployment

### Infrastructure

#### Development
- Local development machines
- GitHub repository
- CI/CD pipeline (GitHub Actions)

#### Staging
- 1 VM or container (2 vCPU, 4GB RAM)
- 20GB storage
- Public URL for testing

#### Production
- 2+ VMs or containers (for HA)
- Load balancer
- 50GB storage
- CDN for static assets (optional)
- Monitoring (Prometheus, Grafana, or cloud equivalent)

### Estimated Costs

**Development** (3 months):
- 2-3 developers × 3 months × $150/hr (estimate)
- Infrastructure: Minimal (use free tiers)

**Production** (monthly):
- Hosting: $50-200/month (depending on provider)
- Monitoring: $0-50/month
- Total: $50-250/month

### Dependencies

- Java 11+ runtime
- Node.js 18+ for frontend development
- Maven 3.8+
- Docker (optional, for containerized deployment)

---

## Success Metrics

### Technical Metrics
- ✅ API response time < 100ms (p95)
- ✅ Validation < 1 second for 1MB files
- ✅ Batch operations support 10 files
- ✅ Zero critical security vulnerabilities
- ✅ 80%+ test coverage

### User Metrics
- ✅ 90%+ operation success rate
- ✅ < 5% error rate
- ✅ Average operation time reduction by 80% vs CLI
- ✅ Mobile usability score > 90

### Business Metrics
- ✅ 50+ active users in first month
- ✅ 90% user satisfaction (surveys)
- ✅ 10+ API integrations (if authentication added)

---

## Risk Mitigation

### Technical Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| OSCAL library integration issues | High | Low | Use existing CLI code directly, proven approach |
| Performance issues with large files | Medium | Medium | Implement file size limits, optimize parsing |
| WebSocket connection stability | Medium | Low | Implement reconnection logic, fallback to polling |
| Browser compatibility issues | Low | Low | Test early on multiple browsers |

### Project Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Scope creep | High | Medium | Strict phase gates, defer non-critical features |
| Resource unavailability | High | Low | Document thoroughly, cross-train team |
| Delayed dependency updates | Low | Low | Use stable versions, plan upgrade path |

---

## Conclusion

This roadmap provides a structured 12-week plan to deliver a fully functional, production-ready web interface for OSCAL CLI. The phased approach ensures continuous delivery of value while managing complexity.

**Next Steps**:
1. Secure development team and resources
2. Set up project repositories and infrastructure
3. Begin Phase 1: Week 1 tasks
4. Schedule weekly progress reviews
5. Adjust timeline based on actual progress

For questions or clarifications, refer to the other documentation files:
- [ARCHITECTURE.md](ARCHITECTURE.md) - Technical architecture
- [API-SPECIFICATION.md](API-SPECIFICATION.md) - API endpoints
- [FRONTEND-DESIGN.md](FRONTEND-DESIGN.md) - UI/UX design
