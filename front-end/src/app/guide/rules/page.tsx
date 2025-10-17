'use client';

import Link from 'next/link';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  ArrowLeft,
  ShieldCheck,
  AlertCircle,
  AlertTriangle,
  Info,
  CheckCircle,
  XCircle,
  Settings,
  FileText,
  Filter,
  Layers
} from 'lucide-react';

export default function RulesGuidePage() {
  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto py-12 px-4 max-w-5xl">
        {/* Header */}
        <div className="mb-8">
          <Link
            href="/"
            className="inline-flex items-center text-sm text-muted-foreground hover:text-primary mb-4"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Home
          </Link>
          <div className="flex items-start justify-between">
            <div>
              <div className="flex items-center gap-3 mb-2">
                <ShieldCheck className="h-10 w-10 text-primary" />
                <h1 className="text-4xl font-bold">Validation Rules User Guide</h1>
              </div>
              <p className="text-lg text-muted-foreground">
                Understanding and managing OSCAL validation rules
              </p>
            </div>
          </div>
        </div>

        {/* Quick Links */}
        <Card className="mb-8 bg-primary/5 border-primary/20">
          <CardHeader>
            <CardTitle className="text-lg">Quick Navigation</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
              <a href="#overview" className="text-primary hover:underline">• What are Validation Rules?</a>
              <a href="#built-in" className="text-primary hover:underline">• Built-in Rules</a>
              <a href="#custom" className="text-primary hover:underline">• Custom Rules</a>
              <a href="#severity" className="text-primary hover:underline">• Severity Levels</a>
              <a href="#categories" className="text-primary hover:underline">• Rule Categories</a>
              <a href="#filtering" className="text-primary hover:underline">• Filtering Rules</a>
            </div>
          </CardContent>
        </Card>

        {/* Overview Section */}
        <section id="overview" className="mb-12">
          <h2 className="text-3xl font-bold mb-4 flex items-center gap-2">
            <FileText className="h-8 w-8 text-primary" />
            What are Validation Rules?
          </h2>
          <Card>
            <CardContent className="pt-6 space-y-4 text-muted-foreground">
              <p>
                Validation rules are checks that ensure your OSCAL documents meet quality standards,
                comply with specifications, and contain all required information. When you validate
                an OSCAL document, the system runs these rules to identify potential issues.
              </p>
              <p>
                The OSCAL UX platform includes two types of validation rules:
              </p>
              <ul className="list-disc pl-6 space-y-2">
                <li>
                  <strong className="text-foreground">Built-in Rules:</strong> Pre-configured rules based on OSCAL specifications
                  and best practices
                </li>
                <li>
                  <strong className="text-foreground">Custom Rules:</strong> Organization-specific rules you can create to enforce
                  your own requirements
                </li>
              </ul>
            </CardContent>
          </Card>
        </section>

        {/* Built-in Rules Section */}
        <section id="built-in" className="mb-12">
          <h2 className="text-3xl font-bold mb-4 flex items-center gap-2">
            <Layers className="h-8 w-8 text-primary" />
            Built-in Validation Rules
          </h2>
          <Card className="mb-4">
            <CardContent className="pt-6 space-y-4 text-muted-foreground">
              <p>
                Built-in rules are automatically included with the platform and cover common OSCAL
                validation scenarios. These rules check for:
              </p>
              <ul className="list-disc pl-6 space-y-2">
                <li>Required metadata fields (title, version, last-modified)</li>
                <li>UUID format and uniqueness</li>
                <li>Valid references to other OSCAL objects</li>
                <li>Proper structure and relationships between elements</li>
                <li>Date/time format compliance</li>
                <li>Security control identifiers and references</li>
              </ul>
              <div className="pt-4">
                <Link href="/rules">
                  <Button>
                    <ShieldCheck className="mr-2 h-4 w-4" />
                    View All Built-in Rules
                  </Button>
                </Link>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-blue-50 dark:bg-blue-950/20 border-blue-200 dark:border-blue-900">
            <CardContent className="pt-6 flex items-start gap-3">
              <Info className="h-5 w-5 text-blue-500 flex-shrink-0 mt-0.5" />
              <div className="text-sm text-muted-foreground">
                <strong className="text-foreground">Tip:</strong> Built-in rules are regularly updated
                to reflect the latest OSCAL specifications. They cannot be modified but can provide
                a foundation for creating your own custom rules.
              </div>
            </CardContent>
          </Card>
        </section>

        {/* Custom Rules Section */}
        <section id="custom" className="mb-12">
          <h2 className="text-3xl font-bold mb-4 flex items-center gap-2">
            <Settings className="h-8 w-8 text-primary" />
            Custom Validation Rules
          </h2>
          <Card className="mb-4">
            <CardContent className="pt-6 space-y-4 text-muted-foreground">
              <p>
                Custom rules allow you to enforce organization-specific requirements that go beyond
                the standard OSCAL specifications. You can create custom rules to:
              </p>
              <ul className="list-disc pl-6 space-y-2">
                <li>Enforce naming conventions for controls or components</li>
                <li>Require specific metadata fields for your organization</li>
                <li>Validate custom extensions or properties</li>
                <li>Check for required relationships between elements</li>
                <li>Ensure compliance with internal policies</li>
              </ul>
            </CardContent>
          </Card>

          <h3 className="text-2xl font-semibold mb-3">Creating a Custom Rule</h3>
          <Card className="mb-4">
            <CardContent className="pt-6 space-y-4 text-muted-foreground">
              <p>To create a custom validation rule:</p>
              <ol className="list-decimal pl-6 space-y-3">
                <li>
                  Navigate to the <Link href="/rules/custom" className="text-primary hover:underline">
                    Custom Rules Management
                  </Link> page
                </li>
                <li>Click the "New Rule" button</li>
                <li>Fill in the required information:
                  <ul className="list-disc pl-6 mt-2 space-y-1">
                    <li><strong className="text-foreground">Rule ID:</strong> Unique identifier (e.g., "custom-rule-001")</li>
                    <li><strong className="text-foreground">Name:</strong> Descriptive name for the rule</li>
                    <li><strong className="text-foreground">Description:</strong> What the rule validates</li>
                    <li><strong className="text-foreground">Rule Type:</strong> Type of validation (required-field, pattern-match, etc.)</li>
                    <li><strong className="text-foreground">Severity:</strong> Error, Warning, or Info</li>
                    <li><strong className="text-foreground">Category:</strong> Organizational grouping</li>
                    <li><strong className="text-foreground">Applicable Model Types:</strong> Which OSCAL models this rule applies to</li>
                  </ul>
                </li>
                <li>Click "Create Rule" to save</li>
              </ol>
              <div className="pt-4">
                <Link href="/rules/custom">
                  <Button>
                    <Settings className="mr-2 h-4 w-4" />
                    Manage Custom Rules
                  </Button>
                </Link>
              </div>
            </CardContent>
          </Card>

          <h3 className="text-2xl font-semibold mb-3">Rule Types</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Required Field</CardTitle>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">
                Ensures specific fields are present in the document
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Pattern Match</CardTitle>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">
                Validates field values against regex patterns
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Allowed Values</CardTitle>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">
                Restricts fields to specific allowed values
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Cardinality</CardTitle>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">
                Checks minimum/maximum occurrences of elements
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Cross-Field</CardTitle>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">
                Validates relationships between multiple fields
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="text-base">ID Reference</CardTitle>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">
                Ensures references point to valid IDs
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Data Type</CardTitle>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">
                Validates field values are correct data type
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle className="text-base">Custom</CardTitle>
              </CardHeader>
              <CardContent className="text-sm text-muted-foreground">
                Organization-specific validation logic
              </CardContent>
            </Card>
          </div>
        </section>

        {/* Severity Levels Section */}
        <section id="severity" className="mb-12">
          <h2 className="text-3xl font-bold mb-4">Understanding Severity Levels</h2>
          <div className="space-y-4">
            <Card className="border-red-200 dark:border-red-900">
              <CardHeader className="bg-red-50 dark:bg-red-950/20">
                <CardTitle className="flex items-center gap-2 text-red-700 dark:text-red-300">
                  <XCircle className="h-5 w-5" />
                  Error
                </CardTitle>
              </CardHeader>
              <CardContent className="pt-4 text-muted-foreground">
                <p className="mb-2">Critical issues that must be fixed. Documents with errors:</p>
                <ul className="list-disc pl-6 space-y-1">
                  <li>Violate OSCAL schema requirements</li>
                  <li>Have missing required fields</li>
                  <li>Contain invalid data that prevents processing</li>
                  <li>Should not be used in production until resolved</li>
                </ul>
              </CardContent>
            </Card>

            <Card className="border-yellow-200 dark:border-yellow-900">
              <CardHeader className="bg-yellow-50 dark:bg-yellow-950/20">
                <CardTitle className="flex items-center gap-2 text-yellow-700 dark:text-yellow-300">
                  <AlertTriangle className="h-5 w-5" />
                  Warning
                </CardTitle>
              </CardHeader>
              <CardContent className="pt-4 text-muted-foreground">
                <p className="mb-2">Issues that should be addressed but don't prevent processing:</p>
                <ul className="list-disc pl-6 space-y-1">
                  <li>Recommended best practices not followed</li>
                  <li>Potential quality issues</li>
                  <li>Fields present but may need attention</li>
                  <li>Could cause problems in certain contexts</li>
                </ul>
              </CardContent>
            </Card>

            <Card className="border-blue-200 dark:border-blue-900">
              <CardHeader className="bg-blue-50 dark:bg-blue-950/20">
                <CardTitle className="flex items-center gap-2 text-blue-700 dark:text-blue-300">
                  <Info className="h-5 w-5" />
                  Info
                </CardTitle>
              </CardHeader>
              <CardContent className="pt-4 text-muted-foreground">
                <p className="mb-2">Informational notices for awareness:</p>
                <ul className="list-disc pl-6 space-y-1">
                  <li>Suggestions for improvement</li>
                  <li>Optional enhancements</li>
                  <li>Documentation reminders</li>
                  <li>Nice-to-have additions</li>
                </ul>
              </CardContent>
            </Card>
          </div>
        </section>

        {/* Categories Section */}
        <section id="categories" className="mb-12">
          <h2 className="text-3xl font-bold mb-4">Rule Categories</h2>
          <Card>
            <CardContent className="pt-6">
              <p className="text-muted-foreground mb-4">
                Rules are organized into categories to help you find and manage them:
              </p>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Metadata</h4>
                  <p className="text-muted-foreground">Document-level information like title, version, dates</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Security Controls</h4>
                  <p className="text-muted-foreground">Control definitions, parameters, and implementations</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Identifiers</h4>
                  <p className="text-muted-foreground">UUIDs, IDs, and unique references</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">References</h4>
                  <p className="text-muted-foreground">Links to external resources and documents</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Structural</h4>
                  <p className="text-muted-foreground">Document organization and relationships</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Profile</h4>
                  <p className="text-muted-foreground">Profile-specific validation</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Component</h4>
                  <p className="text-muted-foreground">Component definition validation</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">SSP</h4>
                  <p className="text-muted-foreground">System Security Plan validation</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Assessment</h4>
                  <p className="text-muted-foreground">Assessment-related validation</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </section>

        {/* Filtering Section */}
        <section id="filtering" className="mb-12">
          <h2 className="text-3xl font-bold mb-4 flex items-center gap-2">
            <Filter className="h-8 w-8 text-primary" />
            Filtering and Searching Rules
          </h2>
          <Card>
            <CardContent className="pt-6 space-y-4 text-muted-foreground">
              <p>
                The Validation Rules page provides powerful filtering to help you find specific rules:
              </p>
              <ul className="list-disc pl-6 space-y-2">
                <li>
                  <strong className="text-foreground">Text Search:</strong> Search by rule name, description, or ID
                </li>
                <li>
                  <strong className="text-foreground">OSCAL Model Type:</strong> Filter by which OSCAL models the rule applies to
                  (Catalog, Profile, SSP, etc.)
                </li>
                <li>
                  <strong className="text-foreground">Category:</strong> Filter by rule category to focus on specific areas
                </li>
                <li>
                  <strong className="text-foreground">Statistics:</strong> View real-time counts of filtered rules, including
                  built-in vs custom rules
                </li>
              </ul>
              <div className="bg-primary/5 border border-primary/20 rounded-lg p-4 mt-4">
                <p className="text-sm">
                  <strong className="text-foreground">Pro Tip:</strong> Combine multiple filters to narrow down
                  exactly the rules you need. The statistics tiles update in real-time to show your filtered results.
                </p>
              </div>
            </CardContent>
          </Card>
        </section>

        {/* Best Practices Section */}
        <section id="best-practices" className="mb-12">
          <h2 className="text-3xl font-bold mb-4 flex items-center gap-2">
            <CheckCircle className="h-8 w-8 text-primary" />
            Best Practices
          </h2>
          <Card>
            <CardContent className="pt-6">
              <div className="space-y-4 text-muted-foreground">
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Start with Built-in Rules</h4>
                  <p>Review all built-in rules to understand what's already being validated before creating custom rules.</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Use Descriptive Names</h4>
                  <p>Give your custom rules clear, descriptive names that explain what they validate.</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Set Appropriate Severity</h4>
                  <p>Use Error only for critical issues, Warning for best practices, and Info for suggestions.</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Organize with Categories</h4>
                  <p>Assign rules to appropriate categories to keep them organized and easy to find.</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Document Your Rules</h4>
                  <p>Include detailed descriptions explaining what the rule checks and why it's important.</p>
                </div>
                <div>
                  <h4 className="font-semibold text-foreground mb-2">Test Before Enabling</h4>
                  <p>Create rules in a disabled state first, test them, then enable them for production use.</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </section>

        {/* Action Buttons */}
        <div className="flex gap-4 justify-center">
          <Link href="/rules">
            <Button size="lg">
              <ShieldCheck className="mr-2 h-5 w-5" />
              View Validation Rules
            </Button>
          </Link>
          <Link href="/rules/custom">
            <Button size="lg" variant="outline">
              <Settings className="mr-2 h-5 w-5" />
              Manage Custom Rules
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
