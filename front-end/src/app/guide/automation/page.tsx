import Link from 'next/link';
import { ArrowLeft, Terminal, Code2 } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';

export default function AutomationGuidePage() {
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-8 px-4 max-w-5xl">
        {/* Back Button */}
        <Link
          href="/guide"
          className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground mb-6 transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded"
          aria-label="Navigate back to user guide"
        >
          <ArrowLeft className="h-4 w-4 mr-2" aria-hidden="true" />
          Back to User Guide
        </Link>

        {/* Header */}
        <header className="mb-8">
          <div className="flex items-center gap-3 mb-3">
            <Terminal className="h-8 w-8 text-primary" />
            <h1 className="text-4xl font-bold">API Automation Guide</h1>
          </div>
          <p className="text-lg text-muted-foreground">
            Learn how to automate OSCAL operations using our REST API with Python, Bash, curl, and TypeScript
          </p>
        </header>

        {/* Table of Contents */}
        <Card className="mb-8">
          <CardHeader>
            <CardTitle>Table of Contents</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
              <a href="#getting-started" className="text-primary hover:underline">Getting Started</a>
              <a href="#authentication" className="text-primary hover:underline">Authentication</a>
              <a href="#validate" className="text-primary hover:underline">Validate Documents</a>
              <a href="#convert" className="text-primary hover:underline">Convert Formats</a>
              <a href="#resolve" className="text-primary hover:underline">Resolve Profiles</a>
              <a href="#batch" className="text-primary hover:underline">Batch Operations</a>
              <a href="#file-management" className="text-primary hover:underline">File Management</a>
              <a href="#error-handling" className="text-primary hover:underline">Error Handling</a>
              <a href="#best-practices" className="text-primary hover:underline">Best Practices</a>
              <a href="#ci-cd" className="text-primary hover:underline">CI/CD Integration</a>
            </div>
          </CardContent>
        </Card>

        {/* Content */}
        <div className="space-y-8">
          {/* Getting Started */}
          <Card id="getting-started">
            <CardHeader>
              <CardTitle>Getting Started</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <h3 className="text-xl font-semibold mb-3">API Base URL</h3>
                <p className="text-muted-foreground mb-2">
                  All API endpoints are relative to the base URL:
                </p>
                <code className="block bg-muted p-3 rounded text-sm font-mono">
                  http://localhost:8080/api
                </code>
                <p className="text-sm text-muted-foreground mt-2">
                  For production deployments, replace <code className="bg-muted px-1.5 py-0.5 rounded">localhost:8080</code> with your actual API host.
                </p>
              </div>

              <div>
                <h3 className="text-xl font-semibold mb-3">Available Endpoints</h3>
                <ul className="space-y-2 text-muted-foreground">
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">POST /api/validate</code> - Validate OSCAL documents</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">POST /api/convert</code> - Convert document formats</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">POST /api/profile/resolve</code> - Resolve OSCAL profiles</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">POST /api/batch</code> - Process multiple files</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">GET /api/batch/:operationId</code> - Get batch operation status</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">GET /api/files</code> - List saved files</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">GET /api/files/:fileId</code> - Get file metadata</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">GET /api/files/:fileId/content</code> - Get file content</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">POST /api/files</code> - Upload and save file</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">DELETE /api/files/:fileId</code> - Delete file</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">GET /api/health</code> - Health check</li>
                </ul>
              </div>

              <div>
                <h3 className="text-xl font-semibold mb-3">OSCAL Model Types</h3>
                <p className="text-muted-foreground mb-2">
                  The following model types are supported:
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">catalog</code></li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">profile</code></li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">component-definition</code></li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">system-security-plan</code></li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">assessment-plan</code></li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">assessment-results</code></li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">plan-of-action-and-milestones</code></li>
                </ul>
              </div>

              <div>
                <h3 className="text-xl font-semibold mb-3">Supported Formats</h3>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">JSON</code></li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">XML</code></li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">YAML</code></li>
                </ul>
              </div>
            </CardContent>
          </Card>

          {/* Authentication */}
          <Card id="authentication">
            <CardHeader>
              <CardTitle>Authentication</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-muted-foreground">
                All API requests require authentication using a service account token. You can generate tokens
                from the Profile page in the web interface.
              </p>

              <div>
                <h3 className="text-xl font-semibold mb-3">Creating a Service Account Token</h3>
                <ol className="list-decimal list-inside space-y-2 text-muted-foreground">
                  <li>Log in to the web interface</li>
                  <li>Click your username and select &quot;Profile&quot;</li>
                  <li>Scroll to &quot;Service Account Tokens&quot;</li>
                  <li>Enter a name and expiration period (1-3650 days)</li>
                  <li>Click &quot;Generate Service Account Token&quot;</li>
                  <li>Copy the token immediately (it cannot be retrieved later)</li>
                </ol>
              </div>

              <div>
                <h3 className="text-xl font-semibold mb-3">Using the Token</h3>
                <p className="text-muted-foreground mb-3">
                  Include the token in the <code className="bg-muted px-1.5 py-0.5 rounded">Authorization</code> header:
                </p>
                <code className="block bg-muted p-3 rounded text-sm font-mono">
                  Authorization: Bearer YOUR_TOKEN_HERE
                </code>
              </div>

              <div className="bg-amber-500/10 border border-amber-500/20 p-4 rounded">
                <p className="text-sm font-semibold mb-2 text-amber-600 dark:text-amber-400">Security Best Practices:</p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Store tokens in environment variables or secure vaults</li>
                  <li>Never commit tokens to version control</li>
                  <li>Use different tokens for different environments</li>
                  <li>Rotate tokens regularly</li>
                  <li>Revoke tokens immediately if compromised</li>
                </ul>
              </div>
            </CardContent>
          </Card>

          {/* Validate Documents */}
          <Card id="validate">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Code2 className="h-5 w-5" />
                Validate Documents
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <p className="text-muted-foreground">
                Validate OSCAL documents against their schema to ensure compliance.
              </p>

              <div>
                <h3 className="text-lg font-semibold mb-3">Endpoint</h3>
                <code className="block bg-muted p-3 rounded text-sm font-mono">
                  POST /api/validate
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Request Body</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`{
  "content": "... OSCAL document content as string ...",
  "modelType": "catalog",
  "format": "JSON",
  "fileName": "my-catalog.json"
}`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example: curl</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`curl -X POST http://localhost:8080/api/validate \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \\
  -d '{
    "content": "{\\"catalog\\": {\\"uuid\\": \\"123\\", \\"metadata\\": {...}}}",
    "modelType": "catalog",
    "format": "JSON",
    "fileName": "my-catalog.json"
  }'`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example: Python</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`import requests
import json
import os

# Load your OSCAL document
with open('my-catalog.json', 'r') as f:
    content = f.read()

# API configuration
API_BASE_URL = 'http://localhost:8080/api'
TOKEN = os.getenv('OSCAL_API_TOKEN')  # Store token in environment variable

# Validate the document
response = requests.post(
    f'{API_BASE_URL}/validate',
    headers={
        'Content-Type': 'application/json',
        'Authorization': f'Bearer {TOKEN}'
    },
    json={
        'content': content,
        'modelType': 'catalog',
        'format': 'JSON',
        'fileName': 'my-catalog.json'
    }
)

result = response.json()

if result['valid']:
    print('✓ Document is valid!')
else:
    print('✗ Validation failed:')
    for error in result['errors']:
        print(f"  Line {error['line']}: {error['message']}")
        print(f"  Path: {error['path']}")`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example: Bash Script</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`#!/bin/bash

# Configuration
API_BASE_URL="http://localhost:8080/api"
TOKEN="\${OSCAL_API_TOKEN}"
FILE_PATH="my-catalog.json"

# Read file content (escape quotes for JSON)
CONTENT=$(cat "$FILE_PATH" | jq -Rs .)

# Create request body
REQUEST_BODY=$(cat <<EOF
{
  "content": $CONTENT,
  "modelType": "catalog",
  "format": "JSON",
  "fileName": "$FILE_PATH"
}
EOF
)

# Validate document
response=$(curl -s -X POST "$API_BASE_URL/validate" \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer $TOKEN" \\
  -d "$REQUEST_BODY")

# Check if valid
is_valid=$(echo "$response" | jq -r '.valid')

if [ "$is_valid" = "true" ]; then
  echo "✓ Document is valid!"
  exit 0
else
  echo "✗ Validation failed:"
  echo "$response" | jq -r '.errors[] | "  Line \\(.line): \\(.message)"'
  exit 1
fi`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example: TypeScript</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`import * as fs from 'fs';

interface ValidationRequest {
  content: string;
  modelType: string;
  format: string;
  fileName?: string;
}

interface ValidationError {
  line: number;
  column: number;
  message: string;
  severity: string;
  path: string;
}

interface ValidationResult {
  valid: boolean;
  errors: ValidationError[];
  fileName: string;
  modelType: string;
  format: string;
  timestamp: string;
}

async function validateDocument(
  filePath: string,
  modelType: string,
  format: string
): Promise<ValidationResult> {
  const API_BASE_URL = 'http://localhost:8080/api';
  const TOKEN = process.env.OSCAL_API_TOKEN;

  // Read file content
  const content = fs.readFileSync(filePath, 'utf-8');

  // Create request
  const request: ValidationRequest = {
    content,
    modelType,
    format,
    fileName: filePath
  };

  // Send validation request
  const response = await fetch(\`\${API_BASE_URL}/validate\`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': \`Bearer \${TOKEN}\`
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    throw new Error(\`API request failed: \${response.statusText}\`);
  }

  return await response.json() as ValidationResult;
}

// Usage
(async () => {
  try {
    const result = await validateDocument(
      'my-catalog.json',
      'catalog',
      'JSON'
    );

    if (result.valid) {
      console.log('✓ Document is valid!');
    } else {
      console.log('✗ Validation failed:');
      result.errors.forEach(error => {
        console.log(\`  Line \${error.line}: \${error.message}\`);
        console.log(\`  Path: \${error.path}\`);
      });
    }
  } catch (error) {
    console.error('Error:', error);
    process.exit(1);
  }
})();`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Response Format</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`{
  "valid": true,
  "errors": [],
  "fileName": "my-catalog.json",
  "modelType": "catalog",
  "format": "JSON",
  "timestamp": "2025-10-20T14:30:00Z"
}

// Or if invalid:
{
  "valid": false,
  "errors": [
    {
      "line": 15,
      "column": 8,
      "message": "Missing required field 'uuid'",
      "severity": "ERROR",
      "path": "/catalog/metadata"
    }
  ],
  "fileName": "my-catalog.json",
  "modelType": "catalog",
  "format": "JSON",
  "timestamp": "2025-10-20T14:30:00Z"
}`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Convert Formats */}
          <Card id="convert">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Code2 className="h-5 w-5" />
                Convert Formats
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <p className="text-muted-foreground">
                Convert OSCAL documents between XML, JSON, and YAML formats.
              </p>

              <div>
                <h3 className="text-lg font-semibold mb-3">Endpoint</h3>
                <code className="block bg-muted p-3 rounded text-sm font-mono">
                  POST /api/convert
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Request Body</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`{
  "content": "... OSCAL document content ...",
  "fromFormat": "XML",
  "toFormat": "JSON",
  "modelType": "catalog",
  "fileName": "my-catalog.xml"
}`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example: curl</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`curl -X POST http://localhost:8080/api/convert \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \\
  -d '{
    "content": "<catalog>...</catalog>",
    "fromFormat": "XML",
    "toFormat": "JSON",
    "modelType": "catalog",
    "fileName": "my-catalog.xml"
  }'`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example: Python</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`import requests
import os

def convert_oscal_format(
    input_file: str,
    from_format: str,
    to_format: str,
    model_type: str,
    output_file: str
):
    """Convert OSCAL document from one format to another."""

    API_BASE_URL = 'http://localhost:8080/api'
    TOKEN = os.getenv('OSCAL_API_TOKEN')

    # Read input file
    with open(input_file, 'r') as f:
        content = f.read()

    # Convert
    response = requests.post(
        f'{API_BASE_URL}/convert',
        headers={
            'Content-Type': 'application/json',
            'Authorization': f'Bearer {TOKEN}'
        },
        json={
            'content': content,
            'fromFormat': from_format,
            'toFormat': to_format,
            'modelType': model_type,
            'fileName': input_file
        }
    )

    result = response.json()

    if result['success']:
        # Save converted content
        with open(output_file, 'w') as f:
            f.write(result['convertedContent'])
        print(f'✓ Converted {input_file} from {from_format} to {to_format}')
        print(f'  Output: {output_file}')
    else:
        print(f'✗ Conversion failed: {result.get("error", "Unknown error")}')
        return False

    return True

# Usage
convert_oscal_format(
    input_file='catalog.xml',
    from_format='XML',
    to_format='JSON',
    model_type='catalog',
    output_file='catalog.json'
)`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example: Bash Script</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`#!/bin/bash

# Convert OSCAL document format
# Usage: ./convert.sh input.xml JSON catalog output.json

INPUT_FILE="$1"
TO_FORMAT="$2"
MODEL_TYPE="$3"
OUTPUT_FILE="$4"

API_BASE_URL="http://localhost:8080/api"
TOKEN="\${OSCAL_API_TOKEN}"

# Detect input format from extension
case "$INPUT_FILE" in
  *.xml) FROM_FORMAT="XML" ;;
  *.json) FROM_FORMAT="JSON" ;;
  *.yaml|*.yml) FROM_FORMAT="YAML" ;;
  *) echo "Error: Unknown file format"; exit 1 ;;
esac

# Read and escape content
CONTENT=$(cat "$INPUT_FILE" | jq -Rs .)

# Convert
response=$(curl -s -X POST "$API_BASE_URL/convert" \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer $TOKEN" \\
  -d "{
    \\"content\\": $CONTENT,
    \\"fromFormat\\": \\"$FROM_FORMAT\\",
    \\"toFormat\\": \\"$TO_FORMAT\\",
    \\"modelType\\": \\"$MODEL_TYPE\\",
    \\"fileName\\": \\"$INPUT_FILE\\"
  }")

# Check success
success=$(echo "$response" | jq -r '.success')

if [ "$success" = "true" ]; then
  echo "$response" | jq -r '.convertedContent' > "$OUTPUT_FILE"
  echo "✓ Converted $INPUT_FILE from $FROM_FORMAT to $TO_FORMAT"
  echo "  Output: $OUTPUT_FILE"
else
  echo "✗ Conversion failed:"
  echo "$response" | jq -r '.error'
  exit 1
fi`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example: TypeScript</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`import * as fs from 'fs';
import * as path from 'path';

interface ConversionRequest {
  content: string;
  fromFormat: string;
  toFormat: string;
  modelType: string;
  fileName?: string;
}

interface ConversionResult {
  success: boolean;
  convertedContent?: string;
  error?: string;
  fileName: string;
  modelType: string;
  originalFormat: string;
  convertedFormat: string;
  timestamp: string;
}

async function convertFormat(
  inputFile: string,
  toFormat: string,
  modelType: string,
  outputFile: string
): Promise<void> {
  const API_BASE_URL = 'http://localhost:8080/api';
  const TOKEN = process.env.OSCAL_API_TOKEN;

  // Detect input format from extension
  const ext = path.extname(inputFile).toLowerCase();
  const formatMap: Record<string, string> = {
    '.xml': 'XML',
    '.json': 'JSON',
    '.yaml': 'YAML',
    '.yml': 'YAML'
  };
  const fromFormat = formatMap[ext];

  if (!fromFormat) {
    throw new Error(\`Unknown file format: \${ext}\`);
  }

  // Read file
  const content = fs.readFileSync(inputFile, 'utf-8');

  // Convert
  const response = await fetch(\`\${API_BASE_URL}/convert\`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': \`Bearer \${TOKEN}\`
    },
    body: JSON.stringify({
      content,
      fromFormat,
      toFormat,
      modelType,
      fileName: inputFile
    } as ConversionRequest)
  });

  if (!response.ok) {
    throw new Error(\`API request failed: \${response.statusText}\`);
  }

  const result = await response.json() as ConversionResult;

  if (result.success && result.convertedContent) {
    fs.writeFileSync(outputFile, result.convertedContent, 'utf-8');
    console.log(\`✓ Converted \${inputFile} from \${fromFormat} to \${toFormat}\`);
    console.log(\`  Output: \${outputFile}\`);
  } else {
    throw new Error(\`Conversion failed: \${result.error || 'Unknown error'}\`);
  }
}

// Usage
(async () => {
  try {
    await convertFormat(
      'catalog.xml',
      'JSON',
      'catalog',
      'catalog.json'
    );
  } catch (error) {
    console.error('Error:', error);
    process.exit(1);
  }
})();`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Resolve Profiles */}
          <Card id="resolve">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Code2 className="h-5 w-5" />
                Resolve Profiles
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <p className="text-muted-foreground">
                Resolve OSCAL profiles into fully resolved catalogs.
              </p>

              <div>
                <h3 className="text-lg font-semibold mb-3">Endpoint</h3>
                <code className="block bg-muted p-3 rounded text-sm font-mono">
                  POST /api/profile/resolve
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Request Body</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`{
  "profileContent": "... OSCAL profile content ...",
  "format": "JSON"
}`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example: curl</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`curl -X POST http://localhost:8080/api/profile/resolve \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \\
  -d '{
    "profileContent": "{\\"profile\\": {...}}",
    "format": "JSON"
  }'`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example: Python</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`import requests
import os

def resolve_profile(profile_file: str, output_file: str, format: str = 'JSON'):
    """Resolve an OSCAL profile into a catalog."""

    API_BASE_URL = 'http://localhost:8080/api'
    TOKEN = os.getenv('OSCAL_API_TOKEN')

    # Read profile
    with open(profile_file, 'r') as f:
        profile_content = f.read()

    # Resolve
    response = requests.post(
        f'{API_BASE_URL}/profile/resolve',
        headers={
            'Content-Type': 'application/json',
            'Authorization': f'Bearer {TOKEN}'
        },
        json={
            'profileContent': profile_content,
            'format': format
        }
    )

    result = response.json()

    if result['success']:
        with open(output_file, 'w') as f:
            f.write(result['resolvedCatalog'])
        print(f'✓ Profile resolved successfully')
        print(f'  Output: {output_file}')
    else:
        print(f'✗ Resolution failed: {result.get("error", "Unknown error")}')
        return False

    return True

# Usage
resolve_profile('my-profile.json', 'resolved-catalog.json', 'JSON')`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Batch Operations */}
          <Card id="batch">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Code2 className="h-5 w-5" />
                Batch Operations
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <p className="text-muted-foreground">
                Process multiple OSCAL files in a single batch operation (validate or convert).
              </p>

              <div>
                <h3 className="text-lg font-semibold mb-3">Endpoint</h3>
                <code className="block bg-muted p-3 rounded text-sm font-mono">
                  POST /api/batch
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example: Python Batch Validation</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`import requests
import os
import glob

def batch_validate(file_pattern: str, model_type: str):
    """Validate multiple OSCAL files in batch."""

    API_BASE_URL = 'http://localhost:8080/api'
    TOKEN = os.getenv('OSCAL_API_TOKEN')

    # Find all matching files
    files = glob.glob(file_pattern)
    print(f'Found {len(files)} files to validate')

    # Process each file
    results = []
    for file_path in files:
        print(f'\\nValidating {file_path}...')

        with open(file_path, 'r') as f:
            content = f.read()

        # Detect format
        if file_path.endswith('.xml'):
            format = 'XML'
        elif file_path.endswith('.json'):
            format = 'JSON'
        else:
            format = 'YAML'

        # Validate
        response = requests.post(
            f'{API_BASE_URL}/validate',
            headers={
                'Content-Type': 'application/json',
                'Authorization': f'Bearer {TOKEN}'
            },
            json={
                'content': content,
                'modelType': model_type,
                'format': format,
                'fileName': file_path
            }
        )

        result = response.json()
        results.append({
            'file': file_path,
            'valid': result['valid'],
            'errors': result.get('errors', [])
        })

        if result['valid']:
            print(f'  ✓ Valid')
        else:
            print(f'  ✗ Invalid ({len(result["errors"])} errors)')

    # Summary
    print(f'\\n{"="*60}')
    print('BATCH VALIDATION SUMMARY')
    print(f'{"="*60}')
    valid_count = sum(1 for r in results if r['valid'])
    print(f'Total: {len(results)} files')
    print(f'Valid: {valid_count}')
    print(f'Invalid: {len(results) - valid_count}')

    # Show errors
    for result in results:
        if not result['valid']:
            print(f'\\n{result["file"]}:')
            for error in result['errors'][:3]:  # Show first 3 errors
                print(f'  Line {error["line"]}: {error["message"]}')
            if len(result['errors']) > 3:
                print(f'  ... and {len(result["errors"]) - 3} more errors')

    return results

# Usage
batch_validate('oscal-files/*.json', 'catalog')`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* File Management */}
          <Card id="file-management">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Code2 className="h-5 w-5" />
                File Management
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <p className="text-muted-foreground">
                Manage saved OSCAL files through the API.
              </p>

              <div>
                <h3 className="text-lg font-semibold mb-3">List Files</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# curl
