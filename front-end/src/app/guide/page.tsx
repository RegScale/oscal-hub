import Link from 'next/link';
import { ArrowLeft, ExternalLink } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';

export default function UserGuidePage() {
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-8 px-4 max-w-4xl" id="main-content">
        {/* Back Button */}
        <Link
          href="/"
          className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground mb-6 transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 rounded"
          aria-label="Navigate back to dashboard"
        >
          <ArrowLeft className="h-4 w-4 mr-2" aria-hidden="true" />
          Back to Dashboard
        </Link>

        {/* Header */}
        <header className="mb-8">
          <h1 className="text-4xl font-bold mb-3">User Guide</h1>
          <p className="text-lg text-muted-foreground">
            Complete guide to using the OSCAL UX interface
          </p>
        </header>

        {/* Content */}
        <div className="space-y-8">
          {/* Getting Started */}
          <Card>
            <CardHeader>
              <CardTitle>Getting Started</CardTitle>
            </CardHeader>
            <CardContent className="prose prose-invert max-w-none">
              <h3 className="text-xl font-semibold mb-3">What is OSCAL UX?</h3>
              <p className="text-muted-foreground mb-4">
                OSCAL UX is a comprehensive web-based interface for working with OSCAL (Open Security Controls Assessment Language)
                documents. It provides a visual, user-friendly way to validate, convert, resolve, and process OSCAL content
                without needing command-line expertise.
              </p>

              <h3 className="text-xl font-semibold mb-3 mt-6">Requirements</h3>
              <ul className="list-disc list-inside space-y-2 text-muted-foreground">
                <li>Java 11 or higher (installed via SDKMAN)</li>
                <li>Maven 3.9 or higher (installed via SDKMAN)</li>
                <li>Node.js 18 or higher</li>
                <li>Modern web browser (Chrome, Firefox, Safari, Edge)</li>
              </ul>

              <h3 className="text-xl font-semibold mb-3 mt-6">Accessibility</h3>
              <p className="text-muted-foreground mb-2">
                OSCAL UX is fully accessible and compliant with WCAG 2.1 Level AA and Section 508 standards:
              </p>
              <ul className="list-disc list-inside space-y-2 text-muted-foreground">
                <li>Full keyboard navigation support (Tab, Enter, Escape)</li>
                <li>Screen reader compatible (NVDA, JAWS, VoiceOver)</li>
                <li>Skip navigation links for efficient browsing</li>
                <li>High contrast and readable text</li>
                <li>Semantic HTML and ARIA labels throughout</li>
              </ul>
            </CardContent>
          </Card>

          {/* Features */}
          <Card>
            <CardHeader>
              <CardTitle>Features</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="text-lg font-semibold mb-2">✅ Document Validation</h3>
                <p className="text-muted-foreground mb-2">
                  Validate OSCAL documents against official schemas to ensure compliance.
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Supports all 7 OSCAL model types</li>
                  <li>Works with JSON, XML, and YAML formats</li>
                  <li>Real-time error detection with line numbers</li>
                  <li>Automatic format detection</li>
                  <li>Document preview with syntax highlighting</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-2">✅ Format Conversion</h3>
                <p className="text-muted-foreground mb-2">
                  Convert OSCAL documents between XML, JSON, and YAML formats seamlessly.
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Side-by-side preview of original and converted content</li>
                  <li>Automatic format detection from file extension</li>
                  <li>One-click download of converted files</li>
                  <li>Real-time conversion feedback</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-2">✅ Profile Resolution</h3>
                <p className="text-muted-foreground mb-2">
                  Resolve OSCAL profiles into fully resolved catalogs with control selection and tailoring.
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Automatic profile resolution with catalog imports</li>
                  <li>Preview resolved catalog before download</li>
                  <li>Supports all OSCAL profile features</li>
                  <li>Choose output format (JSON, XML, YAML)</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-2">✅ Batch Processing</h3>
                <p className="text-muted-foreground mb-2">
                  Process multiple OSCAL files simultaneously with progress tracking.
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Upload multiple files at once</li>
                  <li>Real-time progress tracking for each file</li>
                  <li>Bulk validation, conversion, or resolution</li>
                  <li>Download all results as a ZIP archive</li>
                  <li>Clear error reporting for failed operations</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-2">✅ Operation History</h3>
                <p className="text-muted-foreground mb-2">
                  Track and manage all your OSCAL operations in one place.
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>View all past operations with timestamps</li>
                  <li>See success/failure status for each operation</li>
                  <li>Track operation duration and performance</li>
                  <li>Delete operations from history</li>
                  <li>Statistics dashboard with success rates</li>
                </ul>
              </div>
            </CardContent>
          </Card>

          {/* How to Use Validation */}
          <Card>
            <CardHeader>
              <CardTitle>How to Validate Documents</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <ol className="list-decimal list-inside space-y-4 text-muted-foreground">
                <li>
                  <span className="font-medium text-foreground">Navigate to Validate</span>
                  <p className="ml-6 mt-1">Click the &quot;Validate&quot; card on the dashboard.</p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Upload Your File</span>
                  <p className="ml-6 mt-1">
                    Drag and drop your OSCAL file onto the upload area, or click to browse.
                    The format (XML, JSON, YAML) will be auto-detected from the file extension.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Select Model Type</span>
                  <p className="ml-6 mt-1">
                    Choose the appropriate OSCAL model type from the dropdown:
                    Catalog, Profile, Component Definition, System Security Plan, Assessment Plan,
                    Assessment Results, or Plan of Action and Milestones.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Validate</span>
                  <p className="ml-6 mt-1">
                    Click &quot;Validate Document&quot;. The document will be checked against the OSCAL schema.
                    Any errors or warnings will be displayed with line numbers and detailed messages.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Review Results</span>
                  <p className="ml-6 mt-1">
                    Click on any error to jump to that line in the document preview.
                    A summary shows total errors and warnings detected.
                  </p>
                </li>
              </ol>
            </CardContent>
          </Card>

          {/* How to Use Convert */}
          <Card>
            <CardHeader>
              <CardTitle>How to Convert Document Formats</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <ol className="list-decimal list-inside space-y-4 text-muted-foreground">
                <li>
                  <span className="font-medium text-foreground">Navigate to Convert</span>
                  <p className="ml-6 mt-1">Click the &quot;Convert&quot; card on the dashboard.</p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Upload Your File</span>
                  <p className="ml-6 mt-1">
                    Upload your OSCAL document. The source format will be auto-detected.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Select Target Format</span>
                  <p className="ml-6 mt-1">
                    Choose the format you want to convert to (XML, JSON, or YAML).
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Convert</span>
                  <p className="ml-6 mt-1">
                    Click &quot;Convert Document&quot;. The converted document will appear in a side-by-side
                    view with the original, allowing you to compare them.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Download</span>
                  <p className="ml-6 mt-1">
                    Click &quot;Download Converted&quot; to save the converted file to your computer.
                  </p>
                </li>
              </ol>
            </CardContent>
          </Card>

          {/* How to Use Resolve */}
          <Card>
            <CardHeader>
              <CardTitle>How to Resolve Profiles</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <ol className="list-decimal list-inside space-y-4 text-muted-foreground">
                <li>
                  <span className="font-medium text-foreground">Navigate to Resolve</span>
                  <p className="ml-6 mt-1">Click the &quot;Resolve&quot; card on the dashboard.</p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Upload Your Profile</span>
                  <p className="ml-6 mt-1">
                    Upload an OSCAL profile document. The profile defines which controls to include
                    and how to tailor them from source catalogs.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Select Output Format</span>
                  <p className="ml-6 mt-1">
                    Choose the format for the resolved catalog (XML, JSON, or YAML).
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Resolve</span>
                  <p className="ml-6 mt-1">
                    Click &quot;Resolve Profile&quot;. The system will fetch referenced catalogs,
                    apply control selections and modifications, and generate a resolved catalog.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Download Resolved Catalog</span>
                  <p className="ml-6 mt-1">
                    Review the resolved catalog in the preview, then download it.
                  </p>
                </li>
              </ol>
            </CardContent>
          </Card>

          {/* How to Use Batch */}
          <Card>
            <CardHeader>
              <CardTitle>How to Process Multiple Files</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <ol className="list-decimal list-inside space-y-4 text-muted-foreground">
                <li>
                  <span className="font-medium text-foreground">Navigate to Batch</span>
                  <p className="ml-6 mt-1">Click the &quot;Batch&quot; card on the dashboard.</p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Upload Multiple Files</span>
                  <p className="ml-6 mt-1">
                    Drag and drop multiple OSCAL files, or click to select multiple files from your computer.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Choose Operation Type</span>
                  <p className="ml-6 mt-1">
                    Select what you want to do: Validate, Convert, or Resolve.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Configure Settings</span>
                  <p className="ml-6 mt-1">
                    Set model types, formats, and other options depending on the operation type.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Process</span>
                  <p className="ml-6 mt-1">
                    Click &quot;Process Files&quot;. Watch the progress in real-time as each file is processed.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Download Results</span>
                  <p className="ml-6 mt-1">
                    When complete, download all results as a ZIP file, or download individual files.
                  </p>
                </li>
              </ol>
            </CardContent>
          </Card>

          {/* How to Use History */}
          <Card>
            <CardHeader>
              <CardTitle>How to Use Operation History</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <ol className="list-decimal list-inside space-y-4 text-muted-foreground">
                <li>
                  <span className="font-medium text-foreground">Navigate to History</span>
                  <p className="ml-6 mt-1">Click the &quot;History&quot; card on the dashboard.</p>
                </li>
                <li>
                  <span className="font-medium text-foreground">View Statistics</span>
                  <p className="ml-6 mt-1">
                    See your operation statistics at the top: total operations, successful operations,
                    failed operations, and overall success rate.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Browse Operations</span>
                  <p className="ml-6 mt-1">
                    Scroll through the table of past operations. Each entry shows the operation type,
                    file name, status, model type, duration, and timestamp.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Delete Operations</span>
                  <p className="ml-6 mt-1">
                    Click the delete button on any operation to remove it from history.
                    You&apos;ll be asked to confirm before deletion.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Navigate Pages</span>
                  <p className="ml-6 mt-1">
                    Use the pagination controls at the bottom to browse through older operations.
                  </p>
                </li>
              </ol>
            </CardContent>
          </Card>

          {/* OSCAL Model Types */}
          <Card>
            <CardHeader>
              <CardTitle>OSCAL Model Types</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div>
                <h4 className="font-semibold">Catalog</h4>
                <p className="text-sm text-muted-foreground">Collection of security controls and control baselines</p>
              </div>
              <div>
                <h4 className="font-semibold">Profile</h4>
                <p className="text-sm text-muted-foreground">Selection and tailoring of controls from catalogs (can be resolved into a catalog)</p>
              </div>
              <div>
                <h4 className="font-semibold">Component Definition</h4>
                <p className="text-sm text-muted-foreground">Description of system components and their control implementations</p>
              </div>
              <div>
                <h4 className="font-semibold">System Security Plan (SSP)</h4>
                <p className="text-sm text-muted-foreground">Documentation of security implementation for a system</p>
              </div>
              <div>
                <h4 className="font-semibold">Assessment Plan</h4>
                <p className="text-sm text-muted-foreground">Plan for assessing system security controls</p>
              </div>
              <div>
                <h4 className="font-semibold">Assessment Results</h4>
                <p className="text-sm text-muted-foreground">Results from security control assessments</p>
              </div>
              <div>
                <h4 className="font-semibold">Plan of Action and Milestones (POA&amp;M)</h4>
                <p className="text-sm text-muted-foreground">Remediation tracking for identified security issues</p>
              </div>
            </CardContent>
          </Card>

          {/* Keyboard Shortcuts */}
          <Card>
            <CardHeader>
              <CardTitle>Keyboard Shortcuts</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <span className="font-mono text-sm bg-muted px-2 py-1 rounded">Tab</span>
                  <p className="text-sm text-muted-foreground mt-1">Navigate between interactive elements</p>
                </div>
                <div>
                  <span className="font-mono text-sm bg-muted px-2 py-1 rounded">Shift + Tab</span>
                  <p className="text-sm text-muted-foreground mt-1">Navigate backward</p>
                </div>
                <div>
                  <span className="font-mono text-sm bg-muted px-2 py-1 rounded">Enter</span>
                  <p className="text-sm text-muted-foreground mt-1">Activate buttons and links</p>
                </div>
                <div>
                  <span className="font-mono text-sm bg-muted px-2 py-1 rounded">Escape</span>
                  <p className="text-sm text-muted-foreground mt-1">Close dialogs and dropdowns</p>
                </div>
                <div>
                  <span className="font-mono text-sm bg-muted px-2 py-1 rounded">Space</span>
                  <p className="text-sm text-muted-foreground mt-1">Toggle checkboxes and select items</p>
                </div>
                <div>
                  <span className="font-mono text-sm bg-muted px-2 py-1 rounded">Arrow Keys</span>
                  <p className="text-sm text-muted-foreground mt-1">Navigate within dropdowns</p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Troubleshooting */}
          <Card>
            <CardHeader>
              <CardTitle>Troubleshooting</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <h4 className="font-semibold mb-2">Backend Not Responding</h4>
                <p className="text-sm text-muted-foreground mb-2">
                  If you see connection errors, ensure the backend server is running:
                </p>
                <code className="block bg-muted p-3 rounded text-sm font-mono">
                  cd front-end<br />
                  ./dev.sh
                </code>
                <p className="text-sm text-muted-foreground mt-2">
                  This will start both the Java backend (port 8080) and the Next.js frontend (port 3000).
                </p>
              </div>

              <div>
                <h4 className="font-semibold mb-2">Validation Errors</h4>
                <p className="text-sm text-muted-foreground">
                  Read error messages carefully - they indicate what&apos;s wrong with your OSCAL document.
                  Common issues include missing required fields, incorrect UUIDs, malformed structure,
                  or wrong model type selected. Click on errors to jump to the problematic line in the preview.
                </p>
              </div>

              <div>
                <h4 className="font-semibold mb-2">Conversion Fails</h4>
                <p className="text-sm text-muted-foreground">
                  Ensure your source document is valid OSCAL before attempting conversion.
                  Use the Validate feature first to check for errors. Malformed documents may fail to convert.
                </p>
              </div>

              <div>
                <h4 className="font-semibold mb-2">Profile Resolution Fails</h4>
                <p className="text-sm text-muted-foreground">
                  Profile resolution requires access to referenced catalogs. Ensure the catalog URIs in your
                  profile are valid and accessible. The backend must be able to fetch these resources.
                </p>
              </div>

              <div>
                <h4 className="font-semibold mb-2">Large Files</h4>
                <p className="text-sm text-muted-foreground">
                  The current upload limit is 10MB per file. For larger files, consider splitting them.
                  Batch operations can handle multiple smaller files more efficiently than one large file.
                </p>
              </div>

              <div>
                <h4 className="font-semibold mb-2">Browser Compatibility</h4>
                <p className="text-sm text-muted-foreground">
                  OSCAL UX works best on modern browsers (Chrome, Firefox, Safari, Edge).
                  If you experience issues, try clearing your browser cache or using a different browser.
                </p>
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
                API Documentation (Swagger UI)
                <ExternalLink className="h-4 w-4 ml-2" aria-hidden="true" />
              </a>
              <p className="text-xs text-muted-foreground ml-6">
                Interactive API documentation for the OSCAL CLI backend
              </p>

              <a
                href="https://pages.nist.gov/OSCAL/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                NIST OSCAL Website
                <ExternalLink className="h-4 w-4 ml-2" aria-hidden="true" />
              </a>
              <p className="text-xs text-muted-foreground ml-6">
                Official OSCAL documentation and specifications from NIST
              </p>

              <a
                href="https://oscalfoundation.org/"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                OSCAL Foundation
                <ExternalLink className="h-4 w-4 ml-2" aria-hidden="true" />
              </a>
              <p className="text-xs text-muted-foreground ml-6">
                Community resources, tools, and ecosystem
              </p>

              <a
                href="https://github.com/usnistgov/OSCAL"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                OSCAL GitHub Repository
                <ExternalLink className="h-4 w-4 ml-2" aria-hidden="true" />
              </a>
              <p className="text-xs text-muted-foreground ml-6">
                Source code, schemas, tools, and issue tracking
              </p>

              <a
                href="https://github.com/usnistgov/oscal-content"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center text-primary hover:underline"
              >
                OSCAL Sample Content
                <ExternalLink className="h-4 w-4 ml-2" aria-hidden="true" />
              </a>
              <p className="text-xs text-muted-foreground ml-6">
                Example OSCAL documents and catalogs for testing
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
