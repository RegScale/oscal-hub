'use client';

import { useEffect, useRef, useState, useCallback } from 'react';

/**
 * WebSocket connection states
 */
export type WebSocketState = 'CONNECTING' | 'CONNECTED' | 'DISCONNECTED' | 'ERROR';

/**
 * Message received from WebSocket
 */
export interface WebSocketMessage<T = unknown> {
  operationId?: string;
  status?: string;
  result?: T;
  error?: string;
  timestamp?: number;
  [key: string]: unknown;
}

/**
 * Options for useWebSocket hook
 */
interface UseWebSocketOptions {
  /** Auto-connect on mount (default: true) */
  autoConnect?: boolean;
  /** Auto-reconnect on disconnect (default: true) */
  autoReconnect?: boolean;
  /** Reconnect interval in ms (default: 5000) */
  reconnectInterval?: number;
  /** Max reconnect attempts (default: 5) */
  maxReconnectAttempts?: number;
}

/**
 * Hook for WebSocket connections with auto-reconnect support.
 *
 * Note: This is a simplified WebSocket hook. For production use with STOMP,
 * consider using @stomp/stompjs library.
 *
 * @example
 * ```tsx
 * const { state, subscribe, unsubscribe } = useWebSocket();
 *
 * useEffect(() => {
 *   const unsubscribe = subscribe('/topic/async/' + operationId, (message) => {
 *     if (message.status === 'COMPLETED') {
 *       // Handle completion
 *     }
 *   });
 *   return unsubscribe;
 * }, [operationId]);
 * ```
 */
export function useWebSocket(options: UseWebSocketOptions = {}) {
  const {
    autoConnect = true,
    autoReconnect = true,
    reconnectInterval = 5000,
    maxReconnectAttempts = 5,
  } = options;

  const [state, setState] = useState<WebSocketState>('DISCONNECTED');
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const subscriptionsRef = useRef<Map<string, Set<(message: WebSocketMessage) => void>>>(new Map());

  const getWebSocketUrl = useCallback(() => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
    // Convert http(s) to ws(s)
    const wsUrl = apiUrl.replace(/^http/, 'ws');
    return `${wsUrl}/ws`;
  }, []);

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return;

    setState('CONNECTING');

    try {
      const ws = new WebSocket(getWebSocketUrl());
      wsRef.current = ws;

      ws.onopen = () => {
        setState('CONNECTED');
        reconnectAttemptsRef.current = 0;
        console.log('[WebSocket] Connected');
      };

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data) as WebSocketMessage;
          const topic = data.operationId ? `/topic/async/${data.operationId}` : '/topic/notifications';

          // Notify all subscribers for this topic
          const callbacks = subscriptionsRef.current.get(topic);
          if (callbacks) {
            callbacks.forEach((callback) => callback(data));
          }

          // Also notify wildcard subscribers
          const wildcardCallbacks = subscriptionsRef.current.get('*');
          if (wildcardCallbacks) {
            wildcardCallbacks.forEach((callback) => callback(data));
          }
        } catch (e) {
          console.error('[WebSocket] Failed to parse message:', e);
        }
      };

      ws.onclose = () => {
        setState('DISCONNECTED');
        console.log('[WebSocket] Disconnected');

        // Auto-reconnect if enabled
        if (autoReconnect && reconnectAttemptsRef.current < maxReconnectAttempts) {
          reconnectAttemptsRef.current++;
          console.log(`[WebSocket] Reconnecting (attempt ${reconnectAttemptsRef.current}/${maxReconnectAttempts})...`);
          setTimeout(connect, reconnectInterval);
        }
      };

      ws.onerror = (error) => {
        setState('ERROR');
        console.error('[WebSocket] Error:', error);
      };
    } catch (error) {
      setState('ERROR');
      console.error('[WebSocket] Connection failed:', error);
    }
  }, [getWebSocketUrl, autoReconnect, maxReconnectAttempts, reconnectInterval]);

  const disconnect = useCallback(() => {
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    setState('DISCONNECTED');
    reconnectAttemptsRef.current = maxReconnectAttempts; // Prevent auto-reconnect
  }, [maxReconnectAttempts]);

  const subscribe = useCallback(
    (topic: string, callback: (message: WebSocketMessage) => void) => {
      if (!subscriptionsRef.current.has(topic)) {
        subscriptionsRef.current.set(topic, new Set());
      }
      subscriptionsRef.current.get(topic)!.add(callback);

      // Return unsubscribe function
      return () => {
        const callbacks = subscriptionsRef.current.get(topic);
        if (callbacks) {
          callbacks.delete(callback);
          if (callbacks.size === 0) {
            subscriptionsRef.current.delete(topic);
          }
        }
      };
    },
    []
  );

  const unsubscribe = useCallback((topic: string) => {
    subscriptionsRef.current.delete(topic);
  }, []);

  // Auto-connect on mount
  useEffect(() => {
    if (autoConnect) {
      connect();
    }

    return () => {
      if (wsRef.current) {
        wsRef.current.close();
      }
    };
  }, [autoConnect, connect]);

  return {
    state,
    isConnected: state === 'CONNECTED',
    connect,
    disconnect,
    subscribe,
    unsubscribe,
  };
}

/**
 * Hook for subscribing to async operation updates.
 *
 * @example
 * ```tsx
 * const { status, result, error } = useAsyncOperation(operationId);
 *
 * if (status === 'COMPLETED') {
 *   // Handle result
 * }
 * ```
 */
export function useAsyncOperation<T = unknown>(operationId: string | null) {
  const [status, setStatus] = useState<string>('PENDING');
  const [result, setResult] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { subscribe, isConnected } = useWebSocket();

  useEffect(() => {
    if (!operationId || !isConnected) return;

    const topic = `/topic/async/${operationId}`;
    const unsubscribe = subscribe(topic, (message) => {
      const typedMessage = message as WebSocketMessage<T>;
      if (typedMessage.status) {
        setStatus(typedMessage.status);
      }
      if (typedMessage.result) {
        setResult(typedMessage.result);
      }
      if (typedMessage.error) {
        setError(typedMessage.error);
      }
    });

    return unsubscribe;
  }, [operationId, subscribe, isConnected]);

  return {
    status,
    result,
    error,
    isComplete: status === 'COMPLETED' || status === 'FAILED',
    isSuccess: status === 'COMPLETED' && !error,
  };
}

export default useWebSocket;
