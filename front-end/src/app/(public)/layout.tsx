/**
 * Pass-through layout for public-catalog routes.
 *
 * The root layout already renders the global Navigation + Footer (which
 * gracefully handle the unauthenticated state via a 'Login' button), so
 * /catalog and /catalog/[itemId] inherit the same chrome as every other
 * page. Wrapping the route group in a separate layout used to drop those
 * controls; now we simply forward children.
 */
export default function PublicLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
