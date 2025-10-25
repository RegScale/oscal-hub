import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { apiClient } from './api-client';
import type { OscalFormat, OscalModelType, ConversionRequest, ProfileResolutionRequest, BatchOperationRequest } from '@/types/oscal';

// Mock fetch globally
const mockFetch = vi.fn();
global.fetch = mockFetch;

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};

  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => {
      store[key] = value;
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  };
})();

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
  writable: true,
});

describe('ApiClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorageMock.clear();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // ========== AUTHENTICATION TESTS ==========

  describe('Authentication', () => {
    describe('login', () => {
      it('should successfully login and store token', async () => {
        const mockResponse = {
          token: 'test-token-123',
          username: 'testuser',
          email: 'test@example.com',
          userId: 1,
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse,
        });

        const result = await apiClient.login('testuser', 'password123');

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/auth/login'),
          expect.objectContaining({
            method: 'POST',
            headers: expect.objectContaining({
              'Content-Type': 'application/json',
            }),
            body: JSON.stringify({
              username: 'testuser',
              password: 'password123',
            }),
          })
        );

        expect(result).toEqual(mockResponse);
        expect(localStorageMock.getItem('token')).toBe('test-token-123');
        expect(JSON.parse(localStorageMock.getItem('user')!)).toMatchObject({
          username: 'testuser',
          email: 'test@example.com',
          userId: 1,
        });
      });

      it('should throw error on invalid credentials', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: false,
          json: async () => ({ error: 'Invalid credentials' }),
        });

        await expect(apiClient.login('testuser', 'wrongpassword')).rejects.toThrow('Invalid credentials');
      });
    });

    describe('register', () => {
      it('should successfully register and store token', async () => {
        const mockResponse = {
          token: 'new-token-456',
          username: 'newuser',
          email: 'new@example.com',
          userId: 2,
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse,
        });

        const result = await apiClient.register('newuser', 'Password123!', 'new@example.com');

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/auth/register'),
          expect.objectContaining({
            method: 'POST',
            body: JSON.stringify({
              username: 'newuser',
              password: 'Password123!',
              email: 'new@example.com',
            }),
          })
        );

        expect(result).toEqual(mockResponse);
        expect(localStorageMock.getItem('token')).toBe('new-token-456');
      });

      it('should handle registration errors', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: false,
          json: async () => ({ error: 'Username already exists' }),
        });

        await expect(
          apiClient.register('existinguser', 'Password123!', 'test@example.com')
        ).rejects.toThrow('Username already exists');
      });
    });

    describe('logout', () => {
      it('should clear stored credentials', async () => {
        localStorageMock.setItem('token', 'test-token');
        localStorageMock.setItem('user', JSON.stringify({ username: 'testuser' }));

        mockFetch.mockResolvedValueOnce({
          ok: true,
        });

        await apiClient.logout();

        expect(localStorageMock.getItem('token')).toBeNull();
        expect(localStorageMock.getItem('user')).toBeNull();
      });

      it('should clear credentials even if API call fails', async () => {
        localStorageMock.setItem('token', 'test-token');
        localStorageMock.setItem('user', JSON.stringify({ username: 'testuser' }));

        mockFetch.mockRejectedValueOnce(new Error('Network error'));

        await apiClient.logout();

        expect(localStorageMock.getItem('token')).toBeNull();
        expect(localStorageMock.getItem('user')).toBeNull();
      });
    });

    describe('getCurrentUser', () => {
      it('should fetch current user from API', async () => {
        const mockUser = {
          id: 1,
          username: 'testuser',
          email: 'test@example.com',
        };

        localStorageMock.setItem('token', 'test-token');

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockUser,
        });

        const result = await apiClient.getCurrentUser();

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/auth/me'),
          expect.objectContaining({
            method: 'GET',
            headers: expect.objectContaining({
              Authorization: 'Bearer test-token',
            }),
          })
        );

        expect(result).toEqual(mockUser);
      });

      it('should throw error when user not found', async () => {
        localStorageMock.setItem('token', 'test-token');

        mockFetch.mockResolvedValueOnce({
          ok: false,
        });

        await expect(apiClient.getCurrentUser()).rejects.toThrow('Failed to get current user');
      });
    });

    describe('refreshToken', () => {
      it('should refresh expired token', async () => {
        const mockResponse = {
          token: 'new-token-789',
          username: 'testuser',
          email: 'test@example.com',
          userId: 1,
        };

        localStorageMock.setItem('token', 'old-token');

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse,
        });

        const result = await apiClient.refreshToken();

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/auth/refresh'),
          expect.objectContaining({
            method: 'POST',
            headers: expect.objectContaining({
              Authorization: 'Bearer old-token',
            }),
          })
        );

        expect(result).toEqual(mockResponse);
        expect(localStorageMock.getItem('token')).toBe('new-token-789');
      });

      it('should handle refresh failure', async () => {
        localStorageMock.setItem('token', 'old-token');

        mockFetch.mockResolvedValueOnce({
          ok: false,
        });

        await expect(apiClient.refreshToken()).rejects.toThrow('Failed to refresh token');
      });
    });

    describe('updateProfile', () => {
      it('should update user profile successfully', async () => {
        const mockUser = {
          id: 1,
          username: 'testuser',
          email: 'newemail@example.com',
        };

        localStorageMock.setItem('token', 'test-token');
        localStorageMock.setItem('user', JSON.stringify({ username: 'testuser', email: 'old@example.com' }));

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockUser,
        });

        const updates = { email: 'newemail@example.com' };
        await apiClient.updateProfile(updates);

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/auth/profile'),
          expect.objectContaining({
            method: 'PUT',
            body: JSON.stringify(updates),
          })
        );
      });
    });

    describe('uploadLogo', () => {
      it('should upload user logo successfully', async () => {
        const mockResponse = {
          logo: 'data:image/png;base64,abc123',
        };

        localStorageMock.setItem('token', 'test-token');
        localStorageMock.setItem('user', JSON.stringify({ username: 'testuser' }));

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse,
          headers: new Headers({ 'content-type': 'application/json' }),
        });

        const logoData = 'data:image/png;base64,abc123';
        await apiClient.uploadLogo(logoData);

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/auth/logo'),
          expect.objectContaining({
            method: 'POST',
            body: JSON.stringify({ logo: logoData }),
          })
        );
      });
    });
  });

  // ========== OSCAL VALIDATION TESTS ==========

  describe('Validation', () => {
    beforeEach(() => {
      localStorageMock.setItem('token', 'test-token');
    });

    describe('validate', () => {
      it('should validate OSCAL document successfully', async () => {
        const mockResponse = {
          valid: true,
          modelType: 'catalog',
          format: 'XML',
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse,
        });

        const result = await apiClient.validate(
          '<catalog/>',
          'catalog' as OscalModelType,
          'xml' as OscalFormat
        );

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/validate'),
          expect.objectContaining({
            method: 'POST',
            headers: expect.objectContaining({
              'Content-Type': 'application/json',
              Authorization: 'Bearer test-token',
            }),
            body: expect.stringContaining('"content":"<catalog/>"'),
          })
        );

        expect(result.valid).toBe(true);
      });

      it('should fallback to mock validation on error', async () => {
        mockFetch.mockRejectedValueOnce(new Error('Network error'));

        const result = await apiClient.validate('<catalog/>', 'catalog', 'xml');

        // Should return mock result
        expect(result).toHaveProperty('valid');
        expect(result).toHaveProperty('errors');
      });
    });
  });

  // ========== CONVERSION TESTS ==========

  describe('Conversion', () => {
    beforeEach(() => {
      localStorageMock.setItem('token', 'test-token');
    });

    describe('convert', () => {
      it('should convert XML to JSON successfully', async () => {
        const mockResponse = {
          success: true,
          content: '{"catalog": {}}',
          fromFormat: 'XML',
          toFormat: 'JSON',
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse,
        });

        const request: ConversionRequest = {
          content: '<catalog/>',
          fromFormat: 'xml',
          toFormat: 'json',
          modelType: 'catalog',
        };

        const result = await apiClient.convert(request);

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/convert'),
          expect.objectContaining({
            method: 'POST',
          })
        );

        expect(result.success).toBe(true);
      });

      it('should fallback to mock conversion on error', async () => {
        mockFetch.mockRejectedValueOnce(new Error('Network error'));

        const request: ConversionRequest = {
          content: '<catalog/>',
          fromFormat: 'xml',
          toFormat: 'json',
          modelType: 'catalog',
        };

        const result = await apiClient.convert(request);

        expect(result.success).toBe(true);
        expect(result).toHaveProperty('content');
      });
    });
  });

  // ========== PROFILE RESOLUTION TESTS ==========

  describe('Profile Resolution', () => {
    beforeEach(() => {
      localStorageMock.setItem('token', 'test-token');
    });

    describe('resolveProfile', () => {
      it('should resolve profile successfully', async () => {
        const mockResponse = {
          success: true,
          resolvedCatalog: '<catalog/>',
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse,
        });

        const request: ProfileResolutionRequest = {
          profileContent: '<profile/>',
          format: 'xml',
        };

        const result = await apiClient.resolveProfile(request);

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/profile/resolve'),
          expect.objectContaining({
            method: 'POST',
          })
        );

        expect(result.success).toBe(true);
      });

      it('should fallback to mock resolution on error', async () => {
        mockFetch.mockRejectedValueOnce(new Error('Network error'));

        const request: ProfileResolutionRequest = {
          profileContent: '<profile/>',
          format: 'xml',
        };

        const result = await apiClient.resolveProfile(request);

        expect(result.success).toBe(true);
      });
    });
  });

  // ========== BATCH OPERATIONS TESTS ==========

  describe('Batch Operations', () => {
    beforeEach(() => {
      localStorageMock.setItem('token', 'test-token');
    });

    describe('submitBatchOperation', () => {
      it('should submit batch operation successfully', async () => {
        const mockResponse = {
          success: true,
          operationId: 'op-12345',
          totalFiles: 2,
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse,
        });

        const request: BatchOperationRequest = {
          operationType: 'validate',
          modelType: 'catalog',
          files: [
            { filename: 'catalog1.xml', content: '<catalog/>', format: 'xml' },
            { filename: 'catalog2.xml', content: '<catalog/>', format: 'xml' },
          ],
        };

        const result = await apiClient.submitBatchOperation(request);

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/batch'),
          expect.objectContaining({
            method: 'POST',
          })
        );

        expect(result.success).toBe(true);
        expect(result.totalFiles).toBe(2);
      });
    });

    describe('getBatchOperationResult', () => {
      it('should retrieve batch operation result', async () => {
        const mockResponse = {
          success: true,
          operationId: 'op-12345',
          totalFiles: 2,
          successCount: 2,
          errorCount: 0,
          results: [],
          executionTimeMs: 1000,
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse,
        });

        const result = await apiClient.getBatchOperationResult('op-12345');

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/batch/op-12345'),
          expect.objectContaining({
            method: 'GET',
          })
        );

        expect(result.success).toBe(true);
      });
    });
  });

  // ========== FILE STORAGE TESTS ==========

  describe('File Storage', () => {
    beforeEach(() => {
      localStorageMock.setItem('token', 'test-token');
    });

    describe('saveFile', () => {
      it('should save file successfully', async () => {
        const mockResponse = {
          id: 'file-123',
          fileName: 'catalog.xml',
          format: 'XML',
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResponse,
        });

        const result = await apiClient.saveFile('<catalog/>', 'catalog.xml', 'xml', 'catalog');

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/files'),
          expect.objectContaining({
            method: 'POST',
          })
        );

        expect(result).toEqual(mockResponse);
      });
    });

    describe('getSavedFiles', () => {
      it('should retrieve all saved files', async () => {
        const mockFiles = [
          { id: 'file-1', fileName: 'catalog1.xml' },
          { id: 'file-2', fileName: 'catalog2.xml' },
        ];

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockFiles,
        });

        const result = await apiClient.getSavedFiles();

        expect(result).toEqual(mockFiles);
      });
    });

    describe('deleteSavedFile', () => {
      it('should delete file successfully', async () => {
        mockFetch.mockResolvedValueOnce({
          ok: true,
        });

        const result = await apiClient.deleteSavedFile('file-123');

        expect(result).toBe(true);
      });
    });
  });

  // ========== OPERATION HISTORY TESTS ==========

  describe('Operation History', () => {
    beforeEach(() => {
      localStorageMock.setItem('token', 'test-token');
    });

    describe('getRecentOperations', () => {
      it('should retrieve recent operations', async () => {
        const mockOperations = [
          { id: 1, operationType: 'validate', timestamp: '2025-01-01' },
          { id: 2, operationType: 'convert', timestamp: '2025-01-02' },
        ];

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockOperations,
        });

        const result = await apiClient.getRecentOperations();

        expect(result).toEqual(mockOperations);
      });

      it('should return empty array on error', async () => {
        mockFetch.mockRejectedValueOnce(new Error('Network error'));

        const result = await apiClient.getRecentOperations();

        expect(result).toEqual([]);
      });
    });

    describe('getOperationStats', () => {
      it('should retrieve operation statistics', async () => {
        const mockStats = {
          totalOperations: 100,
          successfulOperations: 95,
          failedOperations: 5,
          successRate: 0.95,
        };

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockStats,
        });

        const result = await apiClient.getOperationStats();

        expect(result).toEqual(mockStats);
      });
    });
  });

  // ========== LIBRARY TESTS ==========

  describe('Library', () => {
    beforeEach(() => {
      localStorageMock.setItem('token', 'test-token');
    });

    describe('getAllLibraryItems', () => {
      it('should retrieve all library items', async () => {
        const mockItems = [
          { id: 'item-1', title: 'NIST 800-53 Catalog' },
          { id: 'item-2', title: 'Custom Profile' },
        ];

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockItems,
        });

        const result = await apiClient.getAllLibraryItems();

        expect(result).toEqual(mockItems);
      });
    });

    describe('searchLibrary', () => {
      it('should search library items', async () => {
        const mockResults = [
          { id: 'item-1', title: 'NIST 800-53 Catalog' },
        ];

        mockFetch.mockResolvedValueOnce({
          ok: true,
          json: async () => mockResults,
        });

        const result = await apiClient.searchLibrary({ q: 'NIST' });

        expect(mockFetch).toHaveBeenCalledWith(
          expect.stringContaining('/library/search?q=NIST'),
          expect.any(Object)
        );

        expect(result).toEqual(mockResults);
      });
    });
  });

  // ========== ERROR HANDLING TESTS ==========

  describe('Error Handling', () => {
    beforeEach(() => {
      localStorageMock.setItem('token', 'test-token');
    });

    it('should handle timeout errors', async () => {
      mockFetch.mockImplementationOnce(() => {
        return new Promise((_, reject) => {
          setTimeout(() => reject(new Error('Timeout')), 100);
        });
      });

      await expect(apiClient.login('user', 'pass')).rejects.toThrow();
    });

    it('should handle network errors gracefully', async () => {
      mockFetch.mockRejectedValueOnce(new Error('Network error'));

      const result = await apiClient.getSavedFiles();

      expect(result).toEqual([]);
    });
  });

  // ========== INTEGRATION TESTS ==========

  describe('Integration Scenarios', () => {
    it('should handle full authentication flow', async () => {
      // Login
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          token: 'login-token',
          username: 'testuser',
          email: 'test@example.com',
          userId: 1,
        }),
      });

      await apiClient.login('testuser', 'password');
      expect(localStorageMock.getItem('token')).toBe('login-token');

      // Perform operation
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({ valid: true }),
      });

      await apiClient.validate('<catalog/>', 'catalog', 'xml');

      // Logout
      mockFetch.mockResolvedValueOnce({ ok: true });

      await apiClient.logout();
      expect(localStorageMock.getItem('token')).toBeNull();
    });
  });
});
