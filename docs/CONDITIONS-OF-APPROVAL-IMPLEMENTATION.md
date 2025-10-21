# Conditions of Approval - Implementation Summary

**Date**: October 21, 2025
**Status**: Backend Complete ✅ | Frontend Integration Complete ✅ | Condition Saving Pending ⏳

## Overview

This document describes the implementation of the "Conditions of Approval" feature for system authorizations. This feature allows users to establish mandatory and recommended conditions that must be met before or during the authorization period.

## What's Been Completed

### ✅ Backend Implementation (100% Complete)

#### 1. Database Entity
**File**: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/ConditionOfApproval.java`

- Created `ConditionOfApproval` entity with fields:
  - `id` (Long) - Primary key
  - `authorization` (Authorization) - Many-to-one relationship
  - `condition` (String) - The condition text
  - `conditionType` (Enum) - MANDATORY or RECOMMENDED
  - `dueDate` (LocalDate) - Optional for RECOMMENDED, required for MANDATORY
  - `createdAt` / `updatedAt` timestamps

#### 2. Updated Authorization Entity
**File**: `back-end/src/main/java/gov/nist/oscal/tools/api/entity/Authorization.java`

- Added one-to-many relationship: `List<ConditionOfApproval> conditions`
- Configured with `CASCADE.ALL` and `orphanRemoval = true`
- Added helper methods: `addCondition()`, `removeCondition()`

#### 3. Repository Layer
**File**: `back-end/src/main/java/gov/nist/oscal/tools/api/repository/ConditionOfApprovalRepository.java`

Query methods:
- `findByAuthorization(Authorization authorization)`
- `findByAuthorizationId(Long authorizationId)`
- `findByConditionType(ConditionType type)`
- `findByAuthorizationIdAndConditionType(Long authorizationId, ConditionType type)`

#### 4. Service Layer
**File**: `back-end/src/main/java/gov/nist/oscal/tools/api/service/ConditionOfApprovalService.java`

Business logic methods:
- `createCondition()` - Create a new condition
- `updateCondition()` - Update existing condition
- `getCondition()` - Get condition by ID
- `getConditionsByAuthorization()` - Get all conditions for an authorization
- `getConditionsByAuthorizationAndType()` - Get conditions by type
- `deleteCondition()` - Delete a condition
- `deleteConditionsByAuthorization()` - Delete all conditions for an authorization

#### 5. REST API Controller
**File**: `back-end/src/main/java/gov/nist/oscal/tools/api/controller/ConditionOfApprovalController.java`

API Endpoints:
- `POST /api/conditions` - Create condition
- `PUT /api/conditions/{id}` - Update condition
- `GET /api/conditions/{id}` - Get condition by ID
- `GET /api/conditions/authorization/{authorizationId}` - Get all conditions for authorization
- `GET /api/conditions/authorization/{authorizationId}/type/{type}` - Get conditions by type
- `DELETE /api/conditions/{id}` - Delete condition
- `DELETE /api/conditions/authorization/{authorizationId}` - Delete all conditions

#### 6. DTOs (Data Transfer Objects)
**Files**:
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/ConditionOfApprovalRequest.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/model/ConditionOfApprovalResponse.java`

Includes validation annotations for required fields.

#### 7. Updated AuthorizationResponse
**File**: `back-end/src/main/java/gov/nist/oscal/tools/api/model/AuthorizationResponse.java`

- Added `conditions` field: `List<ConditionOfApprovalResponse>`
- Updated constructor to populate conditions from entity

### ✅ Frontend Implementation (Component & Integration Complete)

#### 1. TypeScript Type Definitions
**File**: `front-end/src/types/oscal.ts`

Added:
```typescript
export type ConditionType = 'MANDATORY' | 'RECOMMENDED';

export interface ConditionOfApprovalRequest {
  authorizationId: number;
  condition: string;
  conditionType: ConditionType;
  dueDate?: string;
}

export interface ConditionOfApprovalResponse {
  id: number;
  authorizationId: number;
  condition: string;
  conditionType: ConditionType;
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
}
```

Updated `AuthorizationResponse` to include:
```typescript
conditions?: ConditionOfApprovalResponse[];
```

#### 2. ConditionsManager Component
**File**: `front-end/src/components/conditions-manager.tsx`

