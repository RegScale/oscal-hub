import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  output: 'standalone', // Enable standalone build for Docker
  eslint: {
    ignoreDuringBuilds: true, // Skip ESLint errors during build
  },
};

export default nextConfig;
