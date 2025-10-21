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

// Library Types
export interface LibraryVersion {
  versionId: string;
  versionNumber: number;
  fileName: string;
  format: string;
  fileSize: number;
  uploadedBy: string;
  uploadedAt: string; // ISO 8601 date string
  changeDescription?: string;
}

export interface LibraryItem {
  itemId: string;
  title: string;
  description?: string;
  oscalType: string;
  createdBy: string;
  createdAt: string; // ISO 8601 date string
  updatedAt: string; // ISO 8601 date string
  tags: string[];
  currentVersion?: LibraryVersion;
  downloadCount: number;
  viewCount: number;
  versionCount: number;
}

export interface LibraryItemRequest {
  title: string;
  description?: string;
  oscalType: string;
  fileName: string;
  format: string;
  fileContent: string;
  tags?: string[];
}

export interface LibraryItemUpdateRequest {
  title?: string;
  description?: string;
  tags?: string[];
}

export interface LibraryVersionRequest {
  fileName: string;
  format: string;
  fileContent: string;
  changeDescription?: string;
}

export interface LibraryTag {
  name: string;
  usageCount: number;
}

export interface LibraryAnalytics {
  totalItems: number;
  totalVersions: number;
  totalTags: number;
  itemsByType: Record<string, number>;
  popularTags: Array<{
    name: string;
    count: number;
  }>;
  mostDownloaded: Array<{
    itemId: string;
    title: string;
    downloadCount: number;
  }>;
}

// Service Account Token Types
export interface ServiceAccountTokenRequest {
  tokenName: string;
  expirationDays: number;
}

export interface ServiceAccountTokenResponse {
  token: string;
  tokenName: string;
  username: string;
  expiresAt: string; // ISO 8601 date string
  expirationDays: number;
}

// SSP Visualization Types
export interface SspVisualizationRequest {
  content: string;
  format: OscalFormat;
  fileName?: string;
}

export interface SystemInfo {
  uuid: string;
  name: string;
  shortName: string;
  description: string;
  status: string;
  systemIds: Array<{
    identifierType: string;
    id: string;
  }>;
}

export interface SecurityCategorization {
  confidentiality: string;
  integrity: string;
  availability: string;
  overall: string;
}

export interface InformationType {
  uuid: string;
  title: string;
  description: string;
  categorizations: string[];
  confidentiality: {
    base: string;
    selected: string;
  };
  integrity: {
    base: string;
    selected: string;
  };
  availability: {
    base: string;
    selected: string;
  };
}

export interface PersonnelRole {
  roleId: string;
  roleTitle: string;
  roleShortName: string;
  assignedPersonnel: Array<{
    uuid: string;
    name: string;
    jobTitle: string;
    type: string;
  }>;
}

export interface ControlFamilyStatus {
  familyId: string;
  familyName: string;
  totalControls: number;
  statusCounts: Record<string, number>;
  controls: Array<{
    controlId: string;
    implementationStatus: string;
    controlOrigination: string;
  }>;
}

export interface Asset {
  uuid: string;
  description: string;
  assetType: string;
  function: string;
  fqdn: string;
  ipv4Address: string;
  ipv6Address: string;
  macAddress: string;
  virtual: boolean;
  publicAccess: boolean;
  softwareName: string;
  softwareVersion: string;
  vendorName: string;
  scanned: boolean;
}

export interface SspVisualizationData {
  success: boolean;
  message: string;
  timestamp: string;
  systemInfo: SystemInfo;
  categorization: SecurityCategorization;
  informationTypes: InformationType[];
  personnel: PersonnelRole[];
  controlsByFamily: Record<string, ControlFamilyStatus>;
  assets: Asset[];
}

// Profile Visualization Types
export interface ProfileVisualizationRequest {
  content: string;
  format: OscalFormat;
  fileName?: string;
}

export interface ProfileInfo {
  uuid: string;
  title: string;
  version: string;
  oscalVersion: string;
  lastModified: string;
  published: string;
}

export interface ImportInfo {
  href: string;
  includeAllIds: string[];
  excludeIds: string[];
  estimatedControlCount: number;
}

export interface ControlSummary {
  totalIncludedControls: number;
  totalExcludedControls: number;
  totalModifications: number;
  uniqueFamilies: number;
}

export interface ControlFamilyInfo {
  familyId: string;
  familyName: string;
  includedCount: number;
  excludedCount: number;
  includedControls: string[];
  excludedControls: string[];
}

export interface ModificationSummary {
  totalSetsParameters: number;
  totalAlters: number;
  modifiedControlIds: string[];
}

export interface ProfileVisualizationData {
  success: boolean;
  message: string;
  timestamp: string;
  profileInfo: ProfileInfo;
  imports: ImportInfo[];
  controlSummary: ControlSummary;
  controlsByFamily: Record<string, ControlFamilyInfo>;
  modificationSummary: ModificationSummary;
}

// SAR (Security Assessment Results) Visualization Types
export interface SarVisualizationRequest {
  content: string;
  format: OscalFormat;
  fileName?: string;
}

export interface AssessmentInfo {
  uuid: string;
  title: string;
  description: string;
  version: string;
  oscalVersion: string;
  published: string;
  lastModified: string;
  sspImportHref: string;
}