Features:
- ✅ Add new conditions (MANDATORY or RECOMMENDED)
- ✅ Edit existing conditions
- ✅ Delete conditions
- ✅ Summary cards showing total, mandatory, and recommended counts
- ✅ Due date validation for mandatory conditions
- ✅ Visual distinction between mandatory (red) and recommended (yellow) conditions
- ✅ Informational help text explaining condition types

#### 3. Authorization Wizard Integration
**File**: `front-end/src/components/authorization-wizard.tsx`

**Completed Changes**:
- ✅ Added 'conditions' to the Step type
- ✅ Added conditions state management
- ✅ Imported ConditionsManager component
- ✅ Updated step progression (handleNext/handleBack)
- ✅ Updated progress indicator with 'Conditions' label
- ✅ Added conditions step UI with ConditionsManager
- ✅ Added conditions summary display in review step

**Result**: Users can now add, edit, and delete conditions in the authorization wizard. The conditions step appears between "fill-variables" and "review" steps.

## What Needs To Be Done

### ⏳ Implement Condition Saving Logic

The authorization wizard currently collects conditions in state, but they are **NOT automatically saved** when the authorization is created. You need to update the parent component to save conditions after creating the authorization.

**File to modify**: `front-end/src/app/authorizations/page.tsx`

**Current flow** (lines 108-134):
```typescript
const handleCreateAuthorization = async (data: {
  name: string;
  sspItemId: string;
  sarItemId?: string;
  templateId: number;
  variableValues: Record<string, string>;
  dateAuthorized: string;
  dateExpired: string;
  systemOwner: string;
  securityManager: string;
  authorizingOfficial: string;
  editedContent: string;
}) => {
  try {
    setSavingAuthorization(true);
    await apiClient.createAuthorization(data);  // Creates authorization
    toast.success('Authorization created successfully');
    await loadAuthorizations();
    setView('list-authorizations');
    setActiveTab('authorizations');
  } catch (err) {
    console.error('Failed to create authorization:', err);
    toast.error('Failed to create authorization');
  } finally {
    setSavingAuthorization(false);
  }
};
```

**Required changes**:

1. **Update AuthorizationWizard props** to accept a conditions callback:
```typescript
// In authorization-wizard.tsx, update the props interface:
interface AuthorizationWizardProps {
  templates: AuthorizationTemplateResponse[];
  sspItems: LibraryItem[];
  sarItems: LibraryItem[];
  onSave: (data: AuthorizationData, conditions: Condition[]) => Promise<void>;  // Add conditions parameter
  onCancel: () => void;
  isSaving: boolean;
}

// Update the handleSave function to pass conditions:
const handleSave = async () => {
  // ... existing validation ...
  onSave({
    // ... existing data ...
  }, conditions);  // Pass conditions array
};
```

2. **Update handleCreateAuthorization** in `authorizations/page.tsx`:
```typescript
const handleCreateAuthorization = async (
  data: {
    name: string;
    sspItemId: string;
    sarItemId?: string;
    templateId: number;
    variableValues: Record<string, string>;
    dateAuthorized: string;
    dateExpired: string;
    systemOwner: string;
    securityManager: string;
    authorizingOfficial: string;
    editedContent: string;
  },
  conditions: Condition[]  // Add conditions parameter
) => {
  try {
    setSavingAuthorization(true);

    // Step 1: Create the authorization
    const createdAuth = await apiClient.createAuthorization(data);
    const authorizationId = createdAuth.id;  // Get the ID from response

    // Step 2: Save each condition
    if (conditions && conditions.length > 0) {
      for (const condition of conditions) {
        await apiClient.createCondition({
          authorizationId: authorizationId,
          condition: condition.condition,
          conditionType: condition.conditionType,
          dueDate: condition.dueDate || undefined,
        });
      }
    }

    toast.success('Authorization created successfully with ' + conditions.length + ' condition(s)');
    await loadAuthorizations();
    setView('list-authorizations');
    setActiveTab('authorizations');
  } catch (err) {
    console.error('Failed to create authorization:', err);
    toast.error('Failed to create authorization');
  } finally {
    setSavingAuthorization(false);
  }
};
```

3. **Add API client methods** for condition operations:

**File**: `front-end/src/lib/api-client.ts`

