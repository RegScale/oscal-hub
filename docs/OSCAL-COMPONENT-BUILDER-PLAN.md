# OSCAL Component Builder - Implementation Plan

**Date**: October 21, 2025
**Status**: Planning Phase 📋
**Target**: Visual OSCAL Component Definition Builder with Azure Blob Storage

---

## Table of Contents
1. [Overview](#overview)
2. [Feature Requirements](#feature-requirements)
3. [Architecture & Design](#architecture--design)
4. [Database Schema](#database-schema)
5. [Azure Blob Storage Integration](#azure-blob-storage-integration)
6. [Implementation Phases](#implementation-phases)
7. [UI/UX Design](#uiux-design)
8. [API Endpoints](#api-endpoints)
9. [Testing Strategy](#testing-strategy)
10. [Future Enhancements](#future-enhancements)

---

## Overview

The OSCAL Component Builder is a visual tool for creating, managing, and storing OSCAL Component Definition documents. It provides:

- **Visual Builder**: Intuitive UI for constructing component definitions without manual JSON editing
- **Reusable Elements**: Library system for roles, parties, links, and back matter that can be saved and reused
- **Azure Blob Storage**: Cloud-based storage for built components in a dedicated "build" folder
- **OSCAL v1.1.3 Compliance**: Full support for OSCAL Component Definition schema

### What is an OSCAL Component Definition?

A Component Definition describes how a technology component (software, hardware, service, policy, process) implements security controls. It maps component capabilities to specific controls from catalogs like NIST SP 800-53.

**Example Use Case**: Django framework implements AC-3 (Access Enforcement), AC-7 (Unsuccessful Logon Attempts), etc.

---

## Feature Requirements

### Functional Requirements

1. **Component Definition Builder**
   - Create new component definitions from scratch
   - Edit existing component definitions
   - Delete component definitions
   - Export as JSON
   - Validate against OSCAL schema

2. **Reusable Element Library**
   - **Roles**: Define organizational roles (creator, approver, etc.)
   - **Parties**: Define organizations and individuals
   - **Links**: Define relationships and references
   - **Back Matter**: Resources, citations, and supporting information
   - Save elements to personal library
   - Reuse elements across multiple components

3. **Component Management**
   - Add multiple components to a definition
   - Define component type (software, hardware, service, policy, process, plan, guidance, standard, validation)
   - Add component properties and links
   - Manage control implementations

4. **Control Implementation**
   - Select control source (NIST SP 800-53 rev4/rev5, etc.)
   - Add implemented requirements (which controls the component satisfies)
   - Define implementation status and descriptions
   - Add properties to requirements

5. **Azure Blob Storage Integration**
   - Upload built components to Azure Blob Storage
   - Organize in "build" folder structure
   - Download components from Azure
   - List and manage stored components

### Non-Functional Requirements

- **Usability**: Intuitive wizard-based interface
- **Performance**: Handle large component definitions (100+ control implementations)
- **Validation**: Real-time OSCAL schema validation
- **Security**: Role-based access to Azure storage
- **Scalability**: Support multiple users building components simultaneously

---

## Architecture & Design

### System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (Next.js)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Build Page   │  │ Component    │  │ Element      │     │
│  │ (Dashboard)  │  │ Builder      │  │ Library      │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└───────────────────────────┬─────────────────────────────────┘
                            │ REST API
┌───────────────────────────▼─────────────────────────────────┐
│                   Backend (Spring Boot)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Component    │  │ Element      │  │ Azure Blob   │     │
│  │ Controller   │  │ Library Ctrl │  │ Controller   │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │              │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐     │
│  │ Component    │  │ Element      │  │ Azure Blob   │     │
│  │ Service      │  │ Library Svc  │  │ Service      │     │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘     │
│         │                  │                                 │
│  ┌──────▼───────┐  ┌──────▼───────┐                        │
│  │ Component    │  │ Element      │                        │
│  │ Repository   │  │ Library Repo │                        │
│  └──────────────┘  └──────────────┘                        │
└───────────────────────────┬─────────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            │                               │
┌───────────▼──────────┐      ┌────────────▼────────────┐
│  H2 Database         │      │  Azure Blob Storage     │
│  - Components        │      │  - build/               │
│  - Element Library   │      │    - user1/             │
│  - Users             │      │      - component-1.json │
└──────────────────────┘      │      - component-2.json │
                              │    - user2/             │
                              │      - ...              │
                              └─────────────────────────┘
```

### Technology Stack

**Frontend**:
- Next.js 15 (React framework)
- TypeScript
- Tailwind CSS
- React Hook Form (form management)
- Zod (validation)
- Monaco Editor (optional: for JSON preview)

**Backend**:
- Spring Boot 3.x
- Spring Data JPA
- Azure Storage SDK for Java
- Jackson (JSON processing)
- Hibernate Validator

**Storage**:
- H2 Database (component metadata, element library)
- Azure Blob Storage (actual component JSON files)

---

## Database Schema

### 1. Component Definition Entity

```java
@Entity
@Table(name = "component_definitions")
public class ComponentDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uuid;  // OSCAL UUID

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String version;
    private String oscalVersion;

    @Column(name = "azure_blob_path")
    private String azureBlobPath;  // Path in Azure: build/{username}/{filename}

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // JSON content stored in Azure, not DB
    // Metadata only in DB for listing/searching
}
```

### 2. Reusable Element Library

```java
@Entity
@Table(name = "reusable_elements")
public class ReusableElement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ElementType type;  // ROLE, PARTY, LINK, BACK_MATTER, RESPONSIBLE_PARTY

    @Column(nullable = false)
    private String name;  // User-friendly name

    @Column(columnDefinition = "TEXT", nullable = false)
    private String jsonContent;  // Actual JSON snippet

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_shared")
    private boolean isShared = false;  // Future: allow sharing elements

    public enum ElementType {
        ROLE,
        PARTY,
        LINK,
        BACK_MATTER,
        RESPONSIBLE_PARTY
    }
}
```

### 3. Component Usage Tracking (Optional)

```java
@Entity
@Table(name = "component_usage")
public class ComponentUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "component_id")
    private Long componentId;

    @Column(name = "downloaded_at")
    private LocalDateTime downloadedAt;

    @Column(name = "downloaded_by")
    private String downloadedBy;

    private String action;  // CREATED, UPDATED, DOWNLOADED, DELETED
}
```

---

## Azure Blob Storage Integration

### Storage Structure

```
oscal-build-storage (container)
└── build/
    ├── user@example.com/
    │   ├── django-component-v1.json
    │   ├── nginx-component-v1.json
    │   └── mysql-component-v2.json
    └── another-user@example.com/
        └── ...
```

### Configuration

**application.properties**:
```properties
# Azure Storage Configuration
azure.storage.connection-string=${AZURE_STORAGE_CONNECTION_STRING}
azure.storage.container.name=oscal-build-storage
azure.storage.build.folder=build
```

### Azure Blob Service Implementation

```java
@Service
public class AzureBlobService {

    private final BlobServiceClient blobServiceClient;
    private final String containerName;

    public String uploadComponent(String username, String filename, String jsonContent) {
        // Upload to: build/{username}/{filename}
    }

    public String downloadComponent(String blobPath) {
        // Download from Azure Blob
    }

    public List<BlobItem> listUserComponents(String username) {
        // List all components for a user
    }

    public void deleteComponent(String blobPath) {
        // Delete component from Azure
    }
}
```

---

## Implementation Phases

### Phase 1: Foundation (Week 1) ✅

**Goal**: Set up basic infrastructure and database entities

**Backend Tasks**:
- [ ] Create `ComponentDefinition` entity
- [ ] Create `ReusableElement` entity
- [ ] Create repositories for both entities
- [ ] Set up Azure Blob Storage configuration
- [ ] Implement `AzureBlobService` with basic CRUD operations
- [ ] Create DTOs for API requests/responses

**Frontend Tasks**:
- [ ] Add "Build" tile to main dashboard
- [ ] Create `/build` route and page structure
- [ ] Set up TypeScript types for component definition
- [ ] Create basic layout for build page

**Deliverables**:
- Database entities and repositories working
- Azure Blob Storage connection established
- Build page accessible from dashboard

---

### Phase 2: Reusable Element Library (Week 1-2)

**Goal**: Create library system for roles, parties, links, back matter

**Backend Tasks**:
- [ ] Implement `ElementLibraryService`
  - `createElement()`
  - `updateElement()`
  - `deleteElement()`
  - `getUserElements()`
  - `getElementByType()`
- [ ] Create `ElementLibraryController` with REST endpoints
- [ ] Add validation for element JSON structures

**Frontend Tasks**:
- [ ] Create `ElementLibraryManager` component
  - List all saved elements grouped by type
  - Add new element dialog
  - Edit existing element
  - Delete element
  - Search/filter elements
- [ ] Create form components for each element type:
  - `RoleForm`: id, title, short-name, description
  - `PartyForm`: uuid, type, name, email, addresses
  - `LinkForm`: href, rel, text
  - `BackMatterForm`: resources, citations
- [ ] Add element preview (JSON view)

**Deliverables**:
- Fully functional element library
- Users can create, save, and reuse elements

---

### Phase 3: Component Builder Wizard (Week 2-3)

**Goal**: Visual wizard for building component definitions

**Frontend Tasks**:
- [ ] Create `ComponentBuilderWizard` component with steps:
  1. **Metadata**: title, version, oscal-version, published date
  2. **Select Reusable Elements**: Choose roles, parties, links from library
  3. **Components**: Add one or more components
  4. **Component Details**: For each component:
     - Type, title, description
     - Control implementations
  5. **Control Implementation**: For each component:
     - Source (catalog URL)
     - Implemented requirements (control-id, description, props)
  6. **Review**: Preview full JSON structure
  7. **Save**: Name and save to Azure

- [ ] Create sub-components:
  - `MetadataForm`
  - `ElementSelector` (multi-select from library)
  - `ComponentForm`
  - `ControlImplementationForm`
  - `ImplementedRequirementForm`
  - `JsonPreview` (read-only Monaco editor)

**Backend Tasks**:
- [ ] Implement `ComponentService`:
  - `buildComponentJson()` - constructs OSCAL JSON from form data
  - `validateComponent()` - validates against OSCAL schema
  - `saveComponent()` - saves to DB and Azure
- [ ] Implement `ComponentController`:
  - `POST /api/build/components` - create component
  - `PUT /api/build/components/{id}` - update component
  - `GET /api/build/components` - list user's components
  - `GET /api/build/components/{id}` - get component details
  - `DELETE /api/build/components/{id}` - delete component
  - `POST /api/build/components/{id}/download` - download JSON

**Deliverables**:
- Complete wizard for building components
- Component saved to Azure Blob Storage
- List view of user's components

---

### Phase 4: Import & Edit Existing Components (Week 3-4)

**Goal**: Allow users to import and edit existing component definitions

**Backend Tasks**:
- [ ] Implement import functionality:
  - `POST /api/build/components/import` - upload existing JSON
  - Parse and validate OSCAL component
  - Extract metadata and save to DB
  - Store full JSON in Azure
- [ ] Add edit mode to `ComponentService`
  - Load existing component from Azure
  - Parse into editable form structure
  - Update and re-save

**Frontend Tasks**:
- [ ] Create import dialog:
  - File upload
  - Paste JSON
  - URL import (fetch from URL)
- [ ] Add "Edit" functionality to existing components
  - Load component into wizard
  - Pre-populate all fields
  - Save updates

**Deliverables**:
- Users can import existing component definitions
- Users can edit components created in the builder

---

### Phase 5: Advanced Features (Week 4+)

**Goal**: Enhance builder with advanced capabilities

**Features**:
- [ ] **Catalog Integration**:
  - Load NIST SP 800-53 rev4/rev5 catalogs
  - Search controls by ID or keyword
  - Auto-complete control-id fields

- [ ] **Component Templates**:
  - Pre-built templates for common software (Django, nginx, etc.)
  - Clone existing components as templates

- [ ] **Bulk Operations**:
  - Duplicate component definitions
  - Export multiple components
  - Batch delete

- [ ] **Collaboration**:
  - Share components with other users
  - Export reusable elements for team sharing

- [ ] **Validation & Quality**:
  - Real-time validation feedback
  - Completeness checker (missing required fields)
  - Control coverage analysis

**Deliverables**:
- Enhanced builder with professional features
- Better UX for managing complex components

---

## UI/UX Design

### Main Build Page Layout

```
┌─────────────────────────────────────────────────────────────┐
│  OSCAL Tools                                    [User Menu]  │
├─────────────────────────────────────────────────────────────┤
│  ← Back to Dashboard                                         │
│                                                               │
│  🔧 Build Components                                         │
│  Create OSCAL component definitions visually                 │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Tabs: [My Components] [Element Library]            │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                               │
│  [My Components Tab]                                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  [+ New Component]           [Import Component]     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Component List                                       │   │
│  │ ┌─────────────────────────────────────────────────┐ │   │
│  │ │ Django Framework v1.0                  [Edit] […]│ │   │
│  │ │ Created: Oct 15, 2025 | 9 controls            │ │   │
│  │ └─────────────────────────────────────────────────┘ │   │
│  │ ┌─────────────────────────────────────────────────┐ │   │
│  │ │ Nginx Web Server v2.1                 [Edit] […]│ │   │
│  │ │ Created: Oct 12, 2025 | 15 controls           │ │   │
│  │ └─────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                               │
│  [Element Library Tab]                                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  [+ Add Element] ▼                                   │   │
│  │  ┌────────┬────────┬────────┬────────┐             │   │
│  │  │ Roles  │Parties │ Links  │BackMttr│             │   │
│  │  └────────┴────────┴────────┴────────┘             │   │
│  │                                                      │   │
│  │  Roles (5)                                          │   │
│  │  • Creator                           [Edit][Delete] │   │
│  │  • Approver                          [Edit][Delete] │   │
│  │  • Maintainer                        [Edit][Delete] │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Component Builder Wizard

```
┌─────────────────────────────────────────────────────────────┐
│  Create Component Definition                        [Close]  │
├─────────────────────────────────────────────────────────────┤
│  Progress: [1●][2○][3○][4○][5○][6○][7○]                    │
│            ▲                                                  │
│         Metadata  Elements  Components  Controls  Review  Save
│                                                               │
│  Step 1: Metadata                                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Component Definition Title *                         │   │
│  │ [Django Web Framework                            ]   │   │
│  │                                                      │   │
│  │ Version *                  OSCAL Version *          │   │
│  │ [20240512      ]           [1.1.3               ]   │   │
│  │                                                      │   │
│  │ Published Date                                       │   │
│  │ [2025-10-21                                      ]   │   │
│  │                                                      │   │
│  │ Description                                          │   │
│  │ [This component definition describes...          ]   │   │
│  │ [                                                 ]   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                               │
│                           [Back]  [Next: Select Elements →]  │
└─────────────────────────────────────────────────────────────┘
```

---

## API Endpoints

### Component Endpoints

```
POST   /api/build/components                 - Create new component definition
GET    /api/build/components                 - List user's components
GET    /api/build/components/{id}            - Get component by ID
PUT    /api/build/components/{id}            - Update component
DELETE /api/build/components/{id}            - Delete component
POST   /api/build/components/import          - Import existing JSON
GET    /api/build/components/{id}/download   - Download JSON file
POST   /api/build/components/{id}/validate   - Validate component
```

### Element Library Endpoints

```
POST   /api/build/elements                   - Create reusable element
GET    /api/build/elements                   - Get all user's elements
GET    /api/build/elements?type=ROLE         - Get elements by type
GET    /api/build/elements/{id}              - Get element by ID
PUT    /api/build/elements/{id}              - Update element
DELETE /api/build/elements/{id}              - Delete element
```

### Azure Blob Endpoints

```
POST   /api/build/azure/upload               - Upload to Azure (internal)
GET    /api/build/azure/download/{path}      - Download from Azure
GET    /api/build/azure/list                 - List user's blobs
DELETE /api/build/azure/{path}               - Delete blob
```

---

## Testing Strategy

### Backend Testing

**Unit Tests**:
- Test `ComponentService` JSON building logic
- Test `ElementLibraryService` CRUD operations
- Test `AzureBlobService` upload/download (mocked)
- Test validation logic

**Integration Tests**:
- Test complete component creation flow
- Test Azure Blob Storage integration (with test container)
- Test element library with database

**Example Test**:
```java
@Test
public void testBuildComponentJson() {
    ComponentDefinitionRequest request = new ComponentDefinitionRequest();
    request.setTitle("Django");
    request.setVersion("1.0");
    // ... set all fields

    String json = componentService.buildComponentJson(request);

    assertNotNull(json);
    assertTrue(json.contains("\"component-definition\""));
    // Validate against OSCAL schema
}
```

### Frontend Testing

**Component Tests**:
- Test wizard navigation
- Test form validation
- Test element selection
- Test JSON preview

**E2E Tests** (Playwright):
- Complete flow: create component → save → download
- Import existing component
- Create and use reusable elements

---

## Future Enhancements

1. **Multi-Component Definitions**: Support multiple components in one definition
2. **Version Control**: Track component versions and changes over time
3. **Export Formats**: Support XML and YAML in addition to JSON
4. **Control Catalog Browser**: Integrated catalog viewer
5. **AI Assistance**: Suggest controls based on component description
6. **Compliance Checking**: Verify component meets baseline requirements
7. **Team Workspaces**: Shared folders for team collaboration
8. **Component Marketplace**: Public repository of community components

---

## Summary

This plan provides a comprehensive roadmap for building the OSCAL Component Builder. The phased approach allows for:

1. **Quick wins** with basic functionality (Phase 1-2)
2. **Core value** with the visual builder (Phase 3)
3. **Professional features** with import/edit (Phase 4)
4. **Enterprise readiness** with advanced features (Phase 5)

**Estimated Timeline**: 4-5 weeks for Phases 1-4, ongoing for Phase 5

**Next Steps**:
1. Review and approve this plan
2. Set up Azure Storage account and connection string
3. Begin Phase 1 implementation
4. Iterate based on user feedback

---

**Questions? Feedback?** Let's discuss before we start building!
