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
  aiGenerated?: boolean;
  generationPrompt?: string;
  generationModel?: string;
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

  // Visibility (Phase 1 publish): "PRIVATE" | "ORGANIZATION" | "PUBLIC"
  // Optional because legacy items pre-dating the visibility migration may not
  // round-trip the field, and certain endpoints in the frontend may not
  // populate it.
  visibility?: 'PRIVATE' | 'ORGANIZATION' | 'PUBLIC';
  organizationId?: number;

  // Rating and comment fields
  averageRating?: number;
  totalRatings?: number;
  commentCount?: number;

  // Convenience properties (computed or optional)
  name?: string; // Alias for title
  blobUrl?: string; // Optional blob storage URL
  version?: number; // Convenience property for currentVersion?.versionNumber
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

// Rating Types
export interface RatingStats {
  averageRating: number;
  totalRatings: number;
  userRating?: number; // Current user's rating, null if not rated
}

export interface RatingRequest {
  rating: number; // 1-5
}

// Comment Types
export interface LibraryComment {
  commentId: string;
  content: string;
  username: string;
  userDisplayName?: string;
  createdAt: string; // ISO 8601 date string
  updatedAt: string; // ISO 8601 date string
  isEdited: boolean;
  parentCommentId?: string;
  replies: LibraryComment[];
  replyCount: number;
}

export interface CommentRequest {
  content: string;
  parentCommentId?: string; // Optional - for replies
}

// Service Account Token Types
export interface ServiceAccountTokenRequest {
  tokenName: string;
  expirationDays: number;
}

export interface ServiceAccountTokenResponse {
  id: number;
  token: string;
  tokenName: string;
  username: string;
  expiresAt: string; // ISO 8601 date string
  expirationDays: number;
  /** Permissions snapshotted into the token at issuance. */
  globalRole: string | null;
  orgRole: string | null;
}

/**
 * Listing view of a service account token. Has no `token` field by design —
 * the value is shown once at creation and is never retrievable.
 */
export interface ServiceAccountTokenSummary {
  id: number;
  tokenName: string;
  globalRole: string | null;
  orgRole: string | null;
  organizationId: number | null;
  createdAt: string;
  expiresAt: string;
  lastUsedAt: string | null;
  revokedAt: string | null;
  status: 'ACTIVE' | 'EXPIRED' | 'REVOKED';
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
  organizationId: number;
  name: string;
  content: string;
  createdBy: string;
  createdAt: string; // ISO 8601 date string
  lastUpdatedBy: string;
  lastUpdatedAt: string; // ISO 8601 date string
  variables: string[]; // Extracted variables from content
}

// Authorization ACL / Grant Types
export type AuthorizationRole = 'OWNER' | 'EDITOR' | 'CONTRIBUTOR' | 'VIEWER';

export interface AuthorizationGrantResponse {
  id: number;
  userId: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: AuthorizationRole;
  grantedByUsername?: string;
  grantedAt: string;
}

export interface OrgMemberResponse {
  userId: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
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
  conditions?: ConditionOfApprovalRequest[]; // Conditions of approval
}

export interface AuthorizationResponse {
  id: number;
  organizationId: number;
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

  // Digital Signature fields
  digitalSignatureMethod?: string;
  signerCertificate?: string;
  electronicSignatureImage?: string; // Base64-encoded PNG for electronic signatures
  signerCommonName?: string;
  signerEmail?: string;
  signerEdipi?: string;
  certificateIssuer?: string;
  certificateSerial?: string;
  certificateNotBefore?: string; // ISO 8601 date string
  certificateNotAfter?: string; // ISO 8601 date string
  signatureTimestamp?: string; // ISO 8601 date string
  documentHash?: string;
  certificateVerified?: boolean;
  certificateVerificationDate?: string; // ISO 8601 date string
  certificateVerificationNotes?: string;

  // ACL / sharing fields
  effectiveRole?: AuthorizationRole;
  shareWithOrgDefaultRole?: AuthorizationRole | null;
}

// Condition of Approval Types
export type ConditionType = 'MANDATORY' | 'RECOMMENDED';