curl -X GET http://localhost:8080/api/files \\
  -H "Authorization: Bearer YOUR_TOKEN_HERE"

# Python
response = requests.get(
    f'{API_BASE_URL}/files',
    headers={'Authorization': f'Bearer {TOKEN}'}
)
files = response.json()
for file in files:
    print(f"{file['id']}: {file['fileName']} ({file['modelType']})")`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Upload File</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Python
with open('catalog.json', 'r') as f:
    content = f.read()

response = requests.post(
    f'{API_BASE_URL}/files',
    headers={
        'Content-Type': 'application/json',
        'Authorization': f'Bearer {TOKEN}'
    },
    json={
        'content': content,
        'fileName': 'catalog.json',
        'modelType': 'catalog',
        'format': 'JSON'
    }
)

saved_file = response.json()
print(f"File uploaded: {saved_file['id']}")`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Get File Content</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Python
file_id = 'abc123'
response = requests.get(
    f'{API_BASE_URL}/files/{file_id}/content',
    headers={'Authorization': f'Bearer {TOKEN}'}
)

content = response.json()['content']
print(content)`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Delete File</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`# Python
file_id = 'abc123'
response = requests.delete(
    f'{API_BASE_URL}/files/{file_id}',
    headers={'Authorization': f'Bearer {TOKEN}'}
)

