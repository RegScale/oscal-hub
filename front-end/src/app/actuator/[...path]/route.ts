/**
 * Actuator Proxy for Spring Boot actuator endpoints
 */

import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.NODE_ENV === 'production'
  ? 'http://localhost:8081'
  : 'http://localhost:8080';

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ path: string[] }> }
) {
  const { path: pathSegments } = await params;
  const path = pathSegments.join('/');
  const url = new URL(request.url);
  const backendUrl = `${BACKEND_URL}/actuator/${path}${url.search}`;

  try {
    const backendResponse = await fetch(backendUrl);
    const responseBody = await backendResponse.text();

    const response = new NextResponse(responseBody, {
      status: backendResponse.status,
      statusText: backendResponse.statusText,
    });

    backendResponse.headers.forEach((value, key) => {
      response.headers.set(key, value);
    });

    return response;
  } catch (error) {
    return NextResponse.json(
      { error: 'Backend unavailable' },
      { status: 503 }
    );
  }
}