export interface ConditionOfApprovalRequest {
  authorizationId?: number; // Optional when creating, assigned by backend
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
  capabilityCount?: number;
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
  capabilityCount?: number;
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

// Audit Log Types
export interface AuditLog {
  id: number;
  eventType: string;
  category: string;
  username: string | null;
  userId: number | null;
  ipAddress: string | null;
  userAgent: string | null;
  sessionId: string | null;
  resource: string | null;
  action: string | null;
  outcome: string;
  errorMessage: string | null;
  metadata: string | null;
  riskLevel: string;
  timestamp: string; // ISO 8601 date string
  processingTimeMs: number | null;
  reviewed: boolean;
  reviewNotes: string | null;
  reviewedAt: string | null;
  reviewedBy: string | null;
  requestUrl: string | null;
  httpMethod: string | null;
  integrityHash: string | null;
  previousHash: string | null;
}

export interface AuditLogStats {
  totalLogs: number;
  logsToday: number;
  securityEventsToday: number;
  errorsToday: number;
  highRiskUnreviewed: number;
  byCategory: Record<string, number>;
  byRiskLevel: Record<string, number>;
  byOutcome: Record<string, number>;
}

// Health Check Types
export type HealthStatus = 'UP' | 'DOWN' | 'DEGRADED' | 'UNKNOWN';

export interface SimpleHealthResponse {
  status: HealthStatus;
  timestamp: string;
  version: string;
}

export interface ComponentHealth {
  status: HealthStatus;
  message?: string;
  details?: Record<string, unknown>;
  responseTimeMs?: number;
}

export interface ApplicationInfo {
  name: string;
  version: string;
  profile: string;
  uptime: string;
  startTime: string;
}

export interface HealthSystemInfo {
  totalMemoryMb: number;
  usedMemoryMb: number;
  freeMemoryMb: number;
  memoryUsagePercent: number;
  availableProcessors: number;
  systemLoadAverage: number;
  totalDiskSpaceGb: number;
  freeDiskSpaceGb: number;
  diskUsagePercent: number;
}

export interface EnvironmentInfo {
  javaVersion: string;
  javaVendor: string;
  osName: string;
  osVersion: string;
  osArch: string;
  timezone: string;
}

export interface DetailedHealthResponse {
  status: HealthStatus;
  timestamp: string;
  application: ApplicationInfo;
  components: {
    database: ComponentHealth;
    storage: ComponentHealth;
    memory: ComponentHealth;
    diskSpace: ComponentHealth;
    oscalLibrary: ComponentHealth;
    [key: string]: ComponentHealth;
  };
  system: HealthSystemInfo;
  environment: EnvironmentInfo;
}

// Security Compliance Types (SOC 2)
export type ControlCategory = 'CC6' | 'CC7' | 'CC8' | 'CC9' | 'DATA' | 'AUDIT';

export type ControlStatus = 'IMPLEMENTED' | 'PARTIAL' | 'GAP';

export type GapSeverity = 'HIGH' | 'MEDIUM' | 'LOW';

export interface Soc2Control {
  controlId: string;
  name: string;
  description: string;
  category: ControlCategory;
  status: ControlStatus;
  implementation: string;
  evidence: string[];
}

export interface GapAnalysis {
  gapId: string;
  controlId: string;
  title: string;
  description: string;
  severity: GapSeverity;
  recommendation: string;
  effort: string;
  priority: number;
}

export interface CategorySummary {
  displayName: string;
  total: number;
  implemented: number;
  partial: number;
  gaps: number;
}

export interface ComplianceSummary {
  totalControls: number;
  implementedControls: number;
  partialControls: number;
  gapControls: number;
  compliancePercentage: number;
  assessmentDate: string;
  byCategory: Record<string, CategorySummary>;
}

// Security Policy Types
export interface SecurityPolicy {
  id: number;
  mfaRequired: boolean;
  passwordMinLength: number;
  passwordMaxLength: number;
  passwordRotationDays: number;
  auditLogRetentionDays: number;
  updatedAt: string; // ISO 8601 date string
  updatedBy?: string;
}

export interface SecurityPolicyUpdateRequest {
  mfaRequired: boolean;
  passwordMinLength: number;
  passwordMaxLength: number;
  passwordRotationDays: number;
  auditLogRetentionDays: number;
}

// MFA Types
export interface MfaSetupResponse {
  qrCodeDataUri: string;
  secret: string;
  formattedSecret: string;
  setupToken: string;
}

export interface MfaSetupCompleteRequest {
  setupToken: string;
  totpCode: string;
}

export interface MfaSetupCompleteResponse {
  token: string;
  backupCodes: string[];
}

export interface MfaVerifyRequest {
  mfaToken: string;
  totpCode: string;
}

export interface MfaBackupCodeRequest {
  mfaToken: string;
  backupCode: string;
}

export interface MfaStatus {
  mfaEnabled: boolean;
  mfaSetupCompleted: boolean;
  backupCodesRemaining: number;
}

// Artifact Types
export type ArtifactVisibility = 'PRIVATE' | 'ORGANIZATION' | 'PUBLIC';

export interface ArtifactVersion {
  versionId: string;
  versionNumber: number;
  contentSize: number;
  uploadedBy: string;
  uploadedAt: string; // ISO 8601 date string
  changeDescription?: string;
  extractedVariables: string[];
}

export interface Artifact {
  artifactId: string;
  title: string;
  description?: string;
  visibility: ArtifactVisibility;
  organizationId?: number;
  organizationName?: string;
  createdBy: string;
  createdAt: string; // ISO 8601 date string
  updatedAt: string; // ISO 8601 date string
  tags: string[];
  currentVersion?: ArtifactVersion;
  extractedVariables: string[];
  downloadCount: number;
  viewCount: number;
  versionCount?: number;

