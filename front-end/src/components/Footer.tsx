import Link from 'next/link';
import { ExternalLink } from 'lucide-react';

export function Footer() {
  return (
    <footer className="border-t border-border bg-background mt-auto">
      <div className="container mx-auto py-8 px-4">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Resources */}
          <div>
            <h3 className="font-semibold mb-4">Resources</h3>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>
                <Link href="/guide" className="hover:text-foreground transition-colors inline-flex items-center">
                  User Guide
                </Link>
              </li>
              <li>
                <a
                  href="https://pages.nist.gov/OSCAL/"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-foreground transition-colors inline-flex items-center"
                >
                  NIST OSCAL Website
                  <ExternalLink className="h-3 w-3 ml-1" />
                </a>
              </li>
              <li>
                <a
                  href="https://oscalfoundation.org/"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-foreground transition-colors inline-flex items-center"
                >
                  OSCAL Foundation
                  <ExternalLink className="h-3 w-3 ml-1" />
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/usnistgov/OSCAL"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-foreground transition-colors inline-flex items-center"
                >
                  OSCAL on GitHub
                  <ExternalLink className="h-3 w-3 ml-1" />
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/usnistgov/oscal-content"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-foreground transition-colors inline-flex items-center"
                >
                  OSCAL Content Repository
                  <ExternalLink className="h-3 w-3 ml-1" />
                </a>
              </li>
            </ul>
          </div>

          {/* Tools */}
          <div>
            <h3 className="font-semibold mb-4">Tools</h3>
            <ul className="space-y-2 text-sm text-muted-foreground">
              <li>
                <Link href="/library" className="hover:text-foreground transition-colors">
                  Library
                </Link>
              </li>
              <li>
                <Link href="/visualize" className="hover:text-foreground transition-colors">
                  Visualize
                </Link>
              </li>
              <li>
                <Link href="/validate" className="hover:text-foreground transition-colors">
                  Validate Documents
                </Link>
              </li>
              <li>
                <Link href="/convert" className="hover:text-foreground transition-colors">
                  Convert Formats
                </Link>
              </li>
            </ul>
          </div>

          {/* About */}
          <div>
            <h3 className="font-semibold mb-4">About</h3>
            <p className="text-sm text-muted-foreground mb-4">
              OSCAL Hub provides a modern, user-friendly interface for working with OSCAL documents,
              making security compliance documentation more accessible.
            </p>
            <div className="text-xs text-muted-foreground space-y-1">
              <p>
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
              </p>
            </div>
          </div>
        </div>

        {/* Bottom Bar */}
        <div className="mt-8 pt-8 border-t border-border text-center text-xs text-muted-foreground">
          <p>
            © {new Date().getFullYear()} OSCAL Hub. Open source contribution from{' '}
            <a
              href="https://www.regscale.com"
              target="_blank"
              rel="noopener noreferrer"
              className="text-primary hover:underline"
            >
              RegScale
            </a>
            . Built with the NIST OSCAL framework.
          </p>
          <p className="mt-2">
            Lead Developer:{' '}
            <a
              href="https://www.linkedin.com/in/travishowerton/"
              target="_blank"
              rel="noopener noreferrer"
              className="text-primary hover:underline inline-flex items-center"
            >
              Travis Howerton
              <ExternalLink className="h-3 w-3 ml-1" />
            </a>
          </p>
          <p className="mt-2">
            Free & Open Source - Unlimited production use.{' '}
            No commercial resale or derivative works without permission.{' '}
            <Link href="/license" className="text-primary hover:underline">
              View License
            </Link>
          </p>
        </div>
      </div>
    </footer>
  );
}