Add these methods to the `apiClient` class:
```typescript
async createCondition(condition: ConditionOfApprovalRequest): Promise<ConditionOfApprovalResponse> {
  const response = await fetch(`${this.baseURL}/conditions`, {
    method: 'POST',
    headers: this.getHeaders(),
    body: JSON.stringify(condition),
  });

  if (!response.ok) {
    throw new Error('Failed to create condition');
  }

  return response.json();
}

async updateCondition(id: number, condition: ConditionOfApprovalRequest): Promise<ConditionOfApprovalResponse> {
  const response = await fetch(`${this.baseURL}/conditions/${id}`, {
    method: 'PUT',
    headers: this.getHeaders(),
    body: JSON.stringify(condition),
  });

  if (!response.ok) {
    throw new Error('Failed to update condition');
  }

  return response.json();
}

async deleteCondition(id: number): Promise<void> {
  const response = await fetch(`${this.baseURL}/conditions/${id}`, {
    method: 'DELETE',
    headers: this.getHeaders(),
  });

  if (!response.ok) {
    throw new Error('Failed to delete condition');
  }
}

async getConditionsByAuthorization(authorizationId: number): Promise<ConditionOfApprovalResponse[]> {
  const response = await fetch(`${this.baseURL}/conditions/authorization/${authorizationId}`, {
    method: 'GET',
    headers: this.getHeaders(),
  });

  if (!response.ok) {
    throw new Error('Failed to get conditions');
  }

  return response.json();
}
```

**Note**: The `createAuthorization` method needs to return the full `AuthorizationResponse` object (not just `void`) so we can access the `id` field.

#### 2. Display Conditions in Authorization Detail View

**File**: `front-end/src/app/authorizations/page.tsx`

**Status**: Partially implemented - backend already returns conditions in `AuthorizationResponse`, but frontend detail view needs to display them.

**Current state**: When viewing an authorization (`view-authorization` state), the authorization object already includes a `conditions` array from the backend (lines 253-475).

**Required changes**: Add a conditions section to display the conditions in the authorization detail view:

```typescript
{/* Add this section after the Authorization Document section, around line 473 */}

{/* Conditions of Approval */}
{selectedAuthorization.conditions && selectedAuthorization.conditions.length > 0 && (
  <div className="space-y-4">
    <h3 className="text-lg font-semibold border-b pb-2">Conditions of Approval</h3>

    {/* Summary Cards */}
    <div className="grid grid-cols-3 gap-4">
      <Card className="p-4 bg-slate-800 border-slate-700">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm text-slate-400">Total Conditions</p>
            <p className="text-2xl font-bold">{selectedAuthorization.conditions.length}</p>
          </div>
          <AlertCircle className="h-8 w-8 text-blue-500" />
        </div>
      </Card>

      <Card className="p-4 bg-red-900/20 border-red-800">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm text-red-400">Mandatory</p>
            <p className="text-2xl font-bold text-red-400">
              {selectedAuthorization.conditions.filter(c => c.conditionType === 'MANDATORY').length}
            </p>
          </div>
          <AlertCircle className="h-8 w-8 text-red-500" />
        </div>
      </Card>

      <Card className="p-4 bg-yellow-900/20 border-yellow-800">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm text-yellow-400">Recommended</p>
            <p className="text-2xl font-bold text-yellow-400">
              {selectedAuthorization.conditions.filter(c => c.conditionType === 'RECOMMENDED').length}
            </p>
          </div>
          <AlertCircle className="h-8 w-8 text-yellow-500" />
        </div>
      </Card>
    </div>

    {/* Conditions List */}
    <div className="space-y-2">
      {selectedAuthorization.conditions.map((condition) => (
        <Card
          key={condition.id}
          className={`p-4 ${
            condition.conditionType === 'MANDATORY'
              ? 'bg-red-900/10 border-red-800'
              : 'bg-yellow-900/10 border-yellow-800'
          }`}
        >
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 space-y-2">
              <div className="flex items-center gap-2">
                <Badge
                  variant={condition.conditionType === 'MANDATORY' ? 'destructive' : 'default'}
                  className={
                    condition.conditionType === 'MANDATORY'
                      ? 'bg-red-600 text-white'
                      : 'bg-yellow-600 text-white'
                  }
                >
                  {condition.conditionType}
                </Badge>
                {condition.dueDate && (
                  <Badge variant="outline" className="text-xs">
                    Due: {new Date(condition.dueDate).toLocaleDateString()}
                  </Badge>
                )}
              </div>
              <p className="text-sm">{condition.condition}</p>
            </div>
          </div>
        </Card>
      ))}
    </div>
  </div>
)}
```

