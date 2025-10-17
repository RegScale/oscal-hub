# OSCAL CLI Web Interface - Frontend Design

Complete UI/UX design specification using **Next.js 14+**, **ShadCN UI**, and **Dark Mode Only**.

## Table of Contents

- [Design System](#design-system)
- [Technology Stack](#technology-stack)
- [User Personas](#user-personas)
- [Application Structure](#application-structure)
- [Page Designs](#page-designs)
- [ShadCN Component Library](#shadcn-component-library)
- [User Flows](#user-flows)
- [Responsive Design](#responsive-design)
- [Accessibility](#accessibility)

## Design System

### Dark Mode Theme (Only)

We're using a **dark-first** design approach optimized for developers and technical users working with code and OSCAL documents.

#### Color Palette (Tailwind + ShadCN)

```css
/* Dark Theme Variables */
:root {
  --background: 222.2 84% 4.9%;        /* #09090b - Deep dark background */
  --foreground: 210 40% 98%;            /* #fafafa - Almost white text */

  --card: 222.2 84% 4.9%;               /* #09090b - Card background */
  --card-foreground: 210 40% 98%;       /* #fafafa - Card text */

  --popover: 222.2 84% 4.9%;            /* Popover background */
  --popover-foreground: 210 40% 98%;    /* Popover text */

  --primary: 217.2 91.2% 59.8%;         /* #3b82f6 - Blue for actions */
  --primary-foreground: 222.2 47.4% 11.2%; /* Dark text on primary */

  --secondary: 217.2 32.6% 17.5%;       /* #1e293b - Subtle secondary */
  --secondary-foreground: 210 40% 98%;  /* Light text on secondary */

  --muted: 217.2 32.6% 17.5%;          /* Muted background */
  --muted-foreground: 215 20.2% 65.1%; /* Muted text */

  --accent: 217.2 32.6% 17.5%;         /* Accent background */
  --accent-foreground: 210 40% 98%;    /* Accent text */

  --destructive: 0 62.8% 30.6%;        /* #991b1b - Error red */
  --destructive-foreground: 210 40% 98%;

  --border: 217.2 32.6% 17.5%;         /* Border color */
  --input: 217.2 32.6% 17.5%;          /* Input background */
  --ring: 224.3 76.3% 48%;             /* Focus ring */

  --radius: 0.5rem;                    /* Default border radius */
}
```

#### Status Colors

- **Success**: `text-green-500` / `bg-green-500/10` - Valid documents, completed operations
- **Error**: `text-destructive` / `bg-destructive/10` - Validation errors, failed operations
- **Warning**: `text-yellow-500` / `bg-yellow-500/10` - Warnings, deprecations
- **Info**: `text-blue-500` / `bg-blue-500/10` - Informational messages
- **Processing**: `text-purple-500` / `bg-purple-500/10` - In-progress operations

### Typography

Using system font stack with **Inter** as primary:

```css
font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif;
font-family-mono: 'JetBrains Mono', 'Fira Code', 'Monaco', 'Courier New', monospace;
```

**Tailwind Typography Classes:**
- `text-xs` (12px) - Labels, badges
- `text-sm` (14px) - Body text, descriptions
- `text-base` (16px) - Primary content
- `text-lg` (18px) - Section headings
- `text-xl` (20px) - Page titles
- `text-2xl` (24px) - Main headings

## Technology Stack

### Core Technologies

- **Framework**: Next.js 14+ (App Router)
- **UI Components**: ShadCN UI
- **Styling**: Tailwind CSS (Dark mode only)
- **Icons**: Lucide React
- **Code Editor**: Monaco Editor (with dark theme)
- **State Management**: Zustand
- **Forms**: React Hook Form + Zod
- **File Upload**: react-dropzone
- **TypeScript**: Strict mode enabled

### Key Dependencies

```json
{
  "dependencies": {
    "next": "^14.0.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "tailwindcss": "^3.4.0",
    "@radix-ui/react-*": "Latest",
    "lucide-react": "^0.292.0",
    "@monaco-editor/react": "^4.6.0",
    "zustand": "^4.4.0",
    "react-hook-form": "^7.48.0",
    "zod": "^3.22.0",
    "react-dropzone": "^14.2.0"
  }
}
```

## User Personas

### Persona 1: Security Analyst Sarah
- Validates SSPs and performs format conversions
- Needs clear visual feedback on validation errors
- Non-technical, prefers point-and-click interfaces

### Persona 2: Compliance Manager Mike
- Processes multiple documents for audits
- Needs batch operations and summary reports
- Values efficiency and progress tracking

### Persona 3: Security Engineer Alex
- Integrates OSCAL into CI/CD pipelines
- Uses both UI and API
- Wants keyboard shortcuts and fast operations

## Application Structure

### Next.js App Router Structure

```
app/
├── layout.tsx                 # Root layout with dark theme
├── page.tsx                   # Dashboard (home)
├── validate/
│   └── page.tsx              # Validation page
├── convert/
│   └── page.tsx              # Conversion page
├── resolve/
│   └── page.tsx              # Profile resolution page
├── batch/
│   ├── page.tsx              # Batch operations page
│   └── results/
│       └── [id]/page.tsx     # Batch results detail
├── history/
│   └── page.tsx              # Operation history
└── api/                       # API routes (if needed)
    └── webhook/route.ts

components/
├── ui/                        # ShadCN components
│   ├── button.tsx
│   ├── card.tsx
│   ├── badge.tsx
│   ├── alert.tsx
│   ├── progress.tsx
│   ├── select.tsx
│   └── ...
├── file-uploader.tsx
├── code-editor.tsx
├── validation-results.tsx
├── format-selector.tsx
├── model-type-selector.tsx
├── progress-indicator.tsx
└── layout/
    ├── header.tsx
    ├── sidebar.tsx
    └── footer.tsx

lib/
├── utils.ts                   # cn() utility, etc.
├── api-client.ts              # API wrapper
├── websocket.ts               # WebSocket client
└── stores/
    ├── file-store.ts
    ├── operation-store.ts
    └── websocket-store.ts
```

## Page Designs

### 1. Dashboard (`app/page.tsx`)

Modern card-based dashboard with quick actions.

```tsx
import { Card, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { FileCheck, ArrowRightLeft, GitMerge, Folders, Clock } from 'lucide-react'
import Link from 'next/link'

export default function Dashboard() {
  return (
    <div className="container mx-auto py-8 px-4">
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-2">OSCAL CLI</h1>
        <p className="text-muted-foreground">
          Work with OSCAL documents visually
        </p>
      </div>

      {/* Quick Actions Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-12">
        <Link href="/validate">
          <Card className="hover:bg-accent/50 transition-colors cursor-pointer">
            <CardHeader>
              <FileCheck className="h-12 w-12 mb-4 text-primary" />
              <CardTitle>Validate</CardTitle>
              <CardDescription>
                Check if your OSCAL document is valid
              </CardDescription>
            </CardHeader>
          </Card>
        </Link>

        <Link href="/convert">
          <Card className="hover:bg-accent/50 transition-colors cursor-pointer">
            <CardHeader>
              <ArrowRightLeft className="h-12 w-12 mb-4 text-primary" />
              <CardTitle>Convert</CardTitle>
              <CardDescription>
                Change format between XML, JSON, and YAML
              </CardDescription>
            </CardHeader>
          </Card>
        </Link>

        <Link href="/resolve">
          <Card className="hover:bg-accent/50 transition-colors cursor-pointer">
            <CardHeader>
              <GitMerge className="h-12 w-12 mb-4 text-primary" />
              <CardTitle>Resolve</CardTitle>
              <CardDescription>
                Resolve profiles into catalogs
              </CardDescription>
            </CardHeader>
          </Card>
        </Link>

        <Link href="/batch">
          <Card className="hover:bg-accent/50 transition-colors cursor-pointer">
            <CardHeader>
              <Folders className="h-12 w-12 mb-4 text-primary" />
              <CardTitle>Batch</CardTitle>
              <CardDescription>
                Process multiple files simultaneously
              </CardDescription>
            </CardHeader>
          </Card>
        </Link>

        <Link href="/history">
          <Card className="hover:bg-accent/50 transition-colors cursor-pointer">
            <CardHeader>
              <Clock className="h-12 w-12 mb-4 text-primary" />
              <CardTitle>History</CardTitle>
              <CardDescription>
                View past operations and results
              </CardDescription>
            </CardHeader>
          </Card>
        </Link>
      </div>

      {/* Recent Operations */}
      <RecentOperations />
    </div>
  )
}
```

### 2. Validate Page (`app/validate/page.tsx`)

Two-column layout with file upload and results.

```tsx
'use client'

import { useState } from 'react'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Button } from '@/components/ui/button'
import { FileUploader } from '@/components/file-uploader'
import { CodeEditor } from '@/components/code-editor'
import { ValidationResultsPanel } from '@/components/validation-results'
import { Loader2 } from 'lucide-react'

export default function ValidatePage() {
  const [file, setFile] = useState<File | null>(null)
  const [modelType, setModelType] = useState('catalog')
  const [isValidating, setIsValidating] = useState(false)
  const [result, setResult] = useState(null)
  const [fileContent, setFileContent] = useState('')

  const handleValidate = async () => {
    // Validation logic here
  }

  return (
    <div className="container mx-auto py-8 px-4">
      <h1 className="text-3xl font-bold mb-8">Validate OSCAL Document</h1>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left Column - Upload & Settings */}
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Step 1: Select Model Type</CardTitle>
            </CardHeader>
            <CardContent>
              <Select value={modelType} onValueChange={setModelType}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="catalog">📘 Catalog</SelectItem>
                  <SelectItem value="profile">📋 Profile</SelectItem>
                  <SelectItem value="component-definition">🔧 Component Definition</SelectItem>
                  <SelectItem value="ssp">📄 System Security Plan</SelectItem>
                  <SelectItem value="assessment-plan">📝 Assessment Plan</SelectItem>
                  <SelectItem value="assessment-results">✅ Assessment Results</SelectItem>
                  <SelectItem value="poam">🎯 POA&M</SelectItem>
                </SelectContent>
              </Select>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Step 2: Upload File</CardTitle>
            </CardHeader>
            <CardContent>
              <FileUploader
                onFilesSelected={(files) => setFile(files[0])}
                accept=".xml,.json,.yaml,.yml"
              />
              {file && (
                <div className="mt-4 p-3 bg-accent rounded-md flex items-center justify-between">
                  <span className="text-sm">{file.name}</span>
                  <Button variant="ghost" size="sm" onClick={() => setFile(null)}>
                    Remove
                  </Button>
                </div>
              )}
            </CardContent>
          </Card>

          <Button
            onClick={handleValidate}
            disabled={!file || isValidating}
            className="w-full"
            size="lg"
          >
            {isValidating && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {isValidating ? 'Validating...' : 'Validate Document'}
          </Button>
        </div>

        {/* Right Column - Results */}
        <div className="space-y-6">
          {fileContent && (
            <Card>
              <CardHeader>
                <CardTitle>File Content</CardTitle>
              </CardHeader>
              <CardContent>
                <CodeEditor
                  value={fileContent}
                  language="xml"
                  readOnly
                  height="400px"
                />
              </CardContent>
            </Card>
          )}

          {result && (
            <ValidationResultsPanel
              result={result}
              onErrorClick={(line) => {
                // Jump to line in editor
              }}
            />
          )}
        </div>
      </div>
    </div>
  )
}
```

### 3. Convert Page (`app/convert/page.tsx`)

Side-by-side editor view with format conversion.

```tsx
'use client'

import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Select } from '@/components/ui/select'
import { CodeEditor } from '@/components/code-editor'
import { ArrowRight, Download, Copy } from 'lucide-react'

export default function ConvertPage() {
  return (
    <div className="container mx-auto py-8 px-4">
      <h1 className="text-3xl font-bold mb-8">Convert OSCAL Document</h1>

      {/* Configuration */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Conversion Settings</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-4">
            <Select /* Model Type */></Select>
            <Select /* From Format */></Select>
            <ArrowRight className="text-muted-foreground" />
            <Select /* To Format */></Select>
            <Button>Convert</Button>
          </div>
        </CardContent>
      </Card>

      {/* Side-by-Side Editors */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Source (XML)</CardTitle>
          </CardHeader>
          <CardContent>
            <CodeEditor language="xml" height="600px" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle>Converted (JSON)</CardTitle>
              <div className="flex gap-2">
                <Button variant="outline" size="sm">
                  <Copy className="h-4 w-4 mr-2" />
                  Copy
                </Button>
                <Button variant="outline" size="sm">
                  <Download className="h-4 w-4 mr-2" />
                  Download
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <CodeEditor language="json" height="600px" readOnly />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
```

## ShadCN Component Library

### Core Components Used

#### 1. Button
```tsx
import { Button } from '@/components/ui/button'

// Variants
<Button variant="default">Primary Action</Button>
<Button variant="secondary">Secondary</Button>
<Button variant="outline">Outlined</Button>
<Button variant="ghost">Ghost</Button>
<Button variant="destructive">Delete</Button>

// Sizes
<Button size="sm">Small</Button>
<Button size="default">Default</Button>
<Button size="lg">Large</Button>
```

#### 2. Card
```tsx
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'

<Card>
  <CardHeader>
    <CardTitle>Title</CardTitle>
    <CardDescription>Description</CardDescription>
  </CardHeader>
  <CardContent>Content</CardContent>
  <CardFooter>Actions</CardFooter>
</Card>
```

#### 3. Badge
```tsx
import { Badge } from '@/components/ui/badge'

<Badge>Default</Badge>
<Badge variant="secondary">Secondary</Badge>
<Badge variant="destructive">Error</Badge>
<Badge variant="outline">Outline</Badge>
```

#### 4. Alert
```tsx
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { AlertCircle } from 'lucide-react'

<Alert variant="destructive">
  <AlertCircle className="h-4 w-4" />
  <AlertTitle>Error</AlertTitle>
  <AlertDescription>Your validation failed</AlertDescription>
</Alert>
```

#### 5. Progress
```tsx
import { Progress } from '@/components/ui/progress'

<Progress value={progress} className="w-full" />
```

#### 6. Select
```tsx
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

<Select value={value} onValueChange={setValue}>
  <SelectTrigger>
    <SelectValue placeholder="Select..." />
  </SelectTrigger>
  <SelectContent>
    <SelectItem value="option1">Option 1</SelectItem>
    <SelectItem value="option2">Option 2</SelectItem>
  </SelectContent>
</Select>
```

## User Flows

### Flow 1: Validate a Document

```
1. User navigates to Dashboard
2. Clicks "Validate" card
3. Lands on /validate
4. Selects model type from dropdown
5. Drags OSCAL file into upload zone
6. File preview loads in left panel
7. Clicks "Validate Document" button
8. Loading spinner appears
9. Results appear in right panel:
   - Success: Green checkmark, validation stats
   - Failure: Error list with line numbers
10. User clicks error to jump to line
11. Can download validation report
```

### Flow 2: Batch Validate

```
1. User clicks "Batch" from dashboard
2. Selects "Validate" operation
3. Selects model type
4. Drags multiple files (up to 10)
5. Files listed with status "Pending"
6. Clicks "Start Batch Operation"
7. Progress bar shows overall progress
8. Each file shows individual status:
   - ⏳ Processing
   - ✓ Success
   - ✗ Error
9. WebSocket updates in real-time
10. When complete, summary shows:
    - Success count
    - Error count
    - Download all results button
```

## Responsive Design

### Breakpoints (Tailwind)

- **Mobile**: `< 768px` (sm)
- **Tablet**: `768px - 1024px` (md)
- **Desktop**: `> 1024px` (lg, xl, 2xl)

### Mobile Adaptations

```tsx
// Stack layout on mobile, side-by-side on desktop
<div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
  {/* Content */}
</div>

// Hide sidebar on mobile
<aside className="hidden lg:block">
  {/* Sidebar */}
</aside>

// Responsive text sizes
<h1 className="text-2xl md:text-3xl lg:text-4xl font-bold">
  Title
</h1>
```

## Accessibility

### WCAG 2.1 AA Compliance

1. **Keyboard Navigation**
   - All interactive elements accessible via Tab
   - Visible focus rings (Tailwind: `focus:ring-2 focus:ring-ring`)
   - Logical tab order

2. **Screen Reader Support**
   - Semantic HTML
   - ARIA labels where needed
   - Status announcements

3. **Visual**
   - High contrast dark theme
   - Text contrast ratio > 4.5:1
   - Icons with text labels
   - Focus indicators

### Example Accessible Component

```tsx
<button
  className="relative inline-flex items-center justify-center..."
  aria-label="Validate OSCAL document"
  aria-describedby="validate-help"
>
  Validate
</button>
<span id="validate-help" className="sr-only">
  Validates your OSCAL document against the schema
</span>

{/* Live region for status updates */}
<div role="status" aria-live="polite" aria-atomic="true">
  {statusMessage}
</div>
```

## Animation & Transitions

All animations use Tailwind's transition utilities:

```tsx
// Hover effects
className="transition-colors duration-200 hover:bg-accent/50"

// Loading states
<Loader2 className="animate-spin" />

// Fade in
className="animate-in fade-in duration-300"

// Slide up
className="animate-in slide-in-from-bottom duration-300"
```

## Future Enhancements

1. **Command Palette** (Cmd+K) - Quick navigation
2. **Keyboard Shortcuts** - Power user features
3. **Diff Viewer** - Compare documents
4. **Collaborative Features** - Share operations
5. **Custom Themes** - User-configurable dark themes
6. **Progressive Web App** - Offline capabilities

---

This design system provides a modern, accessible, and developer-friendly interface for OSCAL CLI operations using the latest Next.js and ShadCN UI patterns.
