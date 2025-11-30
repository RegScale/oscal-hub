import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  output: 'standalone', // Enable standalone build for Docker
  eslint: {
    ignoreDuringBuilds: true, // Skip ESLint errors during build
  },

  // Note: API proxying is now handled by Next.js API routes in src/app/api/[...path]/route.ts
  // This is more reliable than rewrites for POST requests with bodies in standalone mode
};

export default nextConfig;
