# User Logo Feature

**Date:** 2025-10-20
**Status:** ✅ Complete

## Overview

The User Logo feature allows users to upload a logo image to their profile and automatically inject it into authorization templates using the special `{{ logo }}` variable tag.

## Features

### 1. Logo Upload
- Users can upload logo images (PNG, JPG, GIF) up to 2MB
- Logo is stored as a base64 data URL in the database
- Preview shown immediately upon selection
- Logo save is deferred until user clicks "Save Profile" or "Save All Changes"

### 2. Template Variable Integration
- Templates can use `{{ logo }}` tag anywhere in the content
- When creating an authorization, the `{{ logo }}` tag is automatically replaced with the user's uploaded logo
- Logo is rendered as markdown image syntax: `![Logo](data:image/...)`

## User Guide

### Uploading a Logo

1. Navigate to your **Profile** page (click your username in the header)
2. Find the **User Logo** card
3. Click **Select Logo** button
4. Choose an image file from your computer (max 2MB)
5. Preview will appear immediately
6. Click **Save Profile** or **Save All Changes** to save the logo

### Using Logo in Authorization Templates

When creating or editing an authorization template, simply include the `{{ logo }}` tag where you want the logo to appear:

```markdown
# System Authorization

{{ logo }}

This system is authorized for operation by [Authorizing Official Name].

**Date Authorized:** {{ dateAuthorized }}
**Date Expires:** {{ dateExpired }}
```

When an authorization is created from this template, the `{{ logo }}` tag will be automatically replaced with the user's logo image.

## Technical Implementation

### Backend Changes

#### 1. User Entity (`User.java`)
Added logo field to store base64 data URL:

```java
@Column(columnDefinition = "TEXT")
private String logo; // Base64-encoded logo image (data URL format)

public String getLogo() { return logo; }
public void setLogo(String logo) { this.logo = logo; }
```

#### 2. AuthController (`AuthController.java`)
Added logo upload endpoint:

```java
@PostMapping("/logo")
public ResponseEntity<?> uploadLogo(@RequestBody Map<String, String> logoData) {
    // Validates data:image/ format
    // Updates user logo
    // Returns updated user data with logo
}
```

All authentication responses now include the `logo` field.

#### 3. AuthService (`AuthService.java`)
Added logo update method:

```java
@Transactional
public User updateLogo(String username, String logo) {
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    user.setLogo(logo);
    return userRepository.save(user);
}
```

**Location:** `AuthService.java:160-167`

#### 4. AuthorizationService (`AuthorizationService.java`)
Enhanced template rendering with user-specific variables:

```java
public String renderTemplate(String template, Map<String, String> variableValues, User user) {
    if (template == null) return template;
    String result = template;

    // Handle user-specific variables first ({{ logo }})
    if (user != null && user.getLogo() != null) {
        String logoPattern = "\\{\\{\\s*logo\\s*\\}\\}";
        String logoReplacement = "![Logo](" + user.getLogo() + ")";
        result = result.replaceAll(logoPattern, Matcher.quoteReplacement(logoReplacement));
    }

    // Handle regular variables
    if (variableValues != null) {
        for (Map.Entry<String, String> entry : variableValues.entrySet()) {
            String variableName = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";
            String pattern = "\\{\\{\\s*" + Pattern.quote(variableName) + "\\s*\\}\\}";
            result = result.replaceAll(pattern, Matcher.quoteReplacement(value));
        }
    }

    return result;
}
```

**Location:** `AuthorizationService.java:215-243`

### Frontend Changes

#### 1. Type Definitions (`auth.ts`)
Added logo field to User and AuthResponse interfaces:

```typescript
export interface User {
    userId: number;
    username: string;
    email: string;
    // ... other fields ...
    logo?: string;
}

export interface AuthResponse {
    // ... other fields ...
    logo?: string;
}
```

**Location:** `front-end/src/types/auth.ts:12, 27`

#### 2. API Client (`api-client.ts`)
Added logo upload method with robust error handling:

```typescript
async uploadLogo(logo: string): Promise<void> {
    try {
        const response = await this.fetchWithTimeout(
            `${API_BASE_URL}/auth/logo`,
            {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ logo }),
            },
            10000
        );

        if (!response.ok) {
            let errorMessage = 'Failed to upload logo';
            try {
                const contentType = response.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    const error = await response.json();
                    errorMessage = error.error || error.message || errorMessage;
                } else {
                    const text = await response.text();
                    if (text) errorMessage = text;
                }
            } catch {
                errorMessage = response.statusText || errorMessage;
            }

            if (response.status === 403) {
                throw new Error('Access denied. Please make sure you are logged in.');
            }
            throw new Error(errorMessage);
        }
    } catch (error) {
        console.error('Logo upload error:', error);
        throw error;
    }
}
```

Updated login, register, and refreshToken methods to store logo in localStorage.

#### 3. Profile Page (`profile/page.tsx`)
Implemented logo upload UI with deferred save approach:

**State Management:**
```typescript
const [logoPreview, setLogoPreview] = useState<string | null>(user?.logo || null);
const [pendingLogo, setPendingLogo] = useState<string | null>(null);
```

