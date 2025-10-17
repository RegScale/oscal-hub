// OSCAL Type Definitions for TypeScript

export type OscalFormat = 'xml' | 'json' | 'yaml';

export type OscalModelType =
  | 'catalog'
  | 'profile'
  | 'component-definition'
  | 'system-security-plan'
  | 'assessment-plan'
  | 'assessment-results'
  | 'plan-of-action-and-milestones';

export interface ValidationError {
  line?: number;
  column?: number;
  message: string;
  severity: 'error' | 'warning' | 'info';
  path?: string;
}

export interface ValidationResult {
  valid: boolean;
  errors: ValidationError[];
  warnings: ValidationError[];
  modelType?: OscalModelType;
  format?: OscalFormat;
  timestamp: string;
}

export interface ConversionRequest {
  content: string;
  fromFormat: OscalFormat;
  toFormat: OscalFormat;
  modelType: OscalModelType;
  fileName?: string;
}

export interface ConversionResult {
  success: boolean;
  content?: string;
  error?: string;
  fromFormat: OscalFormat;
  toFormat: OscalFormat;
}

export interface ProfileResolutionRequest {
  profileContent: string;
  format: OscalFormat;
}

export interface ProfileResolutionResult {
  success: boolean;
  resolvedCatalog?: string;
  error?: string;
  controlCount?: number;
}

export interface BatchOperation {
  id: string;
  type: 'validate' | 'convert' | 'resolve';
  fileName: string;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  progress: number;
  result?: ValidationResult | ConversionResult | ProfileResolutionResult;
  error?: string;
  startTime?: string;
  endTime?: string;
}

// Operation History Types (matches backend OperationHistory entity)
export interface OperationHistory {
  id: number;
  operationType: string; // VALIDATE, CONVERT, RESOLVE, BATCH_VALIDATE, BATCH_CONVERT
  fileName: string;
  timestamp: string; // ISO 8601 date string
  success: boolean;
  details?: string;
  durationMs?: number;
  modelType?: string;
  format?: string;
  fileCount?: number; // For batch operations
  batchOperationId?: string; // Links multiple files in a batch
}

export interface OperationStats {
  totalOperations: number;
  successfulOperations: number;
  failedOperations: number;
  validateCount: number;
  convertCount: number;
  resolveCount: number;
  batchCount: number;
  successRate: number;
}

export interface OperationHistoryPage {
  content: OperationHistory[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number; // Current page number
}

// Batch Operations Types
export type BatchOperationType = 'VALIDATE' | 'CONVERT';

export interface BatchFileContent {
  filename: string;
  content: string;
  format: OscalFormat;
}

export interface BatchOperationRequest {
  operationType: BatchOperationType;
  modelType: OscalModelType;
  files: BatchFileContent[];
  fromFormat?: OscalFormat; // For CONVERT operations
  toFormat?: OscalFormat; // For CONVERT operations
}

export interface BatchFileResult {
  filename: string;
  success: boolean;
  error?: string;
  result?: ValidationResult | ConversionResult;
  durationMs: number;
}

export interface BatchOperationResult {
  success: boolean;
  operationId: string;
  totalFiles: number;
  successCount: number;
  failureCount: number;
  results?: BatchFileResult[];
  totalDurationMs: number;
}

// Saved File Types
export interface SavedFile {
  id: string;
  fileName: string;
  modelType?: OscalModelType;
  format: OscalFormat;
  fileSize: number;
  uploadedAt: string; // ISO 8601 date string
  filePath: string;
}

// Validation Rules Types
export type ValidationRuleType =
  | 'required-field'
  | 'pattern-match'
  | 'allowed-values'
  | 'cardinality'
  | 'cross-field'
  | 'id-reference'
  | 'data-type'
  | 'custom';

export type ValidationRuleSeverity = 'error' | 'warning' | 'info';

export interface ValidationRule {
  id: string;
  name: string;
  description: string;
  ruleType: ValidationRuleType;
  severity: ValidationRuleSeverity;
  applicableModelTypes: OscalModelType[];
  category: string;
  fieldPath?: string;
  constraintDetails?: string;
  builtIn: boolean;
}

export interface ValidationRuleCategory {
  id: string;
  name: string;
  description: string;
  rules: ValidationRule[];
  ruleCount: number;
}

export interface ValidationRulesStats {
  totalRules: number;
  builtInRules: number;
  customRules: number;
  rulesByModelType: Record<string, number>;
  rulesByCategory: Record<string, number>;
}

export interface ValidationRulesResponse {
  totalRules: number;
  builtInRules: number;
  customRules: number;
  rulesByModelType: Record<string, number>;
  rulesByCategory: Record<string, number>;
  categories: ValidationRuleCategory[];
  rules: ValidationRule[];
}

// Custom Rule Types
export interface CustomRuleRequest {
  ruleId: string;
  name: string;
  description?: string;
  ruleType: string;
  severity: string;
  category?: string;
  fieldPath?: string;
  ruleExpression?: string;
  constraintDetails?: string;
  applicableModelTypes?: string[];
  enabled: boolean;
}

export interface CustomRuleResponse {
  id: number;
  ruleId: string;
  name: string;
  description?: string;
  ruleType: string;
  severity: string;
  category?: string;
  fieldPath?: string;
  ruleExpression?: string;
  constraintDetails?: string;
  applicableModelTypes: string[];
  enabled: boolean;
  createdDate: string; // ISO 8601 date string
  updatedDate: string; // ISO 8601 date string
  createdBy?: string;
}
