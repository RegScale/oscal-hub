import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* config options here */
  output: 'standalone', // Enable standalone build for Docker
  eslint: {
    ignoreDuringBuilds: true, // Skip ESLint errors during build
  },

  // Proxy API requests to backend (for single-container deployment)
  async rewrites() {
    // In production (GCP), backend runs on port 8081 in same container
    // In development, backend runs on port 8080
    const backendPort = process.env.NODE_ENV === 'production' ? '8081' : '8080';

    return [
      {
        source: '/api/:path*',
        destination: `http://localhost:${backendPort}/api/:path*`,
      },
      {
        source: '/actuator/:path*',
        destination: `http://localhost:${backendPort}/actuator/:path*`,
      },
      {
        source: '/v3/api-docs/:path*',
        destination: `http://localhost:${backendPort}/v3/api-docs/:path*`,
      },
      {
        source: '/v3/api-docs',
        destination: `http://localhost:${backendPort}/v3/api-docs`,
      },
      {
        source: '/swagger-ui/:path*',
        destination: `http://localhost:${backendPort}/swagger-ui/:path*`,
      },
    ];
  },
};

export default nextConfig;
