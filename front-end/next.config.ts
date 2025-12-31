import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  output: 'standalone', // Enable standalone build for Docker

  // Note: ESLint configuration moved to .eslintrc or package.json
  // To skip ESLint during builds, use: ESLINT_NO_DEV_ERRORS=true or --no-lint flag

  // Note: API proxying is now handled by Next.js API routes in src/app/api/[...path]/route.ts
  // This is more reliable than rewrites for POST requests with bodies in standalone mode
};

export default nextConfig;