export interface AssessmentSummary {
  totalControlsAssessed: number;
  totalFindings: number;
  totalObservations: number;
  totalRisks: number;
  findingsBySeverity: Record<string, number>;
  observationsByType: Record<string, number>;
  scoreDistribution: Record<string, number>;
  risksBySeverity: Record<string, number>;
  uniqueFamiliesAssessed: number;
}

export interface ControlFamilyAssessment {
  familyId: string;
  familyName: string;
  totalControlsAssessed: number;
  totalFindings: number;
  totalObservations: number;
  assessedControls: Array<{
    controlId: string;
    findingsCount: number;
    observationsCount: number;
    assessmentStatus: string;
  }>;
}

export interface Finding {
  uuid: string;
  title: string;
  description: string;
  relatedControls: string[];
  relatedObservations: string[];
  score?: number;
  qualityScore?: number;
  completenessScore?: number;
}

export interface Observation {
  uuid: string;
  title: string;
  description: string;
  relatedControls: string[];
  observationType: string;
  overallScore?: number;
  qualityScore?: number;
  completenessScore?: number;
}

export interface Risk {
  uuid: string;
  title: string;
  description: string;
  status: string;
  relatedControls: string[];
}

export interface SarVisualizationData {
  success: boolean;
  message: string;
  timestamp: string;
  assessmentInfo: AssessmentInfo;
  assessmentSummary: AssessmentSummary;
  controlsByFamily: Record<string, ControlFamilyAssessment>;
  findings: Finding[];
  observations: Observation[];
  risks: Risk[];
}

// Authorization Template Types
export interface AuthorizationTemplateRequest {
  name: string;
  content: string;
}

export interface AuthorizationTemplateResponse {
  id: number;
  name: string;
  content: string;
  createdBy: string;
  createdAt: string; // ISO 8601 date string
  lastUpdatedBy: string;
  lastUpdatedAt: string; // ISO 8601 date string
  variables: string[]; // Extracted variables from content
}

// Authorization Types
export interface AuthorizationRequest {
  name: string;
  sspItemId: string;
  sarItemId?: string; // Optional SAR item ID
  templateId: number;
  variableValues: Record<string, string>;
  dateAuthorized?: string;
  dateExpired?: string;
  systemOwner?: string;
  securityManager?: string;
  authorizingOfficial?: string;
  editedContent?: string; // User-edited template content
}

export interface AuthorizationResponse {
  id: number;
  name: string;
  sspItemId: string;
  sarItemId?: string; // Optional SAR item ID
  templateId: number;
  templateName: string;
  variableValues: Record<string, string>;
  completedContent: string; // Final markdown with variables replaced
  authorizedBy: string;
  authorizedAt: string; // ISO 8601 date string - system timestamp when created
  createdAt: string; // ISO 8601 date string
  dateAuthorized?: string; // ISO 8601 date string - user-specified authorization date
  dateExpired?: string; // ISO 8601 date string
  systemOwner?: string;
  securityManager?: string;
  authorizingOfficial?: string;
  conditions?: ConditionOfApprovalResponse[]; // Conditions of approval
}

// Condition of Approval Types
export type ConditionType = 'MANDATORY' | 'RECOMMENDED';

export interface ConditionOfApprovalRequest {
  authorizationId: number;
  condition: string;
  conditionType: ConditionType;
  dueDate?: string; // ISO date string, optional for RECOMMENDED, required for MANDATORY
}

export interface ConditionOfApprovalResponse {
  id: number;
  authorizationId: number;
  condition: string;
  conditionType: ConditionType;
  dueDate?: string; // ISO date string
  createdAt: string; // ISO 8601 date string
  updatedAt: string; // ISO 8601 date string
}

// Component Definition Builder Types
export interface ComponentDefinitionRequest {
  title: string;
  description?: string;
  version?: string;
  oscalVersion: string;
  filename: string;
  jsonContent: string;
  oscalUuid?: string; // Optional, extracted from JSON if not provided
  componentCount?: number;
  controlCount?: number;
}

export interface ComponentDefinitionResponse {
  id: number;
  oscalUuid: string;
  title: string;
  description?: string;
  version?: string;
  oscalVersion: string;
  azureBlobPath: string;
  filename: string;
  fileSize: number;
  componentCount?: number;
  controlCount?: number;
  createdBy: string;
  createdAt: string; // ISO 8601 date string
  lastUpdatedBy?: string;
  updatedAt: string; // ISO 8601 date string
}

// Reusable Element Types
export type ReusableElementType = 'ROLE' | 'PARTY' | 'LINK' | 'BACK_MATTER' | 'RESPONSIBLE_PARTY';

export interface ReusableElementRequest {
  type: ReusableElementType;
  name: string;
  jsonContent: string;
  description?: string;
  isShared?: boolean;
}

export interface ReusableElementResponse {
  id: number;
  type: ReusableElementType;
  name: string;
  jsonContent: string;
  description?: string;
  createdBy: string;
  createdAt: string; // ISO 8601 date string
  updatedAt: string; // ISO 8601 date string
  isShared: boolean;
  useCount: number;
}
