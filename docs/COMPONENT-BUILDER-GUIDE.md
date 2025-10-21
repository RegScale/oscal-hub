# OSCAL Component Builder Feature Guide

**Status:** Phase 3 Complete - Wizard Implementation
**Date:** October 21, 2025
**OSCAL Version:** 1.1.3

## Overview

The OSCAL Component Builder is a visual web interface for creating OSCAL Component Definition documents without writing JSON manually. It provides a step-by-step wizard that guides users through the process of creating compliant OSCAL component definitions.

## Architecture

### Storage Strategy

The Component Builder uses a two-tier storage approach:

1. **Metadata Storage** - H2 Database
   - Component definition metadata (title, version, counts)
   - User ownership and sharing information
   - Quick search and filtering capabilities

2. **Content Storage** - Azure Blob Storage
   - Full OSCAL JSON content
   - Organized by user: `build/{username}/{filename}.json`
   - Container: `oscal-build-storage`

### Backend Components

#### Entities

**ComponentDefinition** (`ComponentDefinition.java`)
- Stores metadata about component definitions
- Fields: UUID, title, version, OSCAL version, blob path, component count, control count
- Relationships: Many-to-one with User

**ReusableElement** (`ReusableElement.java`)
- Stores reusable OSCAL elements
- Types: ROLE, PARTY, LINK, BACK_MATTER, RESPONSIBLE_PARTY
- Features: Usage tracking, sharing, JSON content storage

#### Services

**ComponentDefinitionService** (`ComponentDefinitionService.java:56-98`)
- CRUD operations for component definitions
- Azure Blob Storage integration for JSON content
- Automatic OSCAL UUID generation
- Statistics and search functionality

**ReusableElementService** (`ReusableElementService.java:48-154`)
- CRUD operations for reusable elements
- Usage tracking and analytics
- Search and filtering by type
- Most-used and recent element queries

**ComponentBlobStorageService** (`ComponentBlobStorageService.java`)
- Azure Blob Storage operations for component definitions
- Blob path management: `build/{username}/{filename}.json`
- Metadata attachment to blobs

#### REST APIs

**Component Definitions API** (`/api/build/components`)

```
POST   /api/build/components              - Create new component definition
PUT    /api/build/components/{id}         - Update component definition
GET    /api/build/components/{id}         - Get by database ID
GET    /api/build/components/uuid/{uuid}  - Get by OSCAL UUID
GET    /api/build/components/{id}/content - Get full JSON content
GET    /api/build/components              - Get user's components
GET    /api/build/components/search       - Search with filters
DELETE /api/build/components/{id}         - Delete component definition
GET    /api/build/components/recent       - Get recent components
GET    /api/build/components/statistics   - Get user statistics
HEAD   /api/build/components/exists       - Check if exists by UUID
```

**Reusable Elements API** (`/api/build/elements`)

```
POST   /api/build/elements                - Create reusable element
PUT    /api/build/elements/{id}           - Update element
GET    /api/build/elements/{id}           - Get by ID
GET    /api/build/elements                - Get user's elements
GET    /api/build/elements/type/{type}    - Get by element type
GET    /api/build/elements/search         - Search with filters
DELETE /api/build/elements/{id}           - Delete element
GET    /api/build/elements/recent         - Get recent elements
GET    /api/build/elements/most-used      - Get most used elements
GET    /api/build/elements/statistics     - Get statistics
POST   /api/build/elements/{id}/use       - Increment use count
```

### Frontend Components

#### ComponentBuilderWizard (`ComponentBuilderWizard.tsx`)

A 4-step wizard for creating OSCAL Component Definitions:

**Step 1: Metadata**
- Component Definition Title (required)
- Description
- Version (default: 1.0.0)
- OSCAL Version (dropdown: 1.1.3, 1.1.2, 1.1.1, 1.1.0)

**Step 2: Components**
- Add/remove system components
- Component type selection (software, hardware, service, policy, etc.)
- Component title and description
- Visual card-based interface

**Step 3: Control Implementations (Optional)**
- Add control implementation sets
- Source catalog reference (e.g., NIST SP 800-53 Rev 5)
- Implemented requirements with control IDs
- Description for each implementation

**Step 4: Review & Save**
- Summary statistics (title, component count, control count)
- Full OSCAL JSON preview
- Save to Azure Blob Storage
- Success/error feedback

#### ElementLibrary (`ElementLibrary.tsx`)

Manages reusable OSCAL elements:

**Features:**
- Search by name or description
- Filter by element type (ROLE, PARTY, LINK, etc.)
- Create/edit/delete elements
- Visual cards with usage statistics
- Share elements with other users
- JSON validation on input

**Element Types:**
- ROLE - Defined functions or positions
- PARTY - Organizations or persons
- LINK - References to resources
- BACK_MATTER - Supporting materials
- RESPONSIBLE_PARTY - Parties with specific roles