if response.status_code == 200:
    print(f"File {file_id} deleted successfully")`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Error Handling */}
          <Card id="error-handling">
            <CardHeader>
              <CardTitle>Error Handling</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <h3 className="text-lg font-semibold mb-3">HTTP Status Codes</h3>
                <ul className="space-y-2 text-muted-foreground">
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">200 OK</code> - Request successful</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">400 Bad Request</code> - Invalid request parameters</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">401 Unauthorized</code> - Missing or invalid authentication token</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">404 Not Found</code> - Resource not found</li>
                  <li><code className="bg-muted px-1.5 py-0.5 rounded">500 Internal Server Error</code> - Server error</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example: Python Error Handling</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`import requests
from requests.exceptions import RequestException

def validate_with_error_handling(file_path: str, model_type: str, format: str):
    """Validate OSCAL document with comprehensive error handling."""

    API_BASE_URL = 'http://localhost:8080/api'
    TOKEN = os.getenv('OSCAL_API_TOKEN')

    try:
        # Read file
        with open(file_path, 'r') as f:
            content = f.read()

        # Validate
        response = requests.post(
            f'{API_BASE_URL}/validate',
            headers={
                'Content-Type': 'application/json',
                'Authorization': f'Bearer {TOKEN}'
            },
            json={
                'content': content,
                'modelType': model_type,
                'format': format,
                'fileName': file_path
            },
            timeout=30  # 30 second timeout
        )

        # Check for HTTP errors
        if response.status_code == 401:
            print('Error: Invalid or expired authentication token')
            return False
        elif response.status_code == 400:
            print('Error: Invalid request parameters')
            print(response.json())
            return False
        elif response.status_code != 200:
            print(f'Error: API returned status code {response.status_code}')
            return False

        # Parse result
        result = response.json()

        if result['valid']:
            print(f'✓ {file_path} is valid')
            return True
        else:
            print(f'✗ {file_path} has validation errors:')
            for error in result['errors']:
                print(f"  Line {error['line']}: {error['message']}")
            return False

    except FileNotFoundError:
        print(f'Error: File not found: {file_path}')
        return False
    except RequestException as e:
        print(f'Error: Network request failed: {e}')
        return False
    except ValueError as e:
        print(f'Error: Invalid JSON response: {e}')
        return False
    except Exception as e:
        print(f'Error: Unexpected error: {e}')
        return False

