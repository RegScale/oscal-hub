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

      {/* Free & Open Source Notice */}
      <Card className="mb-6 border-green-500/50 bg-green-500/5">
        <CardHeader>
          <CardTitle>Free & Open Source License</CardTitle>
          <CardDescription>
            Copyright © 2025 RegScale, Inc. Licensed for unlimited production use.
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
            associated documentation files (the &quot;Software&quot;), to use the Software for <strong>unlimited production use</strong>,
            including commercial production environments, subject to the restrictions below.
          </p>
        </CardContent>
      </Card>

      {/* Quick Summary */}
      <div className="grid md:grid-cols-2 gap-4 mb-6">
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
                <strong>Use in production environments</strong>
              </li>
              <li className="flex items-start">
                <span className="mr-2">•</span>
                <strong>Use in commercial production</strong>
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
                <strong>Resell the software commercially</strong>
              </li>
              <li className="flex items-start">
                <span className="mr-2">•</span>
                <strong>Create derivative works</strong>
              </li>
              <li className="flex items-start">
                <span className="mr-2">•</span>
                Sell or monetize the software itself
              </li>
              <li className="flex items-start">
                <span className="mr-2">•</span>
                Create modified versions without permission
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
      </div>

      {/* Detailed Terms */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Permitted Uses (Including Production)</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <div>
            <h4 className="font-semibold text-sm mb-1">You may:</h4>
            <ul className="text-sm text-muted-foreground space-y-1 ml-4">
              <li>• <strong>Use</strong> the Software for personal, educational, research, or non-profit purposes</li>
              <li>• <strong>Use</strong> the Software in production environments, including commercial production</li>
              <li>• <strong>View and study</strong> the source code for learning purposes</li>
              <li>• <strong>Run</strong> the Software in any environment</li>
              <li>• <strong>Deploy</strong> the Software for internal use within your organization</li>
              <li>• <strong>Deploy</strong> the Software for production workloads without limitation</li>
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
                • <strong>Resell commercially</strong>: Sell, resell, or monetize the Software itself, including but not limited to:
                <ul className="ml-6 mt-1 space-y-1">
                  <li>- Selling the Software as a standalone product</li>
                  <li>- Offering the Software for sale or resale</li>
                  <li>- Licensing the Software to others for a fee</li>
                </ul>
                <p className="mt-2 text-xs italic">Note: Using the Software in production to support your business is permitted.</p>
              </li>
              <li>• <strong>Create derivatives</strong>: Create, modify, adapt, or prepare derivative works based on the Software without explicit permission</li>
              <li>• <strong>Redistribute modified versions</strong>: Distribute modified or derivative versions of the Software</li>
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
          <CardTitle>Creating Derivative Works</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <p className="text-sm text-muted-foreground">
            If you wish to create derivative works or modified versions of the Software, please contact RegScale for permission:
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
