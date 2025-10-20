# Template Editor Variable Detection Fix

**Date**: October 19, 2025
**Issue**: Template editor was not correctly detecting all parameters
**Status**: ✅ Fixed

## Problem Identified

The template editor's frontend regex pattern did not match the backend's pattern exactly, causing discrepancies in variable detection.

### Frontend Pattern (Original - INCORRECT)
```javascript
/\{\{\s*([^}]+)\s*\}\}/g
```
This pattern matched ANY characters between `{{ }}`, including:
- Spaces: `{{ with spaces }}`
- Special characters: `{{ special@char }}`
- Dots: `{{ dot.separated }}`

These would be shown as valid in the frontend but **rejected by the backend**.

### Backend Pattern (Correct)
```java
Pattern.compile("\\{\\{\\s*([\\w\\-]+)\\s*\\}\\}")
```
JavaScript equivalent:
```javascript
/\{\{\s*([\w\-]+)\s*\}\}/g
```

This pattern only allows:
- Letters (a-z, A-Z)
- Digits (0-9)
- Underscore (_)
- Hyphen (-)
- **NO spaces or special characters**

## Solution Implemented

### 1. Fixed Pattern Matching

Updated the frontend to use the exact same pattern as the backend:
```javascript
const validPattern = /\{\{\s*([\w\-]+)\s*\}\}/g;
```

### 2. Added Dual Detection

The editor now detects **both** valid and invalid variables:

```javascript
const validPattern = /\{\{\s*([\w\-]+)\s*\}\}/g;   // Backend-compatible
const allPattern = /\{\{\s*([^}]+)\s*\}\}/g;       // All variable-like patterns
```

- **Valid variables**: Match the backend pattern
- **Invalid variables**: Look like variables but contain disallowed characters

### 3. Added Refresh Button

New "Refresh" button with icon to manually re-parse variables:
```jsx
<Button onClick={handleRefreshVariables}>
  <RefreshCw className="h-4 w-4 mr-2" />
  Refresh
</Button>
```

### 4. Enhanced UI Feedback

Added a prominent status card showing:

**Valid Variables (Blue Section)**:
- Count badge: "X valid"
- List of all valid variables as badges
- Green checkmark indication

**Invalid Variables (Red Warning Section)**:
- Count badge: "X invalid" (only shown if any exist)
- List of invalid variables as badges
- Warning message explaining the rules
- Red color scheme to indicate errors

**Example UI**:
```
┌─────────────────────────────────────────────────────────────┐
│ Detected Variables    [3 valid]  [2 invalid]    [Refresh]   │
│                                                               │
│ Valid Variables:                                              │
│ [system_name] [system-id] [owner_email]                      │
│                                                               │
│ ⚠ Invalid Variables (will be rejected by backend):           │
│ [system name] [owner.email]                                  │
│                                                               │
│ Variables must only contain letters, digits, underscores,    │
│ and hyphens (no spaces or special characters)                │
└─────────────────────────────────────────────────────────────┘
```

## Valid Variable Examples

✅ **ALLOWED**:
- `{{ variable }}`
- `{{ variable_name }}`
- `{{ variable-name }}`
- `{{ Variable123 }}`
- `{{ _underscore_start }}`
- `{{ CamelCaseVar }}`
- `{{ system_name_123 }}`

❌ **NOT ALLOWED** (will be flagged as invalid):
- `{{ with spaces }}` - contains spaces
- `{{ special@char }}` - contains @
- `{{ dot.separated }}` - contains .
- `{{ plus+sign }}` - contains +
- `{{ dollar$sign }}` - contains $
- `{{ colon:name }}` - contains :

## Testing

### Pattern Validation Test
Created comprehensive test file: `test-variable-patterns.js`

Results: **14/14 tests passed** ✅

```
Valid Variables (will be accepted):
  ✓ system_name
  ✓ system-id
  ✓ system_owner
  ✓ SystemName_123

Invalid Variables (will be rejected):
  ✗ system name
  ✗ system.id
  ✗ date@time
```

## User Benefits

1. **Accurate Detection**: Frontend now matches backend exactly
2. **Visual Feedback**: Clear indication of valid vs invalid variables
3. **Manual Control**: Refresh button for manual re-parsing
4. **Error Prevention**: Invalid variables are flagged before saving
5. **Better UX**: Users see exactly which variables will work

## Migration Guide

### For Existing Templates

If you have existing templates with invalid variables:

1. Open the template in the editor
2. Look for the red "Invalid Variables" section
3. Invalid variables will be listed with red badges
4. Replace them with valid versions:
   - `{{ system name }}` → `{{ system_name }}`
   - `{{ owner.email }}` → `{{ owner_email }}`
   - `{{ date@time }}` → `{{ date_time }}`

### Variable Naming Best Practices

**Recommended Conventions**:
- Use snake_case: `{{ system_name }}`, `{{ authorization_date }}`
- Use kebab-case: `{{ system-name }}`, `{{ authorization-date }}`
- Use clear names: `{{ authorized_by }}` not `{{ ab }}`
- Group related: `{{ system_name }}`, `{{ system_owner }}`, `{{ system_id }}`

**Avoid**:
- Spaces: Use underscore or hyphen instead
- Special characters: Stick to letters, digits, _, -
- Very long names: Keep under 30 characters for readability
- Abbreviations: Be descriptive

## Technical Implementation

### Component Changes: `template-editor.tsx`

**Added**:
- `invalidVariables` state array
- `extractVariables()` function with dual pattern matching
- `handleRefreshVariables()` function
- Variables status card UI
- Invalid variables warning section
- Pattern guidance text

**Updated**:
- Variable extraction to use backend-compatible pattern
- Real-time validation on content change
- Badge components for visual feedback

### Files Modified
- `front-end/src/components/template-editor.tsx`

### Files Created
- `test-variable-patterns.js` - Pattern validation test

## Verification

To verify the fix is working:

1. Navigate to http://localhost:3000/authorizations
2. Go to Templates tab
3. Click "Create New Template"
4. Enter a template with mixed variables:
   ```markdown
   # Test Template
   Valid: {{ system_name }}
   Invalid: {{ system name }}
   ```
5. Observe:
   - Blue badge showing "1 valid"
   - Red badge showing "1 invalid"
   - Valid variable displayed in blue section
   - Invalid variable displayed in red warning section
6. Click "Refresh" button to re-parse
7. Variables should update immediately

## Summary

The template editor now:
- ✅ Uses backend-compatible regex pattern
- ✅ Detects valid and invalid variables separately
- ✅ Provides visual feedback for both types
- ✅ Includes manual refresh button
- ✅ Shows clear error messages
- ✅ Prevents confusion about which variables will work

This ensures users create templates that will work correctly when creating authorizations, preventing backend errors and improving the overall user experience.
