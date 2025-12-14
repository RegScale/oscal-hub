/**
 * API Proxy for backend requests
 *
 * This catch-all API route proxies all /api/* requests to the Spring Boot backend.
 * This is needed because Next.js rewrites don't work reliably in standalone mode
 * or with POST requests containing bodies.
 */

import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.NODE_ENV === 'production'
  ? 'http://localhost:8081'
  : 'http://localhost:8080';

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> }
) {
  const { path } = await params;
  return proxyRequest(request, path);
}

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> }
) {
  const { path } = await params;
  return proxyRequest(request, path);
}

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> }
) {
  const { path } = await params;
  return proxyRequest(request, path);
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> }
) {
  const { path } = await params;
  return proxyRequest(request, path);
}

export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> }
) {
  const { path } = await params;
  return proxyRequest(request, path);
}

async function proxyRequest(request: NextRequest, pathSegments: string[]) {
  try {
    const path = pathSegments.join('/');
    const url = new URL(request.url);
    const backendUrl = `${BACKEND_URL}/api/${path}${url.search}`;

    // console.log(`[API Proxy] ${request.method} ${backendUrl}`);

    // Get request body if present
    // IMPORTANT: Use blob() instead of text() to preserve binary data (multipart/form-data uploads)
    let body = null;
    if (request.method !== 'GET' && request.method !== 'HEAD') {
      try {
        body = await request.blob();
      } catch (e) {
        // No body or already consumed
      }
    }

    // Forward headers (but filter out some)
    const headers = new Headers();
    request.headers.forEach((value, key) => {
      // Skip host and connection headers
      if (!['host', 'connection', 'content-length'].includes(key.toLowerCase())) {
        headers.set(key, value);
      }
    });

    // Make request to backend
    const backendResponse = await fetch(backendUrl, {
      method: request.method,
      headers,
      body,
    });

    // Get content type to determine how to handle response body
    const contentType = backendResponse.headers.get('content-type') || '';
    const isBinary = contentType.startsWith('image/') ||
                     contentType.startsWith('application/octet-stream') ||
                     contentType.startsWith('application/pdf') ||
                     contentType.includes('binary');

    // Get response body - use arrayBuffer for binary data, text for everything else
    let responseBody;
    if (isBinary) {
      responseBody = await backendResponse.arrayBuffer();
    } else {
      responseBody = await backendResponse.text();
    }

    // Create response with same status and headers
    const response = new NextResponse(responseBody, {
      status: backendResponse.status,
      statusText: backendResponse.statusText,
    });

    // Copy response headers
    backendResponse.headers.forEach((value, key) => {
      response.headers.set(key, value);
    });

    return response;
  } catch (error) {
    console.error('[API Proxy] Error:', error);
    return NextResponse.json(
      { error: 'Backend unavailable', details: String(error) },
      { status: 503 }
    );
  }
}
