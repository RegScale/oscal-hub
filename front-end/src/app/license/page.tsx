import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ExternalLink, Check, X, Briefcase } from 'lucide-react';
import Link from 'next/link';

export default function LicensePage() {
  return (
    <div className="container mx-auto py-8 px-4 max-w-4xl">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-2">OSCAL UX License</h1>
        <p className="text-muted-foreground">Version 1.0 - October 2025</p>
      </div>

      {/* Non-Commercial Notice */}
      <Card className="mb-6 border-primary/50 bg-primary/5">
        <CardHeader>
          <CardTitle>Non-Commercial Source-Available License</CardTitle>
          <CardDescription>
            Copyright © 2025 RegScale, Inc. All rights reserved.
          </CardDescription>
        </CardHeader>
      </Card>

      {/* Grant of Rights */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Grant of Rights</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
            associated documentation files (the &quot;Software&quot;), to use the Software for <strong>non-commercial purposes only</strong>,
            subject to the following conditions:
          </p>
        </CardContent>
      </Card>

      {/* Quick Summary */}
      <div className="grid md:grid-cols-3 gap-4 mb-6">
        {/* Permitted Uses */}
        <Card className="border-green-500/30">
          <CardHeader>
            <CardTitle className="text-lg flex items-center">
              <Check className="h-5 w-5 mr-2 text-green-500" />
              You CAN
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="text-sm space-y-2 text-muted-foreground">
              <li className="flex items-start">
                <span className="mr-2">•</span>
                Use for personal projects
              </li>
              <li className="flex items-start">
                <span className="mr-2">•</span>
                Use for education and learning
              </li>
              <li className="flex items-start">
                <span className="mr-2">•</span>
                Use for research purposes
              </li>
              <li className="flex items-start">
                <span className="mr-2">•</span>
                Study the source code
              </li>
              <li className="flex items-start">
                <span className="mr-2">•</span>
                Use in non-profit organizations
              </li>
            </ul>
          </CardContent>
        </Card>

        {/* Restrictions */}
        <Card className="border-red-500/30">
          <CardHeader>
            <CardTitle className="text-lg flex items-center">
              <X className="h-5 w-5 mr-2 text-red-500" />
              You CANNOT
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="text-sm space-y-2 text-muted-foreground">
              <li className="flex items-start">
                <span className="mr-2">•</span>
                Use for commercial purposes
              </li>
              <li className="flex items-start">
                <span className="mr-2">•</span>
                Sell or monetize the software
              </li>
              <li className="flex items-start">
                <span className="mr-2">•</span>
                Create modified versions
              </li>
              <li className="flex items-start">
                <span className="mr-2">•</span>
                Redistribute without permission
              </li>
              <li className="flex items-start">
                <span className="mr-2">•</span>
                Remove RegScale attribution
              </li>
            </ul>
          </CardContent>
        </Card>

        {/* Commercial Licensing */}
        <Card className="border-blue-500/30">
          <CardHeader>
            <CardTitle className="text-lg flex items-center">
              <Briefcase className="h-5 w-5 mr-2 text-blue-500" />
              Commercial Use
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground mb-3">
              Contact RegScale for a commercial license
            </p>
            <a
              href="https://www.regscale.com"
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm text-primary hover:underline inline-flex items-center"
            >
              www.regscale.com
              <ExternalLink className="h-3 w-3 ml-1" />
            </a>
          </CardContent>
        </Card>
      </div>

      {/* Detailed Terms */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Permitted Uses (Non-Commercial Only)</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div>
            <h4 className="font-semibold text-sm mb-1">You may:</h4>
            <ul className="text-sm text-muted-foreground space-y-1 ml-4">
              <li>• <strong>Use</strong> the Software for personal, educational, research, or non-profit purposes</li>
              <li>• <strong>View and study</strong> the source code for learning purposes</li>
              <li>• <strong>Run</strong> the Software in non-commercial environments</li>
              <li>• <strong>Deploy</strong> the Software for internal non-commercial use within your organization</li>
            </ul>
          </div>
        </CardContent>
      </Card>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Restrictions</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div>
            <h4 className="font-semibold text-sm mb-1">You may NOT:</h4>
            <ul className="text-sm text-muted-foreground space-y-2 ml-4">
              <li>
                • <strong>Use commercially</strong>: Use the Software or any portion thereof for commercial purposes, including but not limited to:
                <ul className="ml-6 mt-1 space-y-1">
                  <li>- Providing services to third parties for compensation</li>
                  <li>- Incorporating into commercial products or services</li>
                  <li>- Using in business operations that generate revenue</li>
                  <li>- Offering as a hosted or SaaS service for commercial gain</li>
                </ul>
              </li>
              <li>• <strong>Create derivatives</strong>: Create, modify, adapt, or prepare derivative works based on the Software</li>
              <li>• <strong>Redistribute</strong>: Distribute, sublicense, sell, lease, or transfer copies of the Software</li>
              <li>• <strong>Remove attribution</strong>: Remove or alter any copyright notices, license information, or attribution to RegScale</li>
            </ul>
          </div>
        </CardContent>
      </Card>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Attribution Requirements</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground mb-2">
            All copies or substantial portions of the Software must include:
          </p>
          <ol className="text-sm text-muted-foreground space-y-1 ml-4">
            <li>1. This license notice</li>
            <li>2. Copyright notice: &quot;OSCAL UX © 2025 RegScale, Inc.&quot;</li>
            <li>3. Link to the original source repository</li>
            <li>4. Acknowledgment that this is a contribution from RegScale</li>
          </ol>
        </CardContent>
      </Card>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Commercial Licensing</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <p className="text-sm text-muted-foreground">
            For commercial use, enterprise deployments, or creating derivative works, please contact RegScale at:
          </p>
          <ul className="text-sm space-y-1">
            <li>
              Website:{' '}
              <a
                href="https://www.regscale.com"
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary hover:underline inline-flex items-center"
              >
                https://www.regscale.com
                <ExternalLink className="h-3 w-3 ml-1" />
              </a>
            </li>
            <li>
              Email:{' '}
              <a href="mailto:info@regscale.com" className="text-primary hover:underline">
                info@regscale.com
              </a>
            </li>
          </ul>
          <div className="mt-3 pt-3 border-t">
            <p className="text-sm text-muted-foreground mb-2">
              Commercial licenses are available and may include:
            </p>
            <ul className="text-sm text-muted-foreground space-y-1 ml-4">
              <li>• Right to use in commercial applications</li>
              <li>• Right to create derivative works</li>
              <li>• Enterprise support and maintenance</li>
              <li>• Custom features and integrations</li>
            </ul>
          </div>
        </CardContent>
      </Card>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Contributions</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground mb-2">
            Contributions to this project are welcome under the following terms:
          </p>
          <ul className="text-sm text-muted-foreground space-y-1 ml-4">
            <li>• All contributions become the property of RegScale, Inc.</li>
            <li>• Contributors agree to license their contributions under this same license</li>
            <li>• Contributors certify they have the right to make their contribution</li>
          </ul>
        </CardContent>
      </Card>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Disclaimer of Warranty</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-xs text-muted-foreground">
            THE SOFTWARE IS PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
            INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
            PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
            FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
            ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
          </p>
        </CardContent>
      </Card>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Termination</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            This license automatically terminates if you violate any of its terms. Upon termination,
            you must cease all use of the Software and destroy all copies in your possession.
          </p>
        </CardContent>
      </Card>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Governing Law</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            This license shall be governed by and construed in accordance with the laws of the
            United States and the State of Delaware, without regard to its conflict of law provisions.
          </p>
        </CardContent>
      </Card>

      {/* Footer */}
      <div className="mt-8 pt-6 border-t text-center text-sm text-muted-foreground">
        <p className="mb-2">
          <strong>RegScale, Inc.</strong>
        </p>
        <p className="mb-1">
          Website:{' '}
          <a
            href="https://www.regscale.com"
            target="_blank"
            rel="noopener noreferrer"
            className="text-primary hover:underline inline-flex items-center"
          >
            https://www.regscale.com
            <ExternalLink className="h-3 w-3 ml-1" />
          </a>
        </p>
        <p className="mb-4">
          Email:{' '}
          <a href="mailto:info@regscale.com" className="text-primary hover:underline">
            info@regscale.com
          </a>
        </p>
        <p className="text-xs italic">
          Built with NIST OSCAL framework and standards.
        </p>
        <div className="mt-4">
          <Link href="/" className="text-primary hover:underline">
            ← Back to Home
          </Link>
        </div>
      </div>
    </div>
  );
}
