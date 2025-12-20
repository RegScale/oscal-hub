import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ExternalLink, Check } from 'lucide-react';
import Link from 'next/link';

export default function LicensePage() {
  return (
    <main className="min-h-screen bg-background">
      <div className="container mx-auto py-12 px-4 max-w-4xl">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-4xl font-bold mb-2">MIT Open Source License</h1>
          <p className="text-muted-foreground">OSCAL Hub is released under the MIT License</p>
        </div>

        {/* Free & Open Source Notice */}
        <Card className="mb-6 border-green-500/50 bg-green-500/5">
          <CardHeader>
            <CardTitle>Free & Open Source Software</CardTitle>
            <CardDescription>
              Copyright © 2025 RegScale. Free to use, modify, and distribute.
            </CardDescription>
          </CardHeader>
        </Card>

        {/* MIT License Text */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>MIT License</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div className="prose dark:prose-invert max-w-none">
              <p className="text-sm text-muted-foreground mb-6">
                Copyright (c) 2025 RegScale
              </p>

              <div className="bg-muted p-6 rounded-lg space-y-4 text-sm">
                <p>
                  Permission is hereby granted, free of charge, to any person obtaining a copy
                  of this software and associated documentation files (the &quot;Software&quot;), to deal
                  in the Software without restriction, including without limitation the rights
                  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
                  copies of the Software, and to permit persons to whom the Software is
                  furnished to do so, subject to the following conditions:
                </p>

                <p>
                  The above copyright notice and this permission notice shall be included in all
                  copies or substantial portions of the Software.
                </p>

                <p className="font-semibold pt-4 border-t border-border">
                  THE SOFTWARE IS PROVIDED &quot;AS IS&quot;, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
                  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
                  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
                  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
                  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
                  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
                  SOFTWARE.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* What does this mean */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>What does this mean?</CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="space-y-3 text-sm text-muted-foreground">
              <li className="flex items-start">
                <Check className="h-5 w-5 mr-2 text-green-500 mt-0.5" />
                <span>You can use this software for any purpose, including commercial applications</span>
              </li>
              <li className="flex items-start">
                <Check className="h-5 w-5 mr-2 text-green-500 mt-0.5" />
                <span>You can modify the software to suit your needs</span>
              </li>
              <li className="flex items-start">
                <Check className="h-5 w-5 mr-2 text-green-500 mt-0.5" />
                <span>You can distribute copies of the software</span>
              </li>
              <li className="flex items-start">
                <Check className="h-5 w-5 mr-2 text-green-500 mt-0.5" />
                <span>You can sublicense and sell copies of the software</span>
              </li>
              <li className="flex items-start">
                <Check className="h-5 w-5 mr-2 text-green-500 mt-0.5" />
                <span>The only requirement is to include the copyright notice and license in any copies</span>
              </li>
            </ul>
          </CardContent>
        </Card>

        {/* Source Code */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>Source Code</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">
              The source code for OSCAL Hub is available on{' '}
              <a
                href="https://github.com/RegScale/oscal-hub"
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary hover:underline inline-flex items-center"
              >
                GitHub
                <ExternalLink className="h-3 w-3 ml-1" />
              </a>
              . Contributions are welcome!
            </p>
          </CardContent>
        </Card>

        {/* Third-Party Dependencies */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>Third-Party Dependencies</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground mb-4">
              This project uses various open source libraries and components. Each dependency has its own license:
            </p>
            <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground ml-4">
              <li>Next.js - MIT License</li>
              <li>React - MIT License</li>
              <li>Spring Boot - Apache License 2.0</li>
              <li>Metaschema Java Tools - NIST Public Domain</li>
              <li>OSCAL Java Library - NIST Public Domain</li>
            </ul>
            <p className="text-muted-foreground mt-4 text-sm">
              See the project&apos;s package.json and pom.xml files for a complete list of dependencies.
            </p>
          </CardContent>
        </Card>

        {/* Built with OSCAL */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>Built with OSCAL</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground mb-4">
              OSCAL Hub is built on top of the{' '}
              <a
                href="https://pages.nist.gov/OSCAL/"
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary hover:underline inline-flex items-center"
              >
                NIST OSCAL framework
                <ExternalLink className="h-3 w-3 ml-1" />
              </a>
              , an open standard for security compliance content.
            </p>
            <p className="text-sm text-muted-foreground">
              An open source contribution from{' '}
              <a
                href="https://www.regscale.com"
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary hover:underline inline-flex items-center"
              >
                RegScale
                <ExternalLink className="h-3 w-3 ml-1" />
              </a>
              .
            </p>
          </CardContent>
        </Card>

        {/* Footer */}
        <div className="mt-8 pt-6 border-t text-center">
          <Link href="/" className="text-primary hover:underline">
            ← Back to Home
          </Link>
        </div>
      </div>
    </main>
  );
}