# Usage
validate_with_error_handling('catalog.json', 'catalog', 'JSON')`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Best Practices */}
          <Card id="best-practices">
            <CardHeader>
              <CardTitle>Best Practices</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <h3 className="text-lg font-semibold mb-2">Security</h3>
                <ul className="list-disc list-inside space-y-2 text-muted-foreground ml-4">
                  <li>Always store API tokens in environment variables or secure vaults</li>
                  <li>Never hardcode tokens in source code</li>
                  <li>Use HTTPS in production environments</li>
                  <li>Implement token rotation policies</li>
                  <li>Use different tokens for different environments (dev, staging, prod)</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-2 mt-6">Performance</h3>
                <ul className="list-disc list-inside space-y-2 text-muted-foreground ml-4">
                  <li>Use batch operations when processing multiple files</li>
                  <li>Set appropriate timeouts for API requests</li>
                  <li>Implement retry logic with exponential backoff</li>
                  <li>Cache validation results when appropriate</li>
                  <li>Process large files asynchronously</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-2 mt-6">Reliability</h3>
                <ul className="list-disc list-inside space-y-2 text-muted-foreground ml-4">
                  <li>Always check HTTP status codes before parsing responses</li>
                  <li>Implement comprehensive error handling</li>
                  <li>Log API requests and responses for debugging</li>
                  <li>Validate input data before sending to API</li>
                  <li>Handle network timeouts gracefully</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-2 mt-6">Example: Retry Logic with Exponential Backoff</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`import time