  // Rating and comment fields
  averageRating?: number;
  totalRatings?: number;
  commentCount?: number;
}

export interface ArtifactRequest {
  title: string;
  description?: string;
  visibility: ArtifactVisibility;
  organizationId?: number;
  content: string; // Markdown content
  tags?: string[];
}

export interface ArtifactUpdateRequest {
  title?: string;
  description?: string;
  visibility?: ArtifactVisibility;
  organizationId?: number;
  tags?: string[];
}

export interface ArtifactVersionRequest {
  content: string; // Markdown content
  changeDescription?: string;
}

export interface ArtifactTag {
  name: string;
  usageCount: number;
}

export interface ArtifactAnalytics {
  totalArtifacts: number;
  totalVersions: number;
  totalTags: number;
  artifactsByVisibility: Record<string, number>;
  popularTags: Array<{
    name: string;
    count: number;
  }>;
  mostDownloaded: Array<{
    artifactId: string;
    title: string;
    downloadCount: number;
  }>;
}

// Authorization Document Types
export type DocumentType =
  | 'VULNERABILITY_SCAN'
  | 'PENETRATION_TEST'
  | 'ASSET_INVENTORY'
  | 'SSP'
  | 'SAR'
  | 'CONFIGURATION_BASELINE'
  | 'CONTINGENCY_PLAN'
  | 'INCIDENT_RESPONSE_PLAN'
  | 'AUDIT_REPORT'
  | 'AUTHORIZATION_LETTER'
  | 'CHANGE_NOTICE_TICKET'
  | 'RISK_ASSESSMENT'
  | 'BUSINESS_CONTINUITY_PLAN'
  | 'DISASTER_RECOVERY_PLAN'
  | 'BUSINESS_IMPACT_ASSESSMENT'
  | 'OTHER';

export interface AuthorizationDocumentResponse {
  id: number;
  authorizationId: number;
  originalFilename: string;
  fileSize: number;
  contentType: string;
  documentType: DocumentType;
  description?: string | null;
  tags?: string | null;
  version?: string | null;
  effectiveDate?: string | null;
  expiresAt?: string | null;
  uploadedByUsername?: string | null;
  uploadedAt: string;
}

export interface PackageCompletenessItem {
  documentType: DocumentType;
  presentCount: number;
  satisfied: boolean;
}

export interface PackageCompletenessResponse {
  coreDocuments: PackageCompletenessItem[];
}

export interface UpdateDocumentMetadataRequest {
  documentType?: DocumentType;
  description?: string | null;
  tags?: string | null;
  version?: string | null;
  effectiveDate?: string | null;
  expiresAt?: string | null;
}

// Continuous Monitoring (ConMon) Types
export type ConMonItemStatus = 'OPEN' | 'CLOSED' | 'UNKNOWN';
export type ConMonSourceFormat = 'OSCAL_JSON' | 'OSCAL_XML' | 'OSCAL_YAML' | 'FEDRAMP_XLSX';

export interface ConMonReconciliationCounts {
  newCount: number;
  closedCount: number;
  reopenedCount: number;
  stillOpenCount: number;
  removedCount: number;
  changedCount: number;
  previousSnapshotId?: number | null;
}

export interface ConMonSnapshotSummary {
  id: number;
  authorizationId: number;
  uploadedAt: string;
  uploadedByUsername?: string | null;
  sourceFormat: ConMonSourceFormat;
  originalFilename: string;
  oscalUuid?: string | null;
  oscalVersion?: string | null;
  metadataTitle?: string | null;
  metadataLastModified?: string | null;
  openCount: number;
  closedCount: number;
  unknownCount: number;
  notes?: string | null;
  reconciliation?: ConMonReconciliationCounts | null;
}

export interface ConMonPoamItem {
  id: number;
  externalId: string;
  title: string;
  description?: string | null;
  status: ConMonItemStatus;
  rawStatus?: string | null;
  severity?: 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL' | null;
  weaknessSource?: string | null;
  scheduledCompletionDate?: string | null;
  actualCompletionDate?: string | null;
  pointOfContact?: string | null;
  riskRating?: string | null;
}

export interface ConMonChangedItem {
  current: ConMonPoamItem;
  previous: ConMonPoamItem;
  fieldsChanged: string[];
}

export interface ConMonReconciliationDetail {
  snapshotId: number;
  previousSnapshotId: number;
  newCount: number;
  closedCount: number;
  reopenedCount: number;
  stillOpenCount: number;
  removedCount: number;
  changedCount: number;
  newItems: ConMonPoamItem[];
  newlyClosedItems: ConMonPoamItem[];
  reopenedItems: ConMonPoamItem[];
  removedItems: ConMonPoamItem[];
  changedItems: ConMonChangedItem[];
}

export interface ConMonSlaStats {
  openTotal: number;
  withinSla: number;
  overdue: number;
  withoutDeadline: number;
  slaPercent: number | null;
}

export interface ConMonAnalytics {
  openCountSeries: Array<{ date: string; open: number; closed: number; unknown: number }>;
  currentSeverityBreakdown: Array<{ label: string; count: number }>;
  currentStatusBreakdown: Array<{ label: string; count: number }>;
  agingBuckets: Array<{ bucket: string; count: number }>;
  meanTimeToCloseDays?: number | null;
  slaStats: ConMonSlaStats;
}

// Artifact Comment (reuses LibraryComment structure)
export interface ArtifactComment {
  commentId: string;
  content: string;
  username: string;
  userDisplayName?: string;
  createdAt: string; // ISO 8601 date string
  updatedAt: string; // ISO 8601 date string
  isEdited: boolean;
  parentCommentId?: string;
  replies: ArtifactComment[];
  replyCount: number;
}

// Leaderboard Types (matches backend LeaderboardResponse/LeaderboardEntry)
export type LeaderboardWindow = '30d' | 'all';

export interface LeaderboardEntry {
  rank: number;
  username: string;
  displayName: string;
  score: number;
  /**
   * Only present on the "most active" board: activity source
   * (operations, libraryPublishes, artifacts, documents, authorizations)
   * to count. Zero-count sources are omitted.
   */
  breakdown?: Record<string, number> | null;
}

export interface LeaderboardResponse {
  window: LeaderboardWindow;
  generatedAt: string; // ISO 8601 date string
  mostActive: LeaderboardEntry[];
  topContributors: LeaderboardEntry[];
}