**Logo Selection Handler:**
```typescript
const handleLogoSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
        setErrorMessage('Please select an image file');
        toast.error('Please select an image file');
        return;
    }

    // Validate file size (max 2MB)
    if (file.size > 2 * 1024 * 1024) {
        setErrorMessage('Logo file size must be less than 2MB');
        toast.error('Logo file size must be less than 2MB');
        return;
    }

    // Convert to base64 for preview
    const reader = new FileReader();
    reader.onloadend = () => {
        const base64Logo = reader.result as string;
        setLogoPreview(base64Logo);
        setPendingLogo(base64Logo);
        toast.success('Logo selected. Click "Save Profile" or "Save All Changes" to upload.');
    };
    reader.readAsDataURL(file);
};
```

**Location:** `profile/page.tsx:131-165`

**Integration with Profile Save:**
```typescript
const handleUpdateProfile = async (e: React.FormEvent) => {
    // ... validate and prepare other profile updates ...

    // Upload logo if there's a pending logo change
    if (pendingLogo) {
        await apiClient.uploadLogo(pendingLogo);
        setPendingLogo(null);
    }

    // ... success handling ...
};
```

**Location:** `profile/page.tsx:109-112`

## Database Schema

The `logo` field is added to the `users` table as a `TEXT` column to accommodate base64-encoded images:

```sql
ALTER TABLE users ADD COLUMN logo TEXT;
```

This is automatically applied by Hibernate when the application starts (using `spring.jpa.hibernate.ddl-auto=update`).

## Validation Rules

### File Upload Validation
- **File Type:** Must be an image (image/*)
- **File Size:** Maximum 2MB
- **Format:** Automatically converted to base64 data URL

### Template Variable
- **Pattern:** `{{ logo }}` (with optional whitespace)
- **Replacement:** Markdown image syntax `![Logo](data:image/...)`
- **Behavior:** Only replaced if user has uploaded a logo

## Error Handling

### Common Errors

1. **403 Forbidden**
   - **Cause:** User not authenticated or token expired
   - **Solution:** Log in again

2. **File Too Large**
   - **Cause:** Selected file exceeds 2MB limit
   - **Message:** "Logo file size must be less than 2MB"
   - **Solution:** Resize or compress the image

3. **Invalid File Type**
   - **Cause:** Selected file is not an image
   - **Message:** "Please select an image file"
   - **Solution:** Select a PNG, JPG, or GIF file

4. **Failed to Read File**
   - **Cause:** Browser unable to read the selected file
   - **Message:** "Failed to read file. Please try again."
   - **Solution:** Try selecting the file again or use a different file

## Security Considerations

1. **File Size Limit:** Enforced to prevent database bloat (2MB max)
2. **File Type Validation:** Client-side validation for image types
3. **Authentication Required:** Logo upload requires valid JWT token
4. **Base64 Encoding:** Images stored as data URLs, eliminating file system access concerns

## Performance Considerations

1. **Database Storage:** Logos stored as TEXT in database (suitable for small logos)
2. **Transfer Size:** Base64 encoding increases size by ~33%, but logos are small
3. **Caching:** Logos cached in localStorage with user session
4. **Deferred Save:** Logo only saved when user explicitly saves profile

## Testing

### Manual Testing Steps

1. **Upload New Logo**
   - Navigate to Profile page
   - Click "Select Logo"
   - Choose an image file
   - Verify preview appears
   - Verify pending indicator shows
   - Click "Save Profile"
   - Verify success message
   - Refresh page and verify logo persists

2. **Update Existing Logo**
   - Follow steps above with a different image
   - Verify new logo replaces old one

3. **Logo in Authorization Template**
   - Create/edit an authorization template
   - Add `{{ logo }}` tag in the content
   - Create an authorization from the template
   - Verify logo appears in the completed authorization content

4. **Error Cases**
   - Try uploading a non-image file
   - Try uploading a file > 2MB
   - Verify error messages appear

## Known Limitations

1. **File Size:** 2MB limit may be restrictive for high-resolution logos
2. **Format Support:** Only supports web-compatible image formats (PNG, JPG, GIF)
3. **Database Storage:** Large logos increase database size; consider external storage for production

## Future Enhancements

Potential improvements for future releases:

1. **Image Optimization:** Automatic resizing/compression on upload
2. **External Storage:** Store logos in cloud storage (S3, Azure Blob) instead of database
3. **Multiple Logos:** Allow users to have multiple logos for different contexts
4. **Logo Library:** Provide default/stock logos for users without custom logos
5. **Preview in Template Editor:** Show real-time preview of logo in template editor

## Related Files

### Backend
- `back-end/src/main/java/gov/nist/oscal/tools/api/entity/User.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/controller/AuthController.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthService.java`
- `back-end/src/main/java/gov/nist/oscal/tools/api/service/AuthorizationService.java`

### Frontend
- `front-end/src/types/auth.ts`
- `front-end/src/lib/api-client.ts`
- `front-end/src/app/profile/page.tsx`

## Support

For issues or questions:
1. Check error messages in browser console
2. Verify authentication token is valid
3. Ensure file meets validation requirements
4. Restart development servers if schema changes don't apply
