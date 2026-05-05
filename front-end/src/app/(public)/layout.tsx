import Link from "next/link";
import { Library } from "lucide-react";

export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col bg-background text-foreground">
      <header className="border-b border-border bg-background">
        <div className="container mx-auto px-4 py-3 flex items-center justify-between">
          {/* Left: logo + primary nav (mirrors the authenticated Navigation) */}
          <div className="flex items-center gap-6">
            <Link
              href="/"
              className="text-2xl font-bold bg-gradient-to-r from-blue-500 to-purple-500 bg-clip-text text-transparent"
            >
              OSCAL Hub
            </Link>
            <Link
              href="/catalog"
              className="hidden sm:inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
              <Library className="h-4 w-4" />
              Browse
            </Link>
          </div>
          {/* Right: auth */}
          <Link href="/login" className="text-sm text-blue-500 hover:underline">
            Sign in
          </Link>
        </div>
      </header>
      <main className="flex-1 container mx-auto px-4 py-6">{children}</main>
      <footer className="border-t border-border bg-muted/30 py-4 text-center text-xs text-muted-foreground">
        OSCAL Data Products — public content. Sign in to download or rate.
      </footer>
    </div>
  );
}
