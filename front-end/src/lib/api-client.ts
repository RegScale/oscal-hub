import type {
  ValidationResult,
  ValidationError,
  ConversionRequest,
  ConversionResult,
  ProfileResolutionRequest,
  ProfileResolutionResult,
  OscalModelType,
  OscalFormat,
  BatchOperationRequest,
  BatchOperationResult,
  OperationHistory,
  OperationStats,
  OperationHistoryPage,
  SavedFile,
  ValidationRulesResponse,
  ValidationRulesStats,
  ValidationRuleCategory,
  CustomRuleRequest,
  CustomRuleResponse,
  LibraryItem,
  LibraryItemRequest,
  LibraryItemUpdateRequest,
  LibraryVersion,
  LibraryVersionRequest,
  LibraryTag,
  LibraryAnalytics,
  ServiceAccountTokenRequest,
  ServiceAccountTokenResponse,
  SspVisualizationData,
} from '@/types/oscal';
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '@/types/auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';
const USE_MOCK = process.env.NEXT_PUBLIC_USE_MOCK === 'true';

class ApiClient {
  private getAuthHeaders(): HeadersInit {
    const token = localStorage.getItem('token');
    return token
      ? {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        }
      : { 'Content-Type': 'application/json' };
  }

  private async fetchWithTimeout(
    url: string,
    options: RequestInit,
    timeout = 5000
  ): Promise<Response> {
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), timeout);

    try {
      const response = await fetch(url, {
        ...options,
        signal: controller.signal,
      });
      clearTimeout(id);
      return response;
    } catch (error) {
      clearTimeout(id);
      throw error;
    }
  }

  /**
   * Login user
   */
  async login(username: string, password: string): Promise<AuthResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/login`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username, password } as LoginRequest),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Login failed');
      }

      const authResponse: AuthResponse = await response.json();

      // Store token in localStorage
      localStorage.setItem('token', authResponse.token);
      localStorage.setItem('user', JSON.stringify({
        userId: authResponse.userId,
        username: authResponse.username,
        email: authResponse.email,
      }));

      return authResponse;
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  }

  /**
   * Register new user
   */
  async register(username: string, password: string, email: string): Promise<AuthResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/register`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username, password, email } as RegisterRequest),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Registration failed');
      }

      const authResponse: AuthResponse = await response.json();

      // Store token in localStorage
      localStorage.setItem('token', authResponse.token);
      localStorage.setItem('user', JSON.stringify({
        userId: authResponse.userId,
        username: authResponse.username,
        email: authResponse.email,
      }));

      return authResponse;
    } catch (error) {
      console.error('Registration failed:', error);
      throw error;
    }
  }

  /**
   * Logout user
   */
  async logout(): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/logout`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      // Clear local storage regardless of response
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    } catch (error) {
      // Clear local storage even if request fails
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      console.error('Logout request failed:', error);
    }
  }

  /**
   * Get current user info
   */
  async getCurrentUser(): Promise<User> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/me`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error('Failed to get current user');
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get current user:', error);
      throw error;
    }
  }

  /**
   * Refresh authentication token
   */
  async refreshToken(): Promise<AuthResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/refresh`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error('Failed to refresh token');
      }

      const authResponse: AuthResponse = await response.json();

      // Update token in localStorage
      localStorage.setItem('token', authResponse.token);
      localStorage.setItem('user', JSON.stringify({
        userId: authResponse.userId,
        username: authResponse.username,
        email: authResponse.email,
      }));

      return authResponse;
    } catch (error) {
      console.error('Failed to refresh token:', error);
      throw error;
    }
  }

  /**
   * Update user profile (email and/or password)
   */
  async updateProfile(updates: { email?: string; password?: string }): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/profile`,
        {
          method: 'PUT',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(updates),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to update profile');
      }

      const result = await response.json();

      // Update user info in localStorage if email changed
      if (updates.email) {
        const storedUser = localStorage.getItem('user');
        if (storedUser) {
          const user = JSON.parse(storedUser);
          user.email = result.email;
          localStorage.setItem('user', JSON.stringify(user));
        }
      }
    } catch (error) {
      console.error('Failed to update profile:', error);
      throw error;
    }
  }

  /**
   * Validate an OSCAL document
   */
  async validate(
    content: string,
    modelType: OscalModelType,
    format: OscalFormat,
    fileName?: string
  ): Promise<ValidationResult> {
    if (USE_MOCK) {
      return this.mockValidate(content, modelType, format);
    }

    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/validate`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify({
            content,
            modelType,
            format: format.toUpperCase(),
            fileName
          }),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Validation failed: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.warn('Backend unavailable, using mock validation');
      return this.mockValidate(content, modelType, format);
    }
  }

  /**
   * Convert OSCAL document between formats
   */
  async convert(request: ConversionRequest): Promise<ConversionResult> {
    if (USE_MOCK) {
      return this.mockConvert(request);
    }

    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/convert`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify({
            ...request,
            fromFormat: request.fromFormat.toUpperCase(),
            toFormat: request.toFormat.toUpperCase()
          }),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Conversion failed: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.warn('Backend unavailable, using mock conversion');
      return this.mockConvert(request);
    }
  }

  /**
   * Resolve OSCAL profile to catalog
   */
  async resolveProfile(
    request: ProfileResolutionRequest
  ): Promise<ProfileResolutionResult> {
    if (USE_MOCK) {
      return this.mockResolveProfile(request);
    }

    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/profile/resolve`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        15000
      );

      if (!response.ok) {
        throw new Error(`Profile resolution failed: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.warn('Backend unavailable, using mock profile resolution');
      return this.mockResolveProfile(request);
    }
  }

  /**
   * Visualize System Security Plan
   */
  async visualizeSSP(
    content: string,
    format: OscalFormat,
    fileName?: string
  ): Promise<SspVisualizationData> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/visualization/ssp`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify({
            content,
            format: format.toUpperCase(),
            fileName
          }),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`SSP visualization failed: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('SSP visualization failed:', error);
      throw error;
    }
  }

  /**
   * Submit a batch operation
   */
  async submitBatchOperation(
    request: BatchOperationRequest
  ): Promise<BatchOperationResult> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/batch`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        15000
      );

      if (!response.ok) {
        throw new Error(`Batch operation failed: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Batch operation submission failed:', error);
      throw error;
    }
  }

  /**
   * Get batch operation status and results
   */
  async getBatchOperationResult(operationId: string): Promise<BatchOperationResult> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/batch/${operationId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Failed to get batch operation result: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get batch operation result:', error);
      throw error;
    }
  }

  /**
   * Get recent operations (last 10)
   */
  async getRecentOperations(): Promise<OperationHistory[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/history/recent`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get recent operations: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get recent operations:', error);
      return [];
    }
  }

  /**
   * Get operation statistics
   */
  async getOperationStats(): Promise<OperationStats> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/history/stats`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get operation stats: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get operation stats:', error);
      return {
        totalOperations: 0,
        successfulOperations: 0,
        failedOperations: 0,
        validateCount: 0,
        convertCount: 0,
        resolveCount: 0,
        batchCount: 0,
        successRate: 0,
      };
    }
  }

  /**
   * Get paginated operation history
   */
  async getOperationHistory(page = 0, size = 20): Promise<OperationHistoryPage> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/history?page=${page}&size=${size}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get operation history: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get operation history:', error);
      return {
        content: [],
        totalPages: 0,
        totalElements: 0,
        size: size,
        number: page,
      };
    }
  }

  /**
   * Delete an operation from history
   */
  async deleteOperation(id: number): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/history/${id}`,
        {
          method: 'DELETE',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to delete operation: ${response.statusText}`);
      }
    } catch (error) {
      console.error('Failed to delete operation:', error);
      throw error;
    }
  }

  /**
   * Get all operations in a batch
   */
  async getBatchOperations(batchOperationId: string): Promise<OperationHistory[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/history/batch/${batchOperationId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get batch operations: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get batch operations:', error);
      return [];
    }
  }

  /**
   * Get all saved files
   */
  async getSavedFiles(): Promise<SavedFile[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/files`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get saved files: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get saved files:', error);
      return [];
    }
  }

  /**
   * Get a saved file by ID
   */
  async getSavedFile(fileId: string): Promise<SavedFile | null> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/files/${fileId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get saved file: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get saved file:', error);
      return null;
    }
  }

  /**
   * Get file content by ID
   */
  async getFileContent(fileId: string): Promise<string | null> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/files/${fileId}/content`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get file content: ${response.statusText}`);
      }

      const data = await response.json();
      return data.content;
    } catch (error) {
      console.error('Failed to get file content:', error);
      return null;
    }
  }

  /**
   * Delete a saved file
   */
  async deleteSavedFile(fileId: string): Promise<boolean> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/files/${fileId}`,
        {
          method: 'DELETE',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      return response.ok;
    } catch (error) {
      console.error('Failed to delete saved file:', error);
      return false;
    }
  }

  /**
   * Save a file to storage
   */
  async saveFile(
    content: string,
    fileName: string,
    format: OscalFormat,
    modelType?: OscalModelType
  ): Promise<SavedFile | null> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/files`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify({
            content,
            fileName,
            format: format.toUpperCase(),
            modelType,
          }),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Failed to save file: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to save file:', error);
      return null;
    }
  }

  /**
   * Get all validation rules
   */
  async getValidationRules(): Promise<ValidationRulesResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get validation rules: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get validation rules:', error);
      throw error;
    }
  }

  /**
   * Get validation rules for a specific model type
   */
  async getValidationRulesForModel(modelType: OscalModelType): Promise<ValidationRulesResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/model/${modelType}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get validation rules for model: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get validation rules for model:', error);
      throw error;
    }
  }

  /**
   * Get validation rules statistics
   */
  async getValidationRulesStats(): Promise<ValidationRulesStats> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/stats`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get validation rules stats: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get validation rules stats:', error);
      throw error;
    }
  }

  /**
   * Get validation rule categories
   */
  async getValidationRuleCategories(): Promise<ValidationRuleCategory[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/categories`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get validation rule categories: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get validation rule categories:', error);
      throw error;
    }
  }

  /**
   * Get all custom rules
   */
  async getAllCustomRules(): Promise<CustomRuleResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/custom`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get custom rules: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get custom rules:', error);
      throw error;
    }
  }

  /**
   * Get custom rule by ID
   */
  async getCustomRuleById(id: number): Promise<CustomRuleResponse | null> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/custom/${id}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        if (response.status === 404) {
          return null;
        }
        throw new Error(`Failed to get custom rule: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get custom rule:', error);
      return null;
    }
  }

  /**
   * Get custom rule by rule ID
   */
  async getCustomRuleByRuleId(ruleId: string): Promise<CustomRuleResponse | null> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/custom/rule/${ruleId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        if (response.status === 404) {
          return null;
        }
        throw new Error(`Failed to get custom rule: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get custom rule:', error);
      return null;
    }
  }

  /**
   * Create a new custom rule
   */
  async createCustomRule(request: CustomRuleRequest): Promise<CustomRuleResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/custom`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        5000
      );

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to create custom rule: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to create custom rule:', error);
      throw error;
    }
  }

  /**
   * Update an existing custom rule
   */
  async updateCustomRule(id: number, request: CustomRuleRequest): Promise<CustomRuleResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/custom/${id}`,
        {
          method: 'PUT',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        5000
      );

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to update custom rule: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to update custom rule:', error);
      throw error;
    }
  }

  /**
   * Delete a custom rule
   */
  async deleteCustomRule(id: number): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/custom/${id}`,
        {
          method: 'DELETE',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to delete custom rule: ${response.statusText}`);
      }
    } catch (error) {
      console.error('Failed to delete custom rule:', error);
      throw error;
    }
  }

  /**
   * Toggle custom rule enabled status
   */
  async toggleCustomRuleEnabled(id: number): Promise<CustomRuleResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/custom/${id}/toggle`,
        {
          method: 'PATCH',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to toggle custom rule: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to toggle custom rule:', error);
      throw error;
    }
  }

  /**
   * Get enabled custom rules
   */
  async getEnabledCustomRules(): Promise<CustomRuleResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/custom/enabled`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get enabled custom rules: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get enabled custom rules:', error);
      throw error;
    }
  }

  /**
   * Get custom rules by category
   */
  async getCustomRulesByCategory(category: string): Promise<CustomRuleResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/custom/category/${category}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get custom rules by category: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get custom rules by category:', error);
      throw error;
    }
  }

  /**
   * Get custom rules by model type
   */
  async getCustomRulesByModelType(modelType: string): Promise<CustomRuleResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/rules/custom/model/${modelType}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get custom rules by model type: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get custom rules by model type:', error);
      throw error;
    }
  }

  // ========================================
  // Library API Methods
  // ========================================

  /**
   * Create a new library item
   */
  async createLibraryItem(request: LibraryItemRequest): Promise<LibraryItem> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        15000
      );

      if (!response.ok) {
        throw new Error(`Failed to create library item: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to create library item:', error);
      throw error;
    }
  }

  /**
   * Update library item metadata
   */
  async updateLibraryItem(itemId: string, request: LibraryItemUpdateRequest): Promise<LibraryItem> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/${itemId}`,
        {
          method: 'PUT',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to update library item: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to update library item:', error);
      throw error;
    }
  }

  /**
   * Add a new version to a library item
   */
  async addLibraryVersion(itemId: string, request: LibraryVersionRequest): Promise<LibraryVersion> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/${itemId}/versions`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        15000
      );

      if (!response.ok) {
        throw new Error(`Failed to add library version: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to add library version:', error);
      throw error;
    }
  }

  /**
   * Get a library item by ID
   */
  async getLibraryItem(itemId: string): Promise<LibraryItem> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/${itemId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get library item: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get library item:', error);
      throw error;
    }
  }

  /**
   * Get library item file content
   */
  async getLibraryItemContent(itemId: string): Promise<string> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/${itemId}/content`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get library item content: ${response.statusText}`);
      }

      const data = await response.json();
      return data.content;
    } catch (error) {
      console.error('Failed to get library item content:', error);
      throw error;
    }
  }

  /**
   * Get version history for a library item
   */
  async getLibraryVersionHistory(itemId: string): Promise<LibraryVersion[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/${itemId}/versions`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get version history: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get version history:', error);
      throw error;
    }
  }

  /**
   * Get specific version content
   */
  async getLibraryVersionContent(versionId: string): Promise<string> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/versions/${versionId}/content`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get version content: ${response.statusText}`);
      }

      const data = await response.json();
      return data.content;
    } catch (error) {
      console.error('Failed to get version content:', error);
      throw error;
    }
  }

  /**
   * Delete a library item
   */
  async deleteLibraryItem(itemId: string): Promise<boolean> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/${itemId}`,
        {
          method: 'DELETE',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      return response.ok;
    } catch (error) {
      console.error('Failed to delete library item:', error);
      return false;
    }
  }

  /**
   * Search library items
   */
  async searchLibrary(params: { q?: string; oscalType?: string; tag?: string }): Promise<LibraryItem[]> {
    try {
      const queryParams = new URLSearchParams();
      if (params.q) queryParams.append('q', params.q);
      if (params.oscalType) queryParams.append('oscalType', params.oscalType);
      if (params.tag) queryParams.append('tag', params.tag);

      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/search?${queryParams.toString()}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to search library: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to search library:', error);
      return [];
    }
  }

  /**
   * Get all library items
   */
  async getAllLibraryItems(): Promise<LibraryItem[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get library items: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get library items:', error);
      return [];
    }
  }

  /**
   * Get library items by OSCAL type
   */
  async getLibraryItemsByType(oscalType: string): Promise<LibraryItem[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/type/${oscalType}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get library items by type: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get library items by type:', error);
      return [];
    }
  }

  /**
   * Get most popular library items
   */
  async getMostPopularLibraryItems(limit = 10): Promise<LibraryItem[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/popular?limit=${limit}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get popular items: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get popular items:', error);
      return [];
    }
  }

  /**
   * Get recently updated library items
   */
  async getRecentlyUpdatedLibraryItems(limit = 10): Promise<LibraryItem[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/recent?limit=${limit}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get recent items: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get recent items:', error);
      return [];
    }
  }

  /**
   * Get library analytics
   */
  async getLibraryAnalytics(): Promise<LibraryAnalytics> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/analytics`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get library analytics: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get library analytics:', error);
      throw error;
    }
  }

  /**
   * Get all library tags
   */
  async getAllLibraryTags(): Promise<LibraryTag[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/tags`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get library tags: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get library tags:', error);
      return [];
    }
  }

  /**
   * Get popular library tags
   */
  async getPopularLibraryTags(limit = 10): Promise<LibraryTag[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/library/tags/popular?limit=${limit}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get popular tags: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get popular tags:', error);
      return [];
    }
  }

  /**
   * Generate a service account JWT token
   */
  async generateServiceAccountToken(
    request: ServiceAccountTokenRequest
  ): Promise<ServiceAccountTokenResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/service-account-token`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to generate service account token');
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to generate service account token:', error);
      throw error;
    }
  }

  // Mock implementations for development without backend

  private async mockValidate(
    content: string,
    modelType: OscalModelType,
    format: OscalFormat
  ): Promise<ValidationResult> {
    // Simulate API delay
    await new Promise((resolve) => setTimeout(resolve, 800));

    // Simple validation: check if content is not empty and has basic structure
    const isEmpty = !content || content.trim().length === 0;
    const hasBasicStructure =
      (format === 'json' && content.includes('{')) ||
      (format === 'xml' && content.includes('<')) ||
      (format === 'yaml' && content.includes(':'));

    if (isEmpty) {
      return {
        valid: false,
        errors: [
          {
            line: 1,
            column: 1,
            message: 'Document is empty',
            severity: 'error',
          },
        ],
        warnings: [],
        timestamp: new Date().toISOString(),
      };
    }

    if (!hasBasicStructure) {
      return {
        valid: false,
        errors: [
          {
            line: 1,
            column: 1,
            message: `Invalid ${format.toUpperCase()} structure`,
            severity: 'error',
          },
        ],
        warnings: [],
        timestamp: new Date().toISOString(),
      };
    }

    // Mock some validation errors for demonstration
    const mockErrors: ValidationError[] = [];
    const mockWarnings: ValidationError[] = [];

    // Randomly add a warning
    if (Math.random() > 0.7) {
      mockWarnings.push({
        line: Math.floor(Math.random() * 20) + 1,
        column: 1,
        message: 'Consider adding metadata for better compliance tracking',
        severity: 'warning' as const,
        path: '/metadata/remarks',
      });
    }

    return {
      valid: mockErrors.length === 0,
      errors: mockErrors,
      warnings: mockWarnings,
      modelType,
      format,
      timestamp: new Date().toISOString(),
    };
  }

  private async mockConvert(request: ConversionRequest): Promise<ConversionResult> {
    await new Promise((resolve) => setTimeout(resolve, 1000));

    return {
      success: true,
      content: `<!-- Mock converted content from ${request.fromFormat} to ${request.toFormat} -->\n${request.content}`,
      fromFormat: request.fromFormat,
      toFormat: request.toFormat,
    };
  }

  private async mockResolveProfile(
    request: ProfileResolutionRequest
  ): Promise<ProfileResolutionResult> {
    await new Promise((resolve) => setTimeout(resolve, 1200));

    return {
      success: true,
      resolvedCatalog: `<!-- Mock resolved catalog -->\n${request.profileContent}`,
      controlCount: 42,
    };
  }
}

// Export singleton instance
export const apiClient = new ApiClient();
