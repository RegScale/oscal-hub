import Link from "next/link";

export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex flex-col bg-white">
      <header className="border-b bg-white">
        <div className="container mx-auto px-4 py-3 flex items-center justify-between">
          <Link href="/catalog" className="text-lg font-semibold text-slate-900">
            OSCAL Hub — Public Catalog
          </Link>
          <Link href="/login"
                className="text-sm text-blue-600 hover:underline">
            Sign in
          </Link>
        </div>
      </header>
      <main className="flex-1 container mx-auto px-4 py-6">{children}</main>
      <footer className="border-t bg-slate-50 py-4 text-center text-xs text-slate-500">
        Public OSCAL content. Sign in to download or rate.
      </footer>
    </div>
  );
}
