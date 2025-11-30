/**
 * Health endpoint proxy
 */

import { NextRequest, NextResponse } from 'next/server';

const BACKEND_URL = process.env.NODE_ENV === 'production'
  ? 'http://localhost:8081'
  : 'http://localhost:8080';

export async function GET(request: NextRequest) {
  const url = new URL(request.url);
  const backendUrl = `${BACKEND_URL}/actuator/health${url.search}`;

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
      { status: 'DOWN', error: 'Backend unavailable' },
      { status: 503 }
    );
  }
}