#### ElementModal (`ElementModal.tsx:46-293`)

Dialog for creating/editing reusable elements:

**Fields:**
- Element Type (cannot change after creation)
- Name (required)
- Description
- JSON Content (validated)
- Share with other users (checkbox)

**Features:**
- Real-time JSON validation
- Type-specific descriptions
- Error handling and display
- Loading states during save

## OSCAL JSON Structure

The wizard generates valid OSCAL 1.1.3 Component Definition JSON:

```json
{
  "component-definition": {
    "uuid": "generated-uuid",
    "metadata": {
      "title": "Django Web Framework Security Controls",
      "last-modified": "2025-10-21T18:00:00Z",
      "version": "1.0.0",
      "oscal-version": "1.1.3",
      "description": "Optional description"
    },
    "components": [
      {
        "uuid": "component-uuid",
        "type": "software",
        "title": "Django Web Framework",
        "description": "Open-source web framework"
      }
    ],
    "control-implementations": [
      {
        "uuid": "impl-uuid",
        "source": "https://doi.org/10.6028/NIST.SP.800-53r5",
        "description": "NIST 800-53 Rev 5 controls",
        "implemented-requirements": [
          {
            "uuid": "req-uuid",
            "control-id": "AC-1",
            "description": "Access Control Policy"
          }
        ]
      }
    ]
  }
}
```

## User Workflow

### Creating a Component Definition

1. **Navigate to Builder**
   - Go to http://localhost:3000/build
   - Click "Create New" tab

2. **Step 1: Enter Metadata**
   - Enter a descriptive title
   - Optionally add description
   - Set version (or keep default 1.0.0)
   - Select OSCAL version (1.1.3 recommended)
   - Click "Next"

3. **Step 2: Add Components**
   - Click "Add Component"
   - Select component type from dropdown
   - Enter component title (required)
   - Add description
   - Repeat for all components
   - Click "Next"

4. **Step 3: Map Controls (Optional)**
   - Click "Add Control Implementation"
   - Enter source catalog URL or identifier
   - Add description
   - Click "Add Requirement" for each control
   - Enter control ID (e.g., AC-1, AU-2)
   - Add requirement description
   - Click "Next"

5. **Step 4: Review & Save**
   - Review summary statistics
   - Preview generated OSCAL JSON
   - Click "Save Component Definition"
   - Wait for success confirmation

### Managing Reusable Elements

1. **Navigate to Element Library**
   - Go to http://localhost:3000/build
   - Click "Element Library" tab

2. **Create Element**
   - Click "Create Element"
   - Select element type
   - Enter name and description
   - Paste valid OSCAL JSON for element
   - Optionally check "Share with other users"
   - Click "Create Element"

3. **Search and Filter**
   - Use search box for name/description
   - Select type filter to narrow results
   - Click "Clear Filters" to reset

4. **Edit or Delete**
   - Click "Edit" on element card
   - Make changes and save
   - Click trash icon to delete (with confirmation)

## Validation

### Backend Validation

- Component title required
- Version format validation
- OSCAL version must be valid
- Username must exist
- Filename sanitization

### Frontend Validation

**Wizard Validation:**
- Step 1: Title and version required
- Step 2: At least one component required, all must have titles
- Step 3: No validation (optional)
- Step 4: No validation

**Element Validation:**
- Element name required
- JSON content must be valid JSON
- Real-time JSON syntax validation
- Error messages displayed inline

## Storage Details

### Azure Blob Storage Structure

```
Container: oscal-build-storage
├── build/
│   ├── user1/
│   │   ├── django-security-controls.json
│   │   └── kubernetes-components.json
│   └── user2/
│       └── nginx-controls.json
```

### Blob Metadata

Each blob includes metadata:
- `title` - Component definition title
- `oscalVersion` - OSCAL version used
- `uploadedBy` - Username of creator
- `uploadedAt` - Timestamp

### Database Schema

**component_definition table:**
- `id` (BIGINT, PK)
- `oscal_uuid` (VARCHAR, unique)
- `title` (VARCHAR)
- `description` (TEXT)
- `version` (VARCHAR)
- `oscal_version` (VARCHAR)
- `blob_path` (VARCHAR)
- `filename` (VARCHAR)
- `component_count` (INTEGER)
- `control_count` (INTEGER)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)
- `user_id` (BIGINT, FK)

**reusable_element table:**
- `id` (BIGINT, PK)
- `type` (VARCHAR) - ENUM
- `name` (VARCHAR)
- `description` (TEXT)
- `json_content` (TEXT)
- `use_count` (INTEGER)
- `shared` (BOOLEAN)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)
- `user_id` (BIGINT, FK)

## Testing

### Manual Testing Checklist

