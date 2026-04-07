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
  RatingStats,
  RatingRequest,
  LibraryComment,
  CommentRequest,
  ServiceAccountTokenRequest,
  ServiceAccountTokenResponse,
  SspVisualizationData,
  ProfileVisualizationData,
  SarVisualizationData,
  AuthorizationTemplateRequest,
  AuthorizationTemplateResponse,
  AuthorizationRequest,
  AuthorizationResponse,
  ComponentDefinitionRequest,
  ComponentDefinitionResponse,
  ReusableElementRequest,
  ReusableElementResponse,
  AuditLog,
  AuditLogStats,
  SimpleHealthResponse,
  DetailedHealthResponse,
  ComponentHealth,
  ComplianceSummary,
  Soc2Control,
  GapAnalysis,
  SecurityPolicy,
  SecurityPolicyUpdateRequest,
  MfaSetupResponse,
  MfaSetupCompleteRequest,
  MfaSetupCompleteResponse,
  MfaVerifyRequest,
  MfaBackupCodeRequest,
  MfaStatus,
  Artifact,
  ArtifactRequest,
  ArtifactUpdateRequest,
  ArtifactVersion,
  ArtifactVersionRequest,
  ArtifactTag,
  ArtifactAnalytics,
  ArtifactComment,
  ArtifactVisibility,
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

  /**
   * Handle authentication errors by clearing credentials and redirecting to login.
   * This is called when the server returns 401 or 403 on authenticated endpoints,
   * indicating the token is invalid or expired.
   */
  private handleAuthError(): void {
    // Only handle if we thought we were authenticated
    const hadToken = localStorage.getItem('token');
    if (hadToken) {
      console.warn('Authentication token is invalid or expired. Redirecting to login.');
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      localStorage.removeItem('currentOrganization');

      // Redirect to login page (only in browser environment)
      if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
        window.location.href = '/login';
      }
    }
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

      // Check for auth errors on non-auth endpoints
      // (auth endpoints like /login naturally return 401/403 for bad credentials)
      const isAuthEndpoint = url.includes('/auth/login') || url.includes('/auth/register');

      // Only logout on 401 (unauthorized/invalid token)
      // 403 (forbidden) could be resource-level permissions, not token issues
      if (!isAuthEndpoint && response.status === 401) {
        this.handleAuthError();
      }

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

      // Only store token if MFA is not required (token will be present)
      // If MFA is required, mfaToken will be present instead of regular token
      if (authResponse.token && !authResponse.mfaRequired && !authResponse.mfaSetupRequired) {
        localStorage.setItem('token', authResponse.token ?? '');
        localStorage.setItem('user', JSON.stringify({
          userId: authResponse.userId,
          username: authResponse.username,
          email: authResponse.email,
          globalRole: authResponse.globalRole,
          street: authResponse.street,
          city: authResponse.city,
          state: authResponse.state,
          zip: authResponse.zip,
          title: authResponse.title,
          organization: authResponse.organization,
          phoneNumber: authResponse.phoneNumber,
          logo: authResponse.logo,
        }));
      }

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
      localStorage.setItem('token', authResponse.token ?? '');
      localStorage.setItem('user', JSON.stringify({
        userId: authResponse.userId,
        username: authResponse.username,
        email: authResponse.email,
        street: authResponse.street,
        city: authResponse.city,
        state: authResponse.state,
        zip: authResponse.zip,
        title: authResponse.title,
        organization: authResponse.organization,
        phoneNumber: authResponse.phoneNumber,
        logo: authResponse.logo,
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
      await this.fetchWithTimeout(
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
    } catch {
      // Clear local storage even if request fails
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      console.error('Logout request failed');
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
      localStorage.setItem('token', authResponse.token!);

      // Preserve existing user data (especially globalRole, organizationId, etc.)
      // and merge with refreshed data
      const existingUser = localStorage.getItem('user');
      const existingUserData = existingUser ? JSON.parse(existingUser) : {};

      localStorage.setItem('user', JSON.stringify({
        ...existingUserData, // Preserve existing fields like organizationId, orgRole
        userId: authResponse.userId,
        username: authResponse.username,
        email: authResponse.email,
        globalRole: authResponse.globalRole || existingUserData.globalRole, // Preserve if not in response
        street: authResponse.street,
        city: authResponse.city,
        state: authResponse.state,
        zip: authResponse.zip,
        title: authResponse.title,
        organization: authResponse.organization,
        phoneNumber: authResponse.phoneNumber,
      }));

      return authResponse;
    } catch (error) {
      console.error('Failed to refresh token:', error);
      throw error;
    }
  }

  /**
   * Update user profile (email, password, and/or profile metadata)
   */
  async updateProfile(updates: {
    email?: string;
    password?: string;
    firstName?: string;
    lastName?: string;
    street?: string;
    city?: string;
    state?: string;
    zip?: string;
    title?: string;
    organization?: string;
    phoneNumber?: string;
  }): Promise<void> {
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

      // Update user info in localStorage if any fields changed
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        const user = JSON.parse(storedUser);
        if (result.email !== undefined) user.email = result.email;
        if (result.street !== undefined) user.street = result.street;
        if (result.city !== undefined) user.city = result.city;
        if (result.state !== undefined) user.state = result.state;
        if (result.zip !== undefined) user.zip = result.zip;
        if (result.title !== undefined) user.title = result.title;
        if (result.organization !== undefined) user.organization = result.organization;
        if (result.phoneNumber !== undefined) user.phoneNumber = result.phoneNumber;
        localStorage.setItem('user', JSON.stringify(user));
      }
    } catch (error) {
      console.error('Failed to update profile:', error);
      throw error;
    }
  }

  /**
   * Upload user logo (base64-encoded data URL)
   */
  async uploadLogo(logo: string): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/logo`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify({ logo }),
        },
        10000
      );

      if (!response.ok) {
        // Try to parse error as JSON, but handle empty responses
        let errorMessage = 'Failed to upload logo';
        try {
          const contentType = response.headers.get('content-type');
          if (contentType && contentType.includes('application/json')) {
            const error = await response.json();
            errorMessage = error.error || error.message || errorMessage;
          } else {
            const text = await response.text();
            if (text) errorMessage = text;
          }
        } catch {
          // If JSON parsing fails, use status text
          errorMessage = response.statusText || errorMessage;
        }

        if (response.status === 403) {
          throw new Error('Access denied. Please make sure you are logged in.');
        }
        throw new Error(errorMessage);
      }

      const result = await response.json();

      // Update user info in localStorage
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        const user = JSON.parse(storedUser);
        user.logo = result.logo;
        localStorage.setItem('user', JSON.stringify(user));
      }
    } catch (error) {
      console.error('Failed to upload logo:', error);
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
    fileName?: string,
    fileId?: string
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
            fileName,
            fileId
          }),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Validation failed: ${response.statusText}`);
      }

      return await response.json();
    } catch {
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
    } catch {
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
    } catch {
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
   * Visualize Profile
   */
  async visualizeProfile(
    content: string,
    format: OscalFormat,
    fileName?: string
  ): Promise<ProfileVisualizationData> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/visualization/profile`,
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
        throw new Error(`Profile visualization failed: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Profile visualization failed:', error);
      throw error;
    }
  }

  /**
   * Visualize Security Assessment Results (SAR)
   */
  async visualizeSAR(
    content: string,
    format: OscalFormat,
    fileName?: string
  ): Promise<SarVisualizationData> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/visualization/sar`,
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
        throw new Error(`SAR visualization failed: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('SAR visualization failed:', error);
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

      const data = await response.json();
      // Backend returns paginated response with content array
      return data.content || [];
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

      const data = await response.json();
      // Backend returns paginated response with content array
      return data.content || [];
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

      const data = await response.json();
      // Backend returns paginated response with content array
      return data.content || [];
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

      const data = await response.json();
      // Backend may return paginated response or array
      return Array.isArray(data) ? data : (data.content || []);
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

      const data = await response.json();
      // Backend may return paginated response or array
      return Array.isArray(data) ? data : (data.content || []);
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

  // ========================
  // Library Rating Methods
  // ========================

  /**
   * Rate a library item (1-5 stars)
   * Creates or updates the user's rating for the item
   */
  async rateLibraryItem(itemId: string, rating: number): Promise<RatingStats> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/library/${itemId}/ratings`,
      {
        method: 'POST',
        headers: this.getAuthHeaders(),
        body: JSON.stringify({ rating } as RatingRequest),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to rate library item: ${response.statusText}`);
    }

    return await response.json();
  }

  /**
   * Get rating statistics for a library item
   */
  async getLibraryItemRatings(itemId: string): Promise<RatingStats> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/library/${itemId}/ratings`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get library item ratings: ${response.statusText}`);
    }

    return await response.json();
  }

  /**
   * Delete the user's rating for a library item
   */
  async deleteLibraryItemRating(itemId: string): Promise<void> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/library/${itemId}/ratings`,
      {
        method: 'DELETE',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to delete library item rating: ${response.statusText}`);
    }
  }

  // ========================
  // Library Comment Methods
  // ========================

  /**
   * Create a comment on a library item
   * @param itemId The library item UUID
   * @param content The comment content
   * @param parentCommentId Optional parent comment ID for replies
   */
  async createLibraryComment(
    itemId: string,
    content: string,
    parentCommentId?: string
  ): Promise<LibraryComment> {
    const request: CommentRequest = { content, parentCommentId };
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/library/${itemId}/comments`,
      {
        method: 'POST',
        headers: this.getAuthHeaders(),
        body: JSON.stringify(request),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to create comment: ${response.statusText}`);
    }

    return await response.json();
  }

  /**
   * Get all comments for a library item (threaded)
   */
  async getLibraryComments(itemId: string): Promise<LibraryComment[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/library/${itemId}/comments`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get comments: ${response.statusText}`);
    }

    return await response.json();
  }

  /**
   * Get comment count for a library item
   */
  async getLibraryCommentCount(itemId: string): Promise<number> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/library/${itemId}/comments/count`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get comment count: ${response.statusText}`);
    }

    return await response.json();
  }

  /**
   * Update a comment
   */
  async updateLibraryComment(
    itemId: string,
    commentId: string,
    content: string
  ): Promise<LibraryComment> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/library/${itemId}/comments/${commentId}`,
      {
        method: 'PUT',
        headers: this.getAuthHeaders(),
        body: JSON.stringify({ content } as CommentRequest),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to update comment: ${response.statusText}`);
    }

    return await response.json();
  }

  /**
   * Delete a comment (soft delete)
   */
  async deleteLibraryComment(itemId: string, commentId: string): Promise<void> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/library/${itemId}/comments/${commentId}`,
      {
        method: 'DELETE',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to delete comment: ${response.statusText}`);
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

  // ========================================
  // Authorization Template API Methods
  // ========================================

  /**
   * Create a new authorization template
   */
  async createAuthorizationTemplate(request: AuthorizationTemplateRequest): Promise<AuthorizationTemplateResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorization-templates`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to create template: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to create authorization template:', error);
      throw error;
    }
  }

  /**
   * Update an authorization template
   */
  async updateAuthorizationTemplate(id: number, request: AuthorizationTemplateRequest): Promise<AuthorizationTemplateResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorization-templates/${id}`,
        {
          method: 'PUT',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to update template: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to update authorization template:', error);
      throw error;
    }
  }

  /**
   * Get authorization template by ID
   */
  async getAuthorizationTemplate(id: number): Promise<AuthorizationTemplateResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorization-templates/${id}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get template: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get authorization template:', error);
      throw error;
    }
  }

  /**
   * Get all authorization templates
   */
  async getAllAuthorizationTemplates(): Promise<AuthorizationTemplateResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorization-templates`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get templates: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get authorization templates:', error);
      return [];
    }
  }

  /**
   * Search authorization templates
   */
  async searchAuthorizationTemplates(searchTerm?: string): Promise<AuthorizationTemplateResponse[]> {
    try {
      const queryParams = new URLSearchParams();
      if (searchTerm) queryParams.append('q', searchTerm);

      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorization-templates/search?${queryParams.toString()}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to search templates: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to search authorization templates:', error);
      return [];
    }
  }

  /**
   * Delete an authorization template
   */
  async deleteAuthorizationTemplate(id: number): Promise<boolean> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorization-templates/${id}`,
        {
          method: 'DELETE',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      return response.ok;
    } catch (error) {
      console.error('Failed to delete authorization template:', error);
      return false;
    }
  }

  // ========================================
  // Authorization API Methods
  // ========================================

  /**
   * Create a new authorization
   */
  async createAuthorization(request: AuthorizationRequest): Promise<AuthorizationResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorizations`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Failed to create authorization: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to create authorization:', error);
      throw error;
    }
  }

  /**
   * Update an authorization
   */
  async updateAuthorization(id: number, request: Partial<AuthorizationRequest>): Promise<AuthorizationResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorizations/${id}`,
        {
          method: 'PUT',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to update authorization: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to update authorization:', error);
      throw error;
    }
  }

  /**
   * Get authorization by ID
   */
  async getAuthorization(id: number): Promise<AuthorizationResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorizations/${id}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get authorization: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get authorization:', error);
      throw error;
    }
  }

  /**
   * Get all authorizations
   */
  async getAllAuthorizations(): Promise<AuthorizationResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorizations`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get authorizations: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get authorizations:', error);
      return [];
    }
  }

  /**
   * Get authorizations for a specific SSP
   */
  async getAuthorizationsBySsp(sspItemId: string): Promise<AuthorizationResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorizations/ssp/${sspItemId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get authorizations by SSP: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get authorizations by SSP:', error);
      return [];
    }
  }

  /**
   * Search authorizations
   */
  async searchAuthorizations(searchTerm?: string): Promise<AuthorizationResponse[]> {
    try {
      const queryParams = new URLSearchParams();
      if (searchTerm) queryParams.append('q', searchTerm);

      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorizations/search?${queryParams.toString()}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to search authorizations: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to search authorizations:', error);
      return [];
    }
  }

  /**
   * Delete an authorization
   */
  async deleteAuthorization(id: number): Promise<boolean> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorizations/${id}`,
        {
          method: 'DELETE',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      return response.ok;
    } catch (error) {
      console.error('Failed to delete authorization:', error);
      return false;
    }
  }

  /**
   * Verify digital signature on an authorization
   */
  async verifySignature(id: number): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/authorizations/${id}/verify-signature`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to verify signature: ${response.statusText}`);
      }
    } catch (error) {
      console.error('Failed to verify signature:', error);
      throw error;
    }
  }

  // ========================================
  // Reusable Elements API Methods
  // ========================================

  /**
   * Create a new reusable element
   */
  async createReusableElement(request: ReusableElementRequest): Promise<ReusableElementResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/elements`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to create reusable element: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to create reusable element:', error);
      throw error;
    }
  }

  /**
   * Update a reusable element
   */
  async updateReusableElement(elementId: number, request: Partial<ReusableElementRequest>): Promise<ReusableElementResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/elements/${elementId}`,
        {
          method: 'PUT',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to update reusable element: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to update reusable element:', error);
      throw error;
    }
  }

  /**
   * Get a reusable element by ID
   */
  async getReusableElement(elementId: number): Promise<ReusableElementResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/elements/${elementId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get reusable element: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get reusable element:', error);
      throw error;
    }
  }

  /**
   * Get all reusable elements for the current user
   */
  async getUserReusableElements(): Promise<ReusableElementResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/elements`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get reusable elements: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get reusable elements:', error);
      return [];
    }
  }

  /**
   * Get reusable elements by type
   */
  async getReusableElementsByType(type: string): Promise<ReusableElementResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/elements/type/${type}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get reusable elements by type: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get reusable elements by type:', error);
      return [];
    }
  }

  /**
   * Search reusable elements
   */
  async searchReusableElements(params: { q?: string; type?: string }): Promise<ReusableElementResponse[]> {
    try {
      const queryParams = new URLSearchParams();
      if (params.q) queryParams.append('q', params.q);
      if (params.type) queryParams.append('type', params.type);

      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/elements/search?${queryParams.toString()}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to search reusable elements: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to search reusable elements:', error);
      return [];
    }
  }

  /**
   * Delete a reusable element
   */
  async deleteReusableElement(elementId: number): Promise<boolean> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/elements/${elementId}`,
        {
          method: 'DELETE',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      return response.ok;
    } catch (error) {
      console.error('Failed to delete reusable element:', error);
      return false;
    }
  }

  /**
   * Get recent reusable elements
   */
  async getRecentReusableElements(limit = 10): Promise<ReusableElementResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/elements/recent?limit=${limit}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get recent reusable elements: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get recent reusable elements:', error);
      return [];
    }
  }

  /**
   * Get most used reusable elements
   */
  async getMostUsedReusableElements(limit = 10): Promise<ReusableElementResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/elements/most-used?limit=${limit}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get most used reusable elements: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get most used reusable elements:', error);
      return [];
    }
  }

  /**
   * Get reusable element statistics
   */
  async getReusableElementStatistics(): Promise<Record<string, unknown>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/elements/statistics`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get reusable element statistics: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get reusable element statistics:', error);
      return {};
    }
  }

  /**
   * Increment reusable element use count
   */
  async incrementReusableElementUseCount(elementId: number): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/elements/${elementId}/use`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to increment use count: ${response.statusText}`);
      }
    } catch (error) {
      console.error('Failed to increment use count:', error);
      throw error;
    }
  }

  // ========================================
  // Component Definitions API Methods
  // ========================================

  /**
   * Create a new component definition
   */
  async createComponentDefinition(request: ComponentDefinitionRequest): Promise<ComponentDefinitionResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/components`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        15000
      );

      if (!response.ok) {
        throw new Error(`Failed to create component definition: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to create component definition:', error);
      throw error;
    }
  }

  /**
   * Update a component definition
   */
  async updateComponentDefinition(componentId: number, request: Partial<ComponentDefinitionRequest>): Promise<ComponentDefinitionResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/components/${componentId}`,
        {
          method: 'PUT',
          headers: this.getAuthHeaders(),
          body: JSON.stringify(request),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Failed to update component definition: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to update component definition:', error);
      throw error;
    }
  }

  /**
   * Get a component definition by ID
   */
  async getComponentDefinition(componentId: number): Promise<ComponentDefinitionResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/components/${componentId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get component definition: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get component definition:', error);
      throw error;
    }
  }

  /**
   * Get a component definition by OSCAL UUID
   */
  async getComponentDefinitionByUuid(oscalUuid: string): Promise<ComponentDefinitionResponse> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/components/uuid/${oscalUuid}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get component definition by UUID: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get component definition by UUID:', error);
      throw error;
    }
  }

  /**
   * Get component definition JSON content
   */
  async getComponentDefinitionContent(componentId: number): Promise<string> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/components/${componentId}/content`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get component definition content: ${response.statusText}`);
      }

      const data = await response.json();
      return data.content;
    } catch (error) {
      console.error('Failed to get component definition content:', error);
      throw error;
    }
  }

  /**
   * Get all component definitions for the current user
   */
  async getUserComponentDefinitions(): Promise<ComponentDefinitionResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/components`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get component definitions: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get component definitions:', error);
      return [];
    }
  }

  /**
   * Search component definitions
   */
  async searchComponentDefinitions(searchTerm?: string): Promise<ComponentDefinitionResponse[]> {
    try {
      const queryParams = new URLSearchParams();
      if (searchTerm) queryParams.append('q', searchTerm);

      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/components/search?${queryParams.toString()}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to search component definitions: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to search component definitions:', error);
      return [];
    }
  }

  /**
   * Delete a component definition
   */
  async deleteComponentDefinition(componentId: number): Promise<boolean> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/components/${componentId}`,
        {
          method: 'DELETE',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      return response.ok;
    } catch (error) {
      console.error('Failed to delete component definition:', error);
      return false;
    }
  }

  /**
   * Get recent component definitions
   */
  async getRecentComponentDefinitions(limit = 10): Promise<ComponentDefinitionResponse[]> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/components/recent?limit=${limit}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get recent component definitions: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get recent component definitions:', error);
      return [];
    }
  }

  /**
   * Get component definition statistics
   */
  async getComponentDefinitionStatistics(): Promise<Record<string, unknown>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/components/statistics`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get component definition statistics: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get component definition statistics:', error);
      return {};
    }
  }

  /**
   * Check if component definition exists
   */
  async checkComponentDefinitionExists(componentId: number): Promise<boolean> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/build/components/${componentId}/exists`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        return false;
      }

      const data = await response.json();
      return data.exists;
    } catch (error) {
      console.error('Failed to check component definition existence:', error);
      return false;
    }
  }

  // ========================================
  // Organization Management API Methods
  // ========================================

  /**
   * Get all active organizations (public endpoint for NASCAR selector)
   */
  async getOrganizations(): Promise<Array<{
    organizationId: number;
    name: string;
    description: string | null;
    logoUrl: string | null;
  }>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/organizations`,
        {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' }, // Public endpoint, no auth
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get organizations: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get organizations:', error);
      return [];
    }
  }

  /**
   * Get current user's organizations with membership details
   */
  async getMyOrganizations(): Promise<Array<{
    organizationId: number;
    name: string;
    description: string | null;
    logoUrl: string | null;
    role: string;
    joinedAt: string;
  }>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/my-organizations`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get my organizations: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get my organizations:', error);
      return [];
    }
  }

  /**
   * Get current user's pending access requests
   */
  async getMyPendingRequests(): Promise<Array<{
    requestId: number;
    organizationId: number;
    organizationName: string;
    requestDate: string;
    status: string;
    message: string | null;
  }>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/my-pending-requests`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get pending requests: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get pending requests:', error);
      return [];
    }
  }

  /**
   * Select organization after initial login (generates full JWT with org context)
   */
  async selectOrganization(organizationId: number): Promise<{
    token: string;
    username: string;
    email: string;
    userId: number;
    organizationId: number;
    organizationName: string;
    orgRole: string;
    globalRole: string;
    mustChangePassword: boolean;
  }> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/select-organization/${organizationId}`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to select organization');
      }

      const result = await response.json();

      // Get existing user data to preserve firstName/lastName
      const existingUser = localStorage.getItem('user');
      let firstName = '';
      let lastName = '';
      if (existingUser) {
        try {
          const parsed = JSON.parse(existingUser);
          firstName = parsed.firstName || '';
          lastName = parsed.lastName || '';
        } catch (e) {
          // ignore
        }
      }

      // Update token and user info in localStorage
      localStorage.setItem('token', result.token);
      localStorage.setItem('user', JSON.stringify({
        userId: result.userId,
        username: result.username,
        email: result.email,
        firstName: result.firstName || firstName,
        lastName: result.lastName || lastName,
        organizationId: result.organizationId,
        organizationName: result.organizationName,
        orgRole: result.orgRole,
        globalRole: result.globalRole,
        mustChangePassword: result.mustChangePassword,
      }));

      return result;
    } catch (error) {
      console.error('Failed to select organization:', error);
      throw error;
    }
  }

  /**
   * Switch to a different organization (re-issues JWT)
   */
  async switchOrganization(organizationId: number): Promise<{
    token: string;
    username: string;
    email: string;
    userId: number;
    organizationId: number;
    organizationName: string;
    orgRole: string;
    globalRole: string;
    mustChangePassword: boolean;
  }> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/switch-organization/${organizationId}`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to switch organization');
      }

      const result = await response.json();

      // Get existing user data to preserve firstName/lastName
      const existingUser = localStorage.getItem('user');
      let firstName = '';
      let lastName = '';
      if (existingUser) {
        try {
          const parsed = JSON.parse(existingUser);
          firstName = parsed.firstName || '';
          lastName = parsed.lastName || '';
        } catch (e) {
          // ignore
        }
      }

      // Update token and user info in localStorage
      localStorage.setItem('token', result.token);
      localStorage.setItem('user', JSON.stringify({
        userId: result.userId,
        username: result.username,
        email: result.email,
        firstName: result.firstName || firstName,
        lastName: result.lastName || lastName,
        organizationId: result.organizationId,
        organizationName: result.organizationName,
        orgRole: result.orgRole,
        globalRole: result.globalRole,
        mustChangePassword: result.mustChangePassword,
      }));

      return result;
    } catch (error) {
      console.error('Failed to switch organization:', error);
      throw error;
    }
  }

  /**
   * Request access to an organization (public endpoint)
   */
  async requestAccess(request: {
    organizationId: number;
    firstName: string;
    lastName: string;
    email: string;
    username?: string;
    message?: string;
  }): Promise<{ message: string }> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/request-access`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' }, // Public endpoint, no auth
          body: JSON.stringify(request),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to request access');
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to request access:', error);
      throw error;
    }
  }

  /**
   * Change user password
   */
  async changePassword(oldPassword: string, newPassword: string): Promise<{ message: string }> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/auth/change-password`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify({ oldPassword, newPassword }),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to change password');
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to change password:', error);
      throw error;
    }
  }

  // ========================================
  // Super Admin API Methods
  // ========================================

  /**
   * Get organizations summary with member counts and pending request counts
   * Super Admin only
   */
  async getOrganizationsSummary(): Promise<Array<{
    id: number;
    name: string;
    memberCount: number;
    pendingRequestCount: number;
  }>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/organizations/summary`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get organizations summary: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get organizations summary:', error);
      throw error;
    }
  }

  /**
   * Get all pending access requests across all organizations
   * Super Admin only
   */
  async getAllPendingAccessRequests(): Promise<Array<{
    id: number;
    userId: number | null;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    organizationId: number;
    organizationName: string;
    status: string;
    message: string | null;
    requestDate: string;
    reviewedBy: number | null;
    reviewedByUsername: string | null;
    reviewedDate: string | null;
    notes: string | null;
  }>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/access-requests`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get pending access requests: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get pending access requests:', error);
      throw error;
    }
  }

  /**
   * Approve an access request
   * Super Admin only
   */
  async approveAccessRequest(requestId: number, notes?: string): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/access-requests/${requestId}/approve`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify({ notes }),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to approve access request');
      }
    } catch (error) {
      console.error('Failed to approve access request:', error);
      throw error;
    }
  }

  /**
   * Reject an access request
   * Super Admin only
   */
  async rejectAccessRequest(requestId: number, notes?: string): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/access-requests/${requestId}/reject`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify({ notes }),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to reject access request');
      }
    } catch (error) {
      console.error('Failed to reject access request:', error);
      throw error;
    }
  }

  // ========================================
  // Analytics API Methods
  // ========================================

  /**
   * Get comprehensive analytics data for the super admin dashboard
   * Super Admin only
   */
  async getAnalytics(): Promise<any> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/analytics`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Failed to get analytics: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get analytics:', error);
      throw error;
    }
  }

  /**
   * Get summary statistics for quick dashboard header cards
   * Super Admin only
   */
  async getAnalyticsSummary(): Promise<any> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/analytics/summary`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get analytics summary: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get analytics summary:', error);
      throw error;
    }
  }

  // ========================================
  // Admin Audit Logs API Methods
  // ========================================

  /**
   * Get all audit logs with pagination and optional filters
   * Super Admin only
   */
  async getAuditLogs(params: {
    page?: number;
    size?: number;
    username?: string;
    ipAddress?: string;
    riskLevel?: string;
    eventType?: string;
    startDate?: string;
    endDate?: string;
  } = {}): Promise<{
    content: AuditLog[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
  }> {
    try {
      const queryParams = new URLSearchParams();
      queryParams.append('page', String(params.page || 0));
      queryParams.append('size', String(params.size || 50));
      if (params.username) queryParams.append('username', params.username);
      if (params.ipAddress) queryParams.append('ipAddress', params.ipAddress);
      if (params.riskLevel) queryParams.append('riskLevel', params.riskLevel);
      if (params.eventType) queryParams.append('eventType', params.eventType);
      if (params.startDate) queryParams.append('startDate', params.startDate);
      if (params.endDate) queryParams.append('endDate', params.endDate);

      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/logs?${queryParams.toString()}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Failed to get audit logs: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get audit logs:', error);
      return { content: [], totalPages: 0, totalElements: 0, size: 50, number: 0 };
    }
  }

  /**
   * Get raw access logs (API requests)
   * Super Admin only
   */
  async getRawLogs(page = 0, size = 50, filters?: { username?: string; riskLevel?: string }): Promise<{
    content: AuditLog[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
  }> {
    try {
      const queryParams = new URLSearchParams();
      queryParams.append('page', page.toString());
      queryParams.append('size', size.toString());
      if (filters?.username) queryParams.append('username', filters.username);
      if (filters?.riskLevel) queryParams.append('riskLevel', filters.riskLevel);

      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/logs/raw?${queryParams.toString()}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Failed to get raw logs: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get raw logs:', error);
      return { content: [], totalPages: 0, totalElements: 0, size, number: page };
    }
  }

  /**
   * Get security logs (security events and high-risk events)
   * Super Admin only
   */
  async getSecurityLogs(page = 0, size = 50, filters?: { username?: string; riskLevel?: string }): Promise<{
    content: AuditLog[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
  }> {
    try {
      const queryParams = new URLSearchParams();
      queryParams.append('page', page.toString());
      queryParams.append('size', size.toString());
      if (filters?.username) queryParams.append('username', filters.username);
      if (filters?.riskLevel) queryParams.append('riskLevel', filters.riskLevel);

      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/logs/security?${queryParams.toString()}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Failed to get security logs: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get security logs:', error);
      return { content: [], totalPages: 0, totalElements: 0, size, number: page };
    }
  }

  /**
   * Get error logs (failed and error events)
   * Super Admin only
   */
  async getErrorLogs(page = 0, size = 50, filters?: { username?: string; riskLevel?: string }): Promise<{
    content: AuditLog[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
  }> {
    try {
      const queryParams = new URLSearchParams();
      queryParams.append('page', page.toString());
      queryParams.append('size', size.toString());
      if (filters?.username) queryParams.append('username', filters.username);
      if (filters?.riskLevel) queryParams.append('riskLevel', filters.riskLevel);

      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/logs/errors?${queryParams.toString()}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Failed to get error logs: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get error logs:', error);
      return { content: [], totalPages: 0, totalElements: 0, size, number: page };
    }
  }

  /**
   * Search audit logs by keyword
   * Super Admin only
   */
  async searchAuditLogs(query: string, page = 0, size = 50): Promise<{
    content: AuditLog[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
  }> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/logs/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Failed to search audit logs: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to search audit logs:', error);
      return { content: [], totalPages: 0, totalElements: 0, size, number: page };
    }
  }

  /**
   * Get a single audit log by ID
   * Super Admin only
   */
  async getAuditLogById(id: number): Promise<AuditLog | null> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/logs/${id}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        if (response.status === 404) return null;
        throw new Error(`Failed to get audit log: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get audit log:', error);
      return null;
    }
  }

  /**
   * Get audit log statistics
   * Super Admin only
   */
  async getAuditLogStats(): Promise<AuditLogStats> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/admin/logs/stats`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get audit log stats: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get audit log stats:', error);
      return {
        totalLogs: 0,
        logsToday: 0,
        securityEventsToday: 0,
        errorsToday: 0,
        highRiskUnreviewed: 0,
        byCategory: {},
        byRiskLevel: {},
        byOutcome: {},
      };
    }
  }

  /**
   * Export audit logs to CSV with authentication
   * Downloads the file through an authenticated request
   * Super Admin only
   */
  async exportLogsCsv(params: {
    username?: string;
    riskLevel?: string;
    startDate?: string;
    endDate?: string;
  } = {}): Promise<void> {
    const queryParams = new URLSearchParams();
    if (params.username) queryParams.append('username', params.username);
    if (params.riskLevel) queryParams.append('riskLevel', params.riskLevel);
    if (params.startDate) queryParams.append('startDate', params.startDate);
    if (params.endDate) queryParams.append('endDate', params.endDate);

    const query = queryParams.toString();
    const url = `${API_BASE_URL}/admin/logs/export/csv${query ? '?' + query : ''}`;

    const response = await this.fetchWithTimeout(url, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Export failed: ${response.statusText}`);
    }

    // Get the blob and trigger download
    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = `audit-logs-${new Date().toISOString().split('T')[0]}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);
  }

  /**
   * Export audit logs to JSON Lines with authentication
   * Downloads the file through an authenticated request
   * Super Admin only
   */
  async exportLogsJson(params: {
    username?: string;
    riskLevel?: string;
    startDate?: string;
    endDate?: string;
  } = {}): Promise<void> {
    const queryParams = new URLSearchParams();
    if (params.username) queryParams.append('username', params.username);
    if (params.riskLevel) queryParams.append('riskLevel', params.riskLevel);
    if (params.startDate) queryParams.append('startDate', params.startDate);
    if (params.endDate) queryParams.append('endDate', params.endDate);

    const query = queryParams.toString();
    const url = `${API_BASE_URL}/admin/logs/export/json${query ? '?' + query : ''}`;

    const response = await this.fetchWithTimeout(url, {
      method: 'GET',
      headers: this.getAuthHeaders(),
    });

    if (!response.ok) {
      throw new Error(`Export failed: ${response.statusText}`);
    }

    // Get the blob and trigger download
    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = `audit-logs-${new Date().toISOString().split('T')[0]}.jsonl`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);
  }

  // ========== Health Check Methods ==========

  /**
   * Get simple health status (public endpoint - no auth required).
   * Used for basic monitoring and load balancers.
   */
  async getSimpleHealth(): Promise<SimpleHealthResponse> {
    try {
      const response = await fetch(`${API_BASE_URL}/health`, {
        method: 'GET',
      });

      if (!response.ok) {
        return {
          status: 'DOWN',
          timestamp: new Date().toISOString(),
          version: 'unknown',
        };
      }

      return response.json();
    } catch (error) {
      console.error('Health check failed:', error);
      return {
        status: 'DOWN',
        timestamp: new Date().toISOString(),
        version: 'unknown',
      };
    }
  }

  /**
   * Get detailed health status (requires SUPER_ADMIN auth).
   * Returns comprehensive health information for admin dashboard.
   */
  async getDetailedHealth(): Promise<DetailedHealthResponse> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/health/detailed`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Health check failed: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get health status for a specific component (requires SUPER_ADMIN auth).
   * @param component - Component name: database, storage, memory, diskspace, oscal
   */
  async getComponentHealth(component: string): Promise<ComponentHealth> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/health/component/${encodeURIComponent(component)}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Component health check failed: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Simple ping check (public endpoint - no auth required).
   * Returns true if healthy, false if unhealthy.
   */
  async ping(): Promise<boolean> {
    try {
      const response = await fetch(`${API_BASE_URL}/health/ping`, {
        method: 'GET',
      });

      return response.ok;
    } catch (error) {
      console.error('Ping failed:', error);
      return false;
    }
  }

  // ========== Security Compliance Methods ==========

  /**
   * Get SOC 2 compliance summary (requires SUPER_ADMIN auth).
   * Returns overall compliance statistics.
   */
  async getComplianceSummary(): Promise<ComplianceSummary> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/admin/security/compliance-summary`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Failed to get compliance summary: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get all SOC 2 controls (requires SUPER_ADMIN auth).
   * Returns all controls with their implementation status.
   */
  async getAllControls(): Promise<Soc2Control[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/admin/security/controls`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Failed to get controls: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get SOC 2 controls by category (requires SUPER_ADMIN auth).
   * @param category - Category code: CC6, CC7, CC8, CC9, DATA, AUDIT
   */
  async getControlsByCategory(category: string): Promise<Soc2Control[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/admin/security/controls/${encodeURIComponent(category)}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Failed to get controls for category ${category}: ${response.status} ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get SOC 2 gap analysis (requires SUPER_ADMIN auth).
   * Returns identified compliance gaps with recommendations.
   */
  async getGapAnalysis(): Promise<GapAnalysis[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/admin/security/gaps`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Failed to get gap analysis: ${response.status} ${response.statusText}`);
    }

    return response.json();
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

  // ========================================
  // Security Policy API Methods
  // ========================================

  /**
   * Get current security policy (Super Admin only)
   */
  async getSecurityPolicy(): Promise<SecurityPolicy> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/admin/security-policy`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || error.message || 'Failed to fetch security policy');
    }

    return response.json();
  }

  /**
   * Update security policy (Super Admin only)
   */
  async updateSecurityPolicy(request: SecurityPolicyUpdateRequest): Promise<SecurityPolicy> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/admin/security-policy`,
      {
        method: 'PUT',
        headers: this.getAuthHeaders(),
        body: JSON.stringify(request),
      },
      10000
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || error.message || 'Failed to update security policy');
    }

    return response.json();
  }

  /**
   * Trigger manual audit log cleanup (Super Admin only)
   */
  async triggerAuditLogCleanup(): Promise<{ message: string; deletedCount: number; retentionDays: number }> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/admin/security-policy/cleanup-logs`,
      {
        method: 'POST',
        headers: this.getAuthHeaders(),
      },
      60000 // 60 seconds for cleanup operation
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || error.message || 'Failed to trigger audit log cleanup');
    }

    return response.json();
  }

  // ========================================
  // MFA API Methods
  // ========================================

  /**
   * Initiate MFA setup - generates QR code and secret
   * @param mfaSetupToken Optional MFA setup token for users coming from login flow
   */
  async initiateMfaSetup(mfaSetupToken?: string): Promise<MfaSetupResponse> {
    // Use the MFA setup token if provided (from login flow), otherwise use regular auth
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    if (mfaSetupToken) {
      headers['Authorization'] = `Bearer ${mfaSetupToken}`;
    } else {
      const token = localStorage.getItem('token');
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
    }

    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/auth/mfa/setup/initiate`,
      {
        method: 'POST',
        headers,
      },
      10000
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || error.message || 'Failed to initiate MFA setup');
    }

    return response.json();
  }

  /**
   * Complete MFA setup by verifying the first TOTP code
   */
  async completeMfaSetup(request: MfaSetupCompleteRequest): Promise<MfaSetupCompleteResponse> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/auth/mfa/setup/complete`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      },
      10000
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || error.message || 'Failed to complete MFA setup');
    }

    return response.json();
  }

  /**
   * Verify TOTP code during login
   */
  async verifyMfaCode(request: MfaVerifyRequest): Promise<AuthResponse> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/auth/mfa/verify`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      },
      10000
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || error.message || 'MFA verification failed');
    }

    const authResponse: AuthResponse = await response.json();

    // Store token after successful MFA verification
    if (authResponse.token) {
      localStorage.setItem('token', authResponse.token ?? '');
      localStorage.setItem('user', JSON.stringify({
        userId: authResponse.userId,
        username: authResponse.username,
        email: authResponse.email,
        globalRole: authResponse.globalRole,
      }));
    }

    return authResponse;
  }

  /**
   * Verify backup code during login
   */
  async verifyBackupCode(request: MfaBackupCodeRequest): Promise<AuthResponse & { backupCodesRemaining?: number; warning?: string }> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/auth/mfa/verify-backup`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      },
      10000
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || error.message || 'Backup code verification failed');
    }

    const authResponse = await response.json();

    // Store token after successful backup code verification
    if (authResponse.token) {
      localStorage.setItem('token', authResponse.token ?? '');
      localStorage.setItem('user', JSON.stringify({
        userId: authResponse.user?.id || authResponse.userId,
        username: authResponse.user?.username || authResponse.username,
        email: authResponse.user?.email || authResponse.email,
        globalRole: authResponse.user?.globalRole || authResponse.globalRole,
      }));
    }

    return authResponse;
  }

  /**
   * Get MFA status for current user
   */
  async getMfaStatus(): Promise<MfaStatus> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/auth/mfa/status`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || error.message || 'Failed to get MFA status');
    }

    return response.json();
  }

  /**
   * Get backup codes count
   */
  async getBackupCodesCount(): Promise<{ count: number }> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/auth/mfa/backup-codes/count`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || error.message || 'Failed to get backup codes count');
    }

    return response.json();
  }

  /**
   * Regenerate backup codes (requires TOTP verification)
   */
  async regenerateBackupCodes(totpCode: string): Promise<{ backupCodes: string[] }> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/auth/mfa/backup-codes/regenerate?totpCode=${encodeURIComponent(totpCode)}`,
      {
        method: 'POST',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || error.message || 'Failed to regenerate backup codes');
    }

    return response.json();
  }

  /**
   * Disable MFA for current user (requires TOTP verification)
   */
  async disableMfa(totpCode: string): Promise<{ message: string }> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/auth/mfa/disable?totpCode=${encodeURIComponent(totpCode)}`,
      {
        method: 'DELETE',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || error.message || 'Failed to disable MFA');
    }

    return response.json();
  }

  /**
   * Admin: Disable MFA for another user (Super Admin only)
   */
  async adminDisableMfa(userId: number): Promise<{ message: string }> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/auth/mfa/admin/users/${userId}/mfa`,
      {
        method: 'DELETE',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || error.message || 'Failed to disable MFA for user');
    }

    return response.json();
  }

  // ========================================
  // Artifact API Methods
  // ========================================

  /**
   * Create a new artifact
   */
  async createArtifact(request: ArtifactRequest): Promise<Artifact> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts`,
      {
        method: 'POST',
        headers: this.getAuthHeaders(),
        body: JSON.stringify(request),
      },
      10000
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Failed to create artifact');
    }

    return response.json();
  }

  /**
   * Get artifact by ID
   */
  async getArtifact(artifactId: string): Promise<Artifact> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get artifact: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Update artifact metadata
   */
  async updateArtifact(artifactId: string, request: ArtifactUpdateRequest): Promise<Artifact> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}`,
      {
        method: 'PUT',
        headers: this.getAuthHeaders(),
        body: JSON.stringify(request),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to update artifact: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Delete an artifact
   */
  async deleteArtifact(artifactId: string): Promise<void> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}`,
      {
        method: 'DELETE',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to delete artifact: ${response.statusText}`);
    }
  }

  /**
   * Get artifact content (current version)
   */
  async getArtifactContent(artifactId: string): Promise<string> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}/content`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Failed to get artifact content: ${response.statusText}`);
    }

    // Backend returns JSON { content: "..." }
    const data = await response.json();
    return data.content || '';
  }

  /**
   * Add new version to artifact
   */
  async addArtifactVersion(artifactId: string, request: ArtifactVersionRequest): Promise<Artifact> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}/versions`,
      {
        method: 'POST',
        headers: this.getAuthHeaders(),
        body: JSON.stringify(request),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Failed to add artifact version: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get artifact version history
   */
  async getArtifactVersionHistory(artifactId: string): Promise<ArtifactVersion[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}/versions`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get artifact version history: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get specific artifact version content
   */
  async getArtifactVersionContent(versionId: string): Promise<string> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/versions/${versionId}/content`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Failed to get artifact version content: ${response.statusText}`);
    }

    // Backend returns JSON { content: "..." }
    const data = await response.json();
    return data.content || '';
  }

  /**
   * Get all artifacts visible to current user
   */
  async getAllArtifacts(): Promise<Artifact[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Failed to get artifacts: ${response.statusText}`);
    }

    const data = await response.json();
    // Backend returns paginated response with content array
    return data.content || [];
  }

  /**
   * Get current user's own artifacts
   */
  async getMyArtifacts(): Promise<Artifact[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/my`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Failed to get my artifacts: ${response.statusText}`);
    }

    const data = await response.json();
    // Backend returns paginated response with content array
    return data.content || [];
  }

  /**
   * Get public artifacts only
   */
  async getPublicArtifacts(): Promise<Artifact[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/public`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Failed to get public artifacts: ${response.statusText}`);
    }

    const data = await response.json();
    // Backend returns paginated response with content array
    return data.content || [];
  }

  /**
   * Get artifacts for a specific organization
   */
  async getOrganizationArtifacts(organizationId: number): Promise<Artifact[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/organization/${organizationId}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Failed to get organization artifacts: ${response.statusText}`);
    }

    const data = await response.json();
    // Backend returns paginated response with content array
    return data.content || [];
  }

  /**
   * Search artifacts
   */
  async searchArtifacts(params: {
    keyword?: string;
    tag?: string;
    visibility?: ArtifactVisibility;
    organizationId?: number;
  }): Promise<Artifact[]> {
    const queryParams = new URLSearchParams();
    // Backend uses 'q' for keyword search
    if (params.keyword) queryParams.append('q', params.keyword);
    if (params.tag) queryParams.append('tag', params.tag);
    if (params.visibility) queryParams.append('visibility', params.visibility);
    if (params.organizationId) queryParams.append('organizationId', params.organizationId.toString());

    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/search?${queryParams.toString()}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      10000
    );

    if (!response.ok) {
      throw new Error(`Failed to search artifacts: ${response.statusText}`);
    }

    const data = await response.json();
    // Backend returns paginated response with content array
    return data.content || [];
  }

  /**
   * Get most popular (downloaded) artifacts
   */
  async getMostPopularArtifacts(limit = 10): Promise<Artifact[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/popular?limit=${limit}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get popular artifacts: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get recently updated artifacts
   */
  async getRecentArtifacts(limit = 10): Promise<Artifact[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/recent?limit=${limit}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get recent artifacts: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get artifact analytics
   */
  async getArtifactAnalytics(): Promise<ArtifactAnalytics> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/analytics`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get artifact analytics: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get all artifact tags
   */
  async getAllArtifactTags(): Promise<ArtifactTag[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/tags`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get artifact tags: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get popular artifact tags
   */
  async getPopularArtifactTags(limit = 20): Promise<ArtifactTag[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/tags/popular?limit=${limit}`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get popular artifact tags: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Rate an artifact
   */
  async rateArtifact(artifactId: string, rating: number): Promise<RatingStats> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}/ratings`,
      {
        method: 'POST',
        headers: this.getAuthHeaders(),
        body: JSON.stringify({ rating }),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to rate artifact: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get artifact rating stats
   */
  async getArtifactRatings(artifactId: string): Promise<RatingStats> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}/ratings`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get artifact ratings: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Remove user's rating from artifact
   */
  async deleteArtifactRating(artifactId: string): Promise<void> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}/ratings`,
      {
        method: 'DELETE',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to delete artifact rating: ${response.statusText}`);
    }
  }

  /**
   * Create a comment on an artifact
   */
  async createArtifactComment(
    artifactId: string,
    content: string,
    parentCommentId?: string
  ): Promise<ArtifactComment> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}/comments`,
      {
        method: 'POST',
        headers: this.getAuthHeaders(),
        body: JSON.stringify({ content, parentCommentId }),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to create artifact comment: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get comments for an artifact
   */
  async getArtifactComments(artifactId: string): Promise<ArtifactComment[]> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}/comments`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get artifact comments: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Get comment count for an artifact
   */
  async getArtifactCommentCount(artifactId: string): Promise<number> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}/comments/count`,
      {
        method: 'GET',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to get artifact comment count: ${response.statusText}`);
    }

    const data = await response.json();
    return data.count;
  }

  /**
   * Update an artifact comment
   */
  async updateArtifactComment(
    artifactId: string,
    commentId: string,
    content: string
  ): Promise<ArtifactComment> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}/comments/${commentId}`,
      {
        method: 'PUT',
        headers: this.getAuthHeaders(),
        body: JSON.stringify({ content }),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to update artifact comment: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Delete an artifact comment (soft delete)
   */
  async deleteArtifactComment(artifactId: string, commentId: string): Promise<void> {
    const response = await this.fetchWithTimeout(
      `${API_BASE_URL}/artifacts/${artifactId}/comments/${commentId}`,
      {
        method: 'DELETE',
        headers: this.getAuthHeaders(),
      },
      5000
    );

    if (!response.ok) {
      throw new Error(`Failed to delete artifact comment: ${response.statusText}`);
    }
  }

  // ========================================
  // Org Admin API Methods
  // ========================================

  async getOrgUsers(organizationId: number): Promise<Array<{
    userId: number;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
    status: string;
    joinedAt: string;
    updatedAt: string;
  }>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/users?organizationId=${organizationId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to get organization users');
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get organization users:', error);
      throw error;
    }
  }

  async lockOrgUser(organizationId: number, userId: number): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/users/${userId}/lock?organizationId=${organizationId}`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to lock user');
      }
    } catch (error) {
      console.error('Failed to lock user:', error);
      throw error;
    }
  }

  async unlockOrgUser(organizationId: number, userId: number): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/users/${userId}/unlock?organizationId=${organizationId}`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to unlock user');
      }
    } catch (error) {
      console.error('Failed to unlock user:', error);
      throw error;
    }
  }

  async deactivateOrgUser(organizationId: number, userId: number): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/users/${userId}/deactivate?organizationId=${organizationId}`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to deactivate user');
      }
    } catch (error) {
      console.error('Failed to deactivate user:', error);
      throw error;
    }
  }

  async reactivateOrgUser(organizationId: number, userId: number): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/users/${userId}/reactivate?organizationId=${organizationId}`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to reactivate user');
      }
    } catch (error) {
      console.error('Failed to reactivate user:', error);
      throw error;
    }
  }

  async resetOrgUserPassword(organizationId: number, userId: number): Promise<{ tempPassword: string; username: string; email: string }> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/users/${userId}/reset-password?organizationId=${organizationId}`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to reset password');
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to reset password:', error);
      throw error;
    }
  }

  async getOrgPendingRequests(organizationId: number): Promise<Array<{
    id: number;
    userId: number | null;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    organizationId: number;
    organizationName: string;
    status: string;
    message: string | null;
    requestDate: string;
    reviewedBy: number | null;
    reviewedByUsername: string | null;
    reviewedDate: string | null;
    notes: string | null;
  }>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/access-requests?organizationId=${organizationId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get pending requests: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get pending requests:', error);
      throw error;
    }
  }

  async getOrgAllRequests(organizationId: number): Promise<Array<{
    id: number;
    userId: number | null;
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    organizationId: number;
    organizationName: string;
    status: string;
    message: string | null;
    requestDate: string;
    reviewedBy: number | null;
    reviewedByUsername: string | null;
    reviewedDate: string | null;
    notes: string | null;
  }>> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/access-requests/all?organizationId=${organizationId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get access requests: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get access requests:', error);
      throw error;
    }
  }

  async approveOrgRequest(requestId: number, notes?: string): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/access-requests/${requestId}/approve`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify({ notes }),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to approve access request');
      }
    } catch (error) {
      console.error('Failed to approve access request:', error);
      throw error;
    }
  }

  async rejectOrgRequest(requestId: number, notes?: string): Promise<void> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/access-requests/${requestId}/reject`,
        {
          method: 'POST',
          headers: this.getAuthHeaders(),
          body: JSON.stringify({ notes }),
        },
        5000
      );

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to reject access request');
      }
    } catch (error) {
      console.error('Failed to reject access request:', error);
      throw error;
    }
  }

  async getOrgAnalytics(organizationId: number): Promise<{
    activeUsersLast7Days: number;
    totalLoginsLast30Days: number;
    totalOperationsLast30Days: number;
    failedLoginsLast30Days: number;
    loginsPerDay: Array<{ date: string; count: number }>;
    operationsByType: Array<{ name: string; count: number }>;
    topUsers: Array<{ username: string; operationCount: number }>;
  }> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/analytics?organizationId=${organizationId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        10000
      );

      if (!response.ok) {
        throw new Error(`Failed to get org analytics: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get org analytics:', error);
      throw error;
    }
  }

  async getOrgAnalyticsSummary(organizationId: number): Promise<{
    totalMembers: number;
    pendingRequests: number;
    loginsThisMonth: number;
    operationsThisMonth: number;
  }> {
    try {
      const response = await this.fetchWithTimeout(
        `${API_BASE_URL}/org-admin/analytics/summary?organizationId=${organizationId}`,
        {
          method: 'GET',
          headers: this.getAuthHeaders(),
        },
        5000
      );

      if (!response.ok) {
        throw new Error(`Failed to get org analytics summary: ${response.statusText}`);
      }

      return await response.json();
    } catch (error) {
      console.error('Failed to get org analytics summary:', error);
      throw error;
    }
  }

  // ========================================
  // Mock Implementations (for development)
  // ========================================

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