import requests
from typing import Optional

def api_request_with_retry(
    url: str,
    method: str = 'POST',
    max_retries: int = 3,
    **kwargs
) -> Optional[requests.Response]:
    """Make API request with exponential backoff retry."""

    for attempt in range(max_retries):
        try:
            response = requests.request(method, url, **kwargs)

            # Success - return response
            if response.status_code == 200:
                return response

            # Don't retry client errors (400-499)
            if 400 <= response.status_code < 500:
                return response

            # Retry server errors (500-599)
            if response.status_code >= 500:
                wait_time = 2 ** attempt  # 1s, 2s, 4s
                print(f'Server error, retrying in {wait_time}s...')
                time.sleep(wait_time)
                continue

        except requests.exceptions.RequestException as e:
            if attempt == max_retries - 1:
                raise
            wait_time = 2 ** attempt
            print(f'Request failed, retrying in {wait_time}s...')
            time.sleep(wait_time)

    return None

# Usage
response = api_request_with_retry(
    f'{API_BASE_URL}/validate',
    method='POST',
    headers={
        'Content-Type': 'application/json',
        'Authorization': f'Bearer {TOKEN}'
    },
    json=request_body,
    timeout=30
)`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* CI/CD Integration */}
          <Card id="ci-cd">
            <CardHeader>
              <CardTitle>CI/CD Integration</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <p className="text-muted-foreground">
                Integrate OSCAL validation and processing into your continuous integration and deployment pipelines.
              </p>

              <div>
                <h3 className="text-lg font-semibold mb-3">GitHub Actions Example</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`name: Validate OSCAL Documents

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  validate:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v3

    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.11'

    - name: Install dependencies
      run: |
        pip install requests

    - name: Validate OSCAL files
      env:
        OSCAL_API_TOKEN: \${{ secrets.OSCAL_API_TOKEN }}
        OSCAL_API_URL: \${{ secrets.OSCAL_API_URL }}
      run: |
        python scripts/validate_all.py

    - name: Upload validation results
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: validation-results
        path: validation-results.json`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">GitLab CI Example</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`validate-oscal:
  image: python:3.11
  stage: test

  before_script:
    - pip install requests

  script:
    - python scripts/validate_all.py

  variables:
    OSCAL_API_TOKEN: $OSCAL_API_TOKEN
    OSCAL_API_URL: $OSCAL_API_URL

  artifacts:
    reports:
      junit: validation-results.xml
    paths:
      - validation-results.json
    when: always

  only:
    - main
    - merge_requests`}
                </code>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-3">Example Validation Script for CI/CD</h3>
                <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`#!/usr/bin/env python3