**Component Builder:**
- [ ] Can create component definition with all fields
- [ ] Validation prevents proceeding without required fields
- [ ] Can add multiple components
- [ ] Can remove components
- [ ] Can add control implementations
- [ ] Can add multiple implemented requirements
- [ ] JSON preview displays correctly
- [ ] Save succeeds and shows confirmation
- [ ] Saved component appears in database
- [ ] JSON file created in Azure Blob Storage

**Element Library:**
- [ ] Can create all element types
- [ ] JSON validation works
- [ ] Can search elements
- [ ] Can filter by type
- [ ] Can edit existing elements
- [ ] Can delete elements
- [ ] Delete confirmation appears
- [ ] Shared elements marked correctly
- [ ] Usage count increments

### API Testing with curl

**Create Component Definition:**
```bash
curl -X POST http://localhost:8080/api/build/components \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Component",
    "description": "Test description",
    "version": "1.0.0",
    "oscalVersion": "1.1.3",
    "filename": "test-component.json",
    "jsonContent": "{\"component-definition\":{...}}",
    "componentCount": 1,
    "controlCount": 0
  }'
```

**Create Reusable Element:**
```bash
curl -X POST http://localhost:8080/api/build/elements \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "ROLE",
    "name": "System Administrator",
    "description": "Administrator role",
    "jsonContent": "{\"id\":\"admin\",\"title\":\"System Administrator\"}",
    "isShared": false
  }'
```

## Troubleshooting

### Common Issues

**Issue: Save fails with 403 Forbidden**
- **Cause:** JWT token expired or invalid
- **Solution:** Log out and log back in to get new token

**Issue: JSON validation errors in Element Modal**
- **Cause:** Invalid JSON syntax
- **Solution:** Use online JSON validator to check syntax

**Issue: Component not appearing in list**
- **Cause:** Database/blob storage out of sync
- **Solution:** Check backend logs, verify Azure connection

**Issue: Cannot delete element**
- **Cause:** Element in use by component definition
- **Solution:** Warning shown in delete dialog - proceed with caution

### Backend Logs

Check Spring Boot logs for errors:
```bash
tail -f back-end/logs/spring.log | grep -i "component\|element"
```

Look for:
- Azure Blob Storage connection issues
- Database constraint violations
- JSON parsing errors
- Authentication failures

## Future Enhancements (Phase 4 & 5)

**Phase 4: Import & Edit**
- Import existing OSCAL JSON files
- Edit existing component definitions
- Version history
- Clone/duplicate components

**Phase 5: Advanced Features**
- Component templates
- Bulk import/export
- Validation against OSCAL schemas
- Integration with control catalogs
- Collaborative editing
- Component sharing between users

## API Integration Examples

### JavaScript/TypeScript

```typescript
import { apiClient } from '@/lib/api-client';

// Create component definition
const component = await apiClient.createComponentDefinition({
  title: "My Component",
  description: "Component description",
  version: "1.0.0",
  oscalVersion: "1.1.3",
  filename: "my-component.json",
  jsonContent: JSON.stringify(oscalJson),
  componentCount: 2,
  controlCount: 5
});

// Get user's components
const components = await apiClient.getUserComponentDefinitions();

// Search components
const results = await apiClient.searchComponentDefinitions("django");
```

### Python

```python
import requests

token = "YOUR_JWT_TOKEN"
headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json"
}

# Create element
response = requests.post(
    "http://localhost:8080/api/build/elements",
    headers=headers,
    json={
        "type": "PARTY",
        "name": "ACME Corporation",
        "description": "Technology vendor",
        "jsonContent": '{"name": "ACME Corporation", "type": "organization"}',
        "isShared": True
    }
)

element = response.json()
print(f"Created element with ID: {element['id']}")
```

## Security Considerations

1. **Authentication Required:** All endpoints require valid JWT token
2. **User Isolation:** Users can only access their own components
3. **Shared Elements:** Shared elements visible to all authenticated users
4. **Input Validation:** All inputs validated on backend
5. **JSON Sanitization:** JSON content validated before storage
6. **Blob Storage Security:** Azure connection string secured in environment variables

## Performance

- **Component List:** Paginated results (default 20 per page)
- **Search:** Indexed database queries for fast search
- **Blob Storage:** Lazy loading of full JSON content
- **Element Library:** Filters applied client-side for instant response
- **Caching:** Browser caching of static resources

## References

- [OSCAL Component Definition Model](https://pages.nist.gov/OSCAL/concepts/layer/implementation/component-definition/)
- [OSCAL 1.1.3 Specification](https://pages.nist.gov/OSCAL/reference/latest/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Next.js Documentation](https://nextjs.org/docs)
- [Azure Blob Storage SDK](https://learn.microsoft.com/en-us/azure/storage/blobs/)

## Contact & Support

For issues or questions about the Component Builder feature, see the main project README or create an issue on GitHub.