**Note**: Don't forget to import the `AlertCircle` icon from `lucide-react` at the top of the file if not already imported.

## Database Migration

When the application starts, the H2 database will automatically create the new `conditions_of_approval` table with the following structure:

```sql
CREATE TABLE conditions_of_approval (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  authorization_id BIGINT NOT NULL,
  condition TEXT NOT NULL,
  condition_type VARCHAR(20) NOT NULL,
  due_date DATE,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  FOREIGN KEY (authorization_id) REFERENCES authorizations(id) ON DELETE CASCADE
);
```

## Testing Checklist

Once integration is complete, test the following scenarios:

### Backend Testing
- [ ] Create an authorization with conditions via API
- [ ] Retrieve authorization and verify conditions are included
- [ ] Update a condition
- [ ] Delete a condition
- [ ] Delete an authorization and verify conditions are cascade-deleted
- [ ] Verify mandatory conditions require a due date
- [ ] Verify recommended conditions work without a due date

### Frontend Testing
- [ ] Navigate through authorization wizard to conditions step
- [ ] Add a mandatory condition with due date
- [ ] Add a recommended condition without due date
- [ ] Try to add mandatory condition without due date (should show validation error)
- [ ] Edit an existing condition
- [ ] Delete a condition
- [ ] Complete authorization creation with conditions
- [ ] View authorization detail and verify conditions are displayed
- [ ] Verify condition counts in summary cards are correct

## API Examples

### Create a Condition

```bash
POST /api/conditions
Content-Type: application/json
Authorization: Bearer <token>

{
  "authorizationId": 1,
  "condition": "Complete POAM for finding F-001",
  "conditionType": "MANDATORY",
  "dueDate": "2025-12-31"
}
```

### Get Conditions for Authorization

```bash
GET /api/conditions/authorization/1
Authorization: Bearer <token>
```

Response:
```json
[
  {
    "id": 1,
    "authorizationId": 1,
    "condition": "Complete POAM for finding F-001",
    "conditionType": "MANDATORY",
    "dueDate": "2025-12-31",
    "createdAt": "2025-10-21T09:00:00Z",
    "updatedAt": "2025-10-21T09:00:00Z"
  }
]
```

## Files Created/Modified

### Backend
- ✅ Created: `ConditionOfApproval.java` (entity)
- ✅ Modified: `Authorization.java` (added conditions relationship)
- ✅ Created: `ConditionOfApprovalRepository.java`
- ✅ Created: `ConditionOfApprovalService.java`
- ✅ Created: `ConditionOfApprovalController.java`
- ✅ Created: `ConditionOfApprovalRequest.java` (DTO)
- ✅ Created: `ConditionOfApprovalResponse.java` (DTO)
- ✅ Modified: `AuthorizationResponse.java` (added conditions field)

### Frontend
- ✅ Modified: `oscal.ts` (added type definitions)
- ✅ Created: `conditions-manager.tsx` (component)
- ⏳ To modify: `authorization-wizard.tsx` (add conditions step)
- ⏳ To create/modify: Authorization detail view component
- ⏳ To modify: `api-client.ts` (add condition API methods)

## Notes

- The backend server compiled successfully and is running on port 8080
- The frontend is running on port 3000
- All backend API endpoints are secured and require JWT authentication
- Conditions are automatically deleted when their parent authorization is deleted (cascade)
- The ConditionsManager component is reusable and can be embedded in other parts of the application

## Next Steps

1. Integrate ConditionsManager into the authorization wizard as a new step
2. Update the authorization creation flow to save conditions after creating the authorization
3. Create or update the authorization detail view to display conditions
4. Add API client methods for condition operations
5. Test the complete workflow end-to-end
6. Consider adding notifications for approaching condition due dates (future enhancement)

---

For questions or issues, refer to the backend logs at `back-end/logs/spring.log` and frontend console output.