"""
validate_all.py - Validate all OSCAL files in repository for CI/CD
"""

import os
import sys
import glob
import json
import requests
from pathlib import Path

def validate_all_oscal_files():
    """Validate all OSCAL files and return exit code."""

    API_BASE_URL = os.getenv('OSCAL_API_URL', 'http://localhost:8080/api')
    TOKEN = os.getenv('OSCAL_API_TOKEN')

    if not TOKEN:
        print('Error: OSCAL_API_TOKEN environment variable not set')
        return 1

    # Find all OSCAL files
    patterns = ['**/*.json', '**/*.xml', '**/*.yaml']
    files = []
    for pattern in patterns:
        files.extend(glob.glob(pattern, recursive=True))

    # Filter to only OSCAL files (check for common OSCAL keywords)
    oscal_files = []
    for file_path in files:
        with open(file_path, 'r') as f:
            content = f.read()
            if any(kw in content for kw in ['catalog', 'profile', 'component-definition',
                                             'system-security-plan', 'assessment-plan']):
                oscal_files.append(file_path)

    print(f'Found {len(oscal_files)} OSCAL files to validate')

    # Validate each file
    results = []
    failed_count = 0

    for file_path in oscal_files:
        print(f'\\nValidating {file_path}...')

        with open(file_path, 'r') as f:
            content = f.read()

        # Detect format and model type
        if file_path.endswith('.xml'):
            format = 'XML'
        elif file_path.endswith('.json'):
            format = 'JSON'
        else:
            format = 'YAML'

        # Detect model type (simplified)
        model_type = 'catalog'  # Default
        if 'profile' in content.lower():
            model_type = 'profile'
        elif 'system-security-plan' in content.lower():
            model_type = 'system-security-plan'

        # Validate
        try:
            response = requests.post(
                f'{API_BASE_URL}/validate',
                headers={
                    'Content-Type': 'application/json',
                    'Authorization': f'Bearer {TOKEN}'
                },
                json={
                    'content': content,
                    'modelType': model_type,
                    'format': format,
                    'fileName': file_path
                },
                timeout=60
            )

            if response.status_code != 200:
                print(f'  ✗ API error: {response.status_code}')
                failed_count += 1
                continue

            result = response.json()
            results.append({
                'file': file_path,
                'valid': result['valid'],
                'errors': result.get('errors', [])
            })

            if result['valid']:
                print(f'  ✓ Valid')
            else:
                print(f'  ✗ Invalid ({len(result["errors"])} errors)')
                for error in result['errors'][:3]:
                    print(f'    Line {error["line"]}: {error["message"]}')
                failed_count += 1

        except Exception as e:
            print(f'  ✗ Error: {e}')
            failed_count += 1

    # Save results
    with open('validation-results.json', 'w') as f:
        json.dump(results, f, indent=2)

    # Summary
    print(f'\\n{"="*60}')
    print('VALIDATION SUMMARY')
    print(f'{"="*60}')
    print(f'Total files: {len(oscal_files)}')
    print(f'Valid: {len(oscal_files) - failed_count}')
    print(f'Invalid: {failed_count}')

    # Return exit code
    return 1 if failed_count > 0 else 0

if __name__ == '__main__':
    sys.exit(validate_all_oscal_files())`}
                </code>
              </div>
            </CardContent>
          </Card>

          {/* Additional Resources */}
          <Card>
            <CardHeader>
              <CardTitle>Additional Resources</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <a
                href="http://localhost:8080/swagger-ui/index.html"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                Interactive API Documentation (Swagger UI)
              </a>
              <p className="text-xs text-muted-foreground ml-6">
                Test API endpoints directly in your browser with the interactive Swagger UI
              </p>

              <Link
                href="/guide"
                className="flex items-center text-primary hover:underline"
              >
                User Guide
              </Link>
              <p className="text-xs text-muted-foreground ml-6">
                Complete guide to using the web interface
              </p>

              <a
                href="https://pages.nist.gov/OSCAL/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                NIST OSCAL Documentation
              </a>
              <p className="text-xs text-muted-foreground ml-6">
                Official OSCAL documentation and specifications
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
