import Link from 'next/link';
import { ArrowLeft, ExternalLink } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';

export default function UserGuidePage() {
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-8 px-4 max-w-4xl">
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

        {/* Table of Contents */}
        <Card className="mb-8">
          <CardHeader>
            <CardTitle>Table of Contents</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
              <a href="#getting-started" className="text-primary hover:underline">Getting Started</a>
              <a href="#features" className="text-primary hover:underline">Features</a>
              <Link href="/guide/automation" className="text-primary hover:underline font-semibold">
                API Automation Guide →
              </Link>
              <a href="#validate" className="text-primary hover:underline">How to Validate Documents</a>
              <a href="#convert" className="text-primary hover:underline">How to Convert Document Formats</a>
              <a href="#resolve" className="text-primary hover:underline">How to Resolve Profiles</a>
              <a href="#batch" className="text-primary hover:underline">How to Process Multiple Files</a>
              <a href="#history" className="text-primary hover:underline">How to Use Operation History</a>
              <a href="#library" className="text-primary hover:underline">How to Use the OSCAL Library</a>
              <a href="#visualize" className="text-primary hover:underline">How to Visualize OSCAL Documents</a>
              <a href="#authorizations" className="text-primary hover:underline">How to Create System Authorizations</a>
              <a href="#service-tokens" className="text-primary hover:underline">How to Create and Use Service Account Tokens</a>
              <a href="#model-types" className="text-primary hover:underline">OSCAL Model Types</a>
              <a href="#keyboard-shortcuts" className="text-primary hover:underline">Keyboard Shortcuts</a>
              <a href="#troubleshooting" className="text-primary hover:underline">Troubleshooting</a>
              <a href="#resources" className="text-primary hover:underline">Additional Resources</a>
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
          <Card id="features">
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

              <div>
                <h3 className="text-lg font-semibold mb-2">✅ OSCAL Library</h3>
                <p className="text-muted-foreground mb-2">
                  Browse, share, and download example OSCAL documents from the community.
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Upload OSCAL documents to share with the community</li>
                  <li>Browse and download example documents</li>
                  <li>Version control for uploaded documents</li>
                  <li>Search by type, tag, or keyword</li>
                  <li>View analytics and popular downloads</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-2">✅ Data Visualization</h3>
                <p className="text-muted-foreground mb-2">
                  Explore and understand OSCAL documents through interactive visualizations.
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Visual representation of catalog controls</li>
                  <li>Control family distribution charts</li>
                  <li>Interactive data exploration</li>
                  <li>Export visualization data</li>
                  <li>Support for all OSCAL model types</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-2">✅ Service Account Tokens</h3>
                <p className="text-muted-foreground mb-2">
                  Generate JWT tokens for programmatic API access from external applications.
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Create named service account tokens</li>
                  <li>Configurable expiration periods (1-3650 days)</li>
                  <li>Secure JWT-based authentication</li>
                  <li>Use tokens for CI/CD pipelines and automation</li>
                  <li>Full API access with service account tokens</li>
                </ul>
              </div>

              <div>
                <h3 className="text-lg font-semibold mb-2">✅ System Authorizations</h3>
                <p className="text-muted-foreground mb-2">
                  Create professional system authorization documents using customizable templates.
                </p>
                <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
                  <li>Create reusable markdown templates with variables</li>
                  <li>Multi-step wizard for generating authorization documents</li>
                  <li>Link authorizations to System Security Plans (SSPs)</li>
                  <li>Live preview with variable substitution</li>
                  <li>Track authorization history and metadata</li>
                  <li>Search and filter authorizations by system</li>
                </ul>
              </div>
            </CardContent>
          </Card>

          {/* How to Use Validation */}
          <Card id="validate">
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
          <Card id="convert">
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
          <Card id="resolve">
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
          <Card id="batch">
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
          <Card id="history">
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

          {/* How to Use Library */}
          <Card id="library">
            <CardHeader>
              <CardTitle>How to Use the OSCAL Library</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <h3 className="font-semibold text-lg mb-2">Browsing the Library</h3>
              <ol className="list-decimal list-inside space-y-4 text-muted-foreground mb-6">
                <li>
                  <span className="font-medium text-foreground">Navigate to Library</span>
                  <p className="ml-6 mt-1">Click the &quot;Library&quot; card on the dashboard.</p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Browse Items</span>
                  <p className="ml-6 mt-1">
                    View all available OSCAL documents in the library. Each card shows the title,
                    type, tags, and download count.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Download Items</span>
                  <p className="ml-6 mt-1">
                    Click the &quot;Download&quot; button on any item to save it to your computer,
                    or click the card to view detailed information.
                  </p>
                </li>
              </ol>

              <h3 className="font-semibold text-lg mb-2">Uploading to the Library</h3>
              <ol className="list-decimal list-inside space-y-4 text-muted-foreground mb-6">
                <li>
                  <span className="font-medium text-foreground">Go to Upload Tab</span>
                  <p className="ml-6 mt-1">Click the &quot;Upload&quot; tab in the Library page.</p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Fill in Metadata</span>
                  <p className="ml-6 mt-1">
                    Provide a title, description, OSCAL type, and optional tags for your document.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Select File</span>
                  <p className="ml-6 mt-1">
                    Choose your OSCAL file (JSON, XML, or YAML format). The format will be auto-detected.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Upload</span>
                  <p className="ml-6 mt-1">
                    Click &quot;Upload to Library&quot;. Your document will be added to the community library.
                  </p>
                </li>
              </ol>

              <h3 className="font-semibold text-lg mb-2">Searching the Library</h3>
              <p className="text-muted-foreground">
                Use the Search tab to filter library items by keyword, OSCAL type, or tag.
                The Analytics tab provides insights into library usage and popular downloads.
              </p>
            </CardContent>
          </Card>

          {/* How to Use Visualization */}
          <Card id="visualize">
            <CardHeader>
              <CardTitle>How to Visualize OSCAL Documents</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <ol className="list-decimal list-inside space-y-4 text-muted-foreground">
                <li>
                  <span className="font-medium text-foreground">Navigate to Visualize</span>
                  <p className="ml-6 mt-1">Click the &quot;Visualize&quot; card on the dashboard.</p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Upload Your Document</span>
                  <p className="ml-6 mt-1">
                    Upload an OSCAL document (JSON, XML, or YAML). The format will be auto-detected.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Select Model Type</span>
                  <p className="ml-6 mt-1">
                    Choose the appropriate OSCAL model type. The visualization will be customized
                    based on the document structure.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Generate Visualization</span>
                  <p className="ml-6 mt-1">
                    Click &quot;Visualize Document&quot;. Interactive charts and graphs will be
                    generated showing document structure, control families, and relationships.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Explore the Data</span>
                  <p className="ml-6 mt-1">
                    Interact with the visualizations to explore your OSCAL data. For catalogs,
                    view control family distribution. For SSPs, see implementation status.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Export Data</span>
                  <p className="ml-6 mt-1">
                    Download the visualization data or export charts for presentations and reports.
                  </p>
                </li>
              </ol>
            </CardContent>
          </Card>

          {/* How to Use Authorizations */}
          <Card id="authorizations">
            <CardHeader>
              <CardTitle>How to Create System Authorizations</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-muted-foreground">
                The Authorization feature allows you to create professional system authorization documents
                using customizable templates with variable substitution. Perfect for ATO (Authority to Operate)
                decisions, FedRAMP authorizations, and internal system approvals.
              </p>

              <h3 className="font-semibold text-lg mb-2 mt-6">Creating Authorization Templates</h3>
              <ol className="list-decimal list-inside space-y-4 text-muted-foreground mb-6">
                <li>
                  <span className="font-medium text-foreground">Navigate to Authorizations</span>
                  <p className="ml-6 mt-1">
                    Click the &quot;Authorizations&quot; card on the dashboard with the ShieldCheck icon.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Go to Templates Tab</span>
                  <p className="ml-6 mt-1">
                    Click the &quot;Templates&quot; tab to view and manage authorization templates.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Create New Template</span>
                  <p className="ml-6 mt-1">
                    Click &quot;Create New Template&quot; to start creating a reusable template.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Enter Template Details</span>
                  <p className="ml-6 mt-1">
                    Provide a template name (e.g., &quot;FedRAMP ATO Template&quot; or &quot;Internal Authorization&quot;).
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Write Template Content with Variables</span>
                  <p className="ml-6 mt-1">
                    Use markdown formatting and insert variables using double curly braces.
                    Variables can contain letters, numbers, hyphens, underscores, spaces, and special characters.
                  </p>
                  <code className="block bg-muted p-3 rounded text-sm font-mono whitespace-pre overflow-x-auto mt-2">
{`# System Authorization for {{ system_name }}

**System Owner:** {{ system_owner }}
**Environment:** {{ environment }}

## Authorization Decision

This system is **{{ decision }}** for {{ environment }} operations.

**Authorizing Official:** {{ authorizing_official }}
**Date:** {{ authorization_date }}
**Period:** {{ authorization_period }}

## Risk Level
{{ risk_level }}

## Special Conditions
{{ conditions }}`}
                  </code>
                </li>
                <li>
                  <span className="font-medium text-foreground">Preview Template</span>
                  <p className="ml-6 mt-1">
                    View the live preview on the right side. Variables are automatically detected and
                    highlighted in amber. The list of variables appears below the editor.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Save Template</span>
                  <p className="ml-6 mt-1">
                    Click &quot;Save Template&quot; to save your template for reuse.
                  </p>
                </li>
              </ol>

              <h3 className="font-semibold text-lg mb-2 mt-6">Creating an Authorization Document</h3>
              <ol className="list-decimal list-inside space-y-4 text-muted-foreground mb-6">
                <li>
                  <span className="font-medium text-foreground">Go to Authorizations Tab</span>
                  <p className="ml-6 mt-1">
                    Click the &quot;Authorizations&quot; tab and then &quot;Create New Authorization&quot;.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Step 1: Select SSP</span>
                  <p className="ml-6 mt-1">
                    Choose a System Security Plan from your library. This links the authorization to a specific system.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Step 2: Choose Template</span>
                  <p className="ml-6 mt-1">
                    Select an authorization template. You&apos;ll see the template preview and list of variables
                    that need to be filled.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Step 3: Fill Variable Values</span>
                  <p className="ml-6 mt-1">
                    Enter values for each variable. As you type, the preview on the right updates in real-time
                    to show your completed document. All variables must be filled before proceeding.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Step 4: Review and Name</span>
                  <p className="ml-6 mt-1">
                    Review the final document, enter a name for this authorization, and click
                    &quot;Create Authorization&quot; to save it.
                  </p>
                </li>
              </ol>

              <h3 className="font-semibold text-lg mb-2 mt-6">Managing Authorizations</h3>
              <div className="space-y-3 text-muted-foreground ml-4">
                <div>
                  <span className="font-medium text-foreground">View Authorizations:</span>
                  <p className="mt-1">
                    Browse all authorization documents in the Authorizations tab. Each card shows the authorization
                    name, linked SSP, template used, and creation date.
                  </p>
                </div>
                <div>
                  <span className="font-medium text-foreground">Search:</span>
                  <p className="mt-1">
                    Use the search box to find authorizations by name or filter by system.
                  </p>
                </div>
                <div>
                  <span className="font-medium text-foreground">Delete:</span>
                  <p className="mt-1">
                    Click the delete button to remove an authorization. You&apos;ll be asked to confirm.
                  </p>
                </div>
              </div>

              <h3 className="font-semibold text-lg mb-2 mt-6">Variable Naming Best Practices</h3>
              <div className="bg-muted p-4 rounded space-y-2 text-sm text-muted-foreground">
                <p className="font-medium text-foreground">Supported Variable Formats:</p>
                <ul className="list-disc list-inside space-y-1 ml-4">
                  <li><code className="bg-background px-1.5 py-0.5 rounded">{'{{ system_name }}'}</code> - Letters, numbers, underscores, hyphens</li>
                  <li><code className="bg-background px-1.5 py-0.5 rounded">{'{{ Low, Moderate, or High }}'}</code> - Spaces and commas allowed</li>
                  <li><code className="bg-background px-1.5 py-0.5 rounded">{'{{ Federal Agency/Office }}'}</code> - Special characters allowed</li>
                  <li><code className="bg-background px-1.5 py-0.5 rounded">{'{{ agency logo }}'}</code> - Spaces for multi-word variables</li>
                </ul>
                <p className="mt-3 font-medium text-foreground">Tips:</p>
                <ul className="list-disc list-inside space-y-1 ml-4">
                  <li>Use descriptive variable names that clearly indicate what should be filled in</li>
                  <li>Keep variable names concise but meaningful</li>
                  <li>Use consistent naming conventions across templates</li>
                  <li>Variables are case-sensitive: <code className="bg-background px-1.5 py-0.5 rounded">{'{{ Date }}'}</code> ≠ <code className="bg-background px-1.5 py-0.5 rounded">{'{{ date }}'}</code></li>
                </ul>
              </div>

              <h3 className="font-semibold text-lg mb-2 mt-6">Example: FedRAMP Authorization Template</h3>
              <code className="block bg-muted p-4 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`{{ agency logo }}

{{ Insert Date }}

To: {{ CSP System Owner Name }}

The {{ Federal Agency/Office }} has completed review of the
{{ Insert CSP and cloud service name }} system and grants
Authority to Operate based on categorization of
"{{ Low, Moderate, or High }}".

This authorization is valid for {{ authorization period }}
and is subject to {{ special conditions }}.

SIGNED:
{{ Authorizing Official }}
{{ Title }}
{{ Date }}`}
              </code>

              <h3 className="font-semibold text-lg mb-2 mt-6">Common Use Cases</h3>
              <div className="space-y-3 text-muted-foreground ml-4">
                <div>
                  <span className="font-medium text-foreground">FedRAMP Authorizations:</span>
                  <p className="mt-1">
                    Create formal ATO documents for cloud service providers seeking FedRAMP authorization.
                  </p>
                </div>
                <div>
                  <span className="font-medium text-foreground">Internal System Approvals:</span>
                  <p className="mt-1">
                    Generate authorization documents for internal systems with standardized templates.
                  </p>
                </div>
                <div>
                  <span className="font-medium text-foreground">Conditional Authorizations:</span>
                  <p className="mt-1">
                    Document ATOs with specific conditions, requirements, or limitations.
                  </p>
                </div>
                <div>
                  <span className="font-medium text-foreground">Authorization Renewals:</span>
                  <p className="mt-1">
                    Track authorization periods and create renewal documents when needed.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* How to Use Service Account Tokens */}
          <Card id="service-tokens">
            <CardHeader>
              <CardTitle>How to Create and Use Service Account Tokens</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <h3 className="font-semibold text-lg mb-2">Creating a Service Account Token</h3>
              <ol className="list-decimal list-inside space-y-4 text-muted-foreground mb-6">
                <li>
                  <span className="font-medium text-foreground">Navigate to Profile</span>
                  <p className="ml-6 mt-1">
                    Click your username in the top-right corner and select &quot;Profile&quot;.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Scroll to Service Account Tokens</span>
                  <p className="ml-6 mt-1">
                    Find the &quot;Service Account Tokens&quot; section at the bottom of the profile page.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Enter Token Details</span>
                  <p className="ml-6 mt-1">
                    Provide a descriptive name (e.g., &quot;CI/CD Pipeline&quot;) and set the
                    expiration period in days (1-3650 days).
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Generate Token</span>
                  <p className="ml-6 mt-1">
                    Click &quot;Generate Service Account Token&quot;. The token will be displayed
                    once and cannot be retrieved later.
                  </p>
                </li>
                <li>
                  <span className="font-medium text-foreground">Copy and Store Securely</span>
                  <p className="ml-6 mt-1">
                    Copy the token immediately and store it in a secure location such as a password
                    manager or secrets vault. Never commit tokens to version control.
                  </p>
                </li>
              </ol>

              <h3 className="font-semibold text-lg mb-2">Using Service Account Tokens with the API</h3>
              <p className="text-muted-foreground mb-4">
                Service account tokens provide full API access and can be used for automation,
                CI/CD pipelines, and external applications.
              </p>

              <div className="space-y-4">
                <div>
                  <h4 className="font-semibold mb-2">Example: Validate a Document</h4>
                  <code className="block bg-muted p-3 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`curl -X POST http://localhost:8080/api/validate \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \\
  -d '{
    "content": "{ \\"catalog\\": { ... } }",
    "modelType": "catalog",
    "format": "JSON",
    "fileName": "my-catalog.json"
  }'`}
                  </code>
                </div>

                <div>
                  <h4 className="font-semibold mb-2">Example: Convert Format</h4>
                  <code className="block bg-muted p-3 rounded text-sm font-mono whitespace-pre overflow-x-auto">
{`curl -X POST http://localhost:8080/api/convert \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \\
  -d '{
    "content": "<catalog>...</catalog>",
    "fromFormat": "XML",
    "toFormat": "JSON",
    "modelType": "catalog"
  }'`}
                  </code>
                </div>

                <div>
                  <h4 className="font-semibold mb-2">Security Best Practices</h4>
                  <ul className="list-disc list-inside space-y-2 text-sm text-muted-foreground ml-4">
                    <li>Store tokens in environment variables or secure vaults</li>
                    <li>Use different tokens for different applications/environments</li>
                    <li>Set appropriate expiration periods based on usage</li>
                    <li>Rotate tokens regularly for better security</li>
                    <li>Never share tokens or commit them to source control</li>
                    <li>Revoke tokens immediately if compromised</li>
                  </ul>
                </div>

                <div>
                  <h4 className="font-semibold mb-2">API Documentation</h4>
                  <p className="text-sm text-muted-foreground mb-2">
                    For complete API documentation with all available endpoints, visit:
                  </p>
                  <a
                    href="http://localhost:8080/swagger-ui/index.html"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-primary hover:underline text-sm"
                  >
                    http://localhost:8080/swagger-ui/index.html
                  </a>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* OSCAL Model Types */}
          <Card id="model-types">
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
          <Card id="keyboard-shortcuts">
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
          <Card id="troubleshooting">
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
          <Card id="resources">
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
