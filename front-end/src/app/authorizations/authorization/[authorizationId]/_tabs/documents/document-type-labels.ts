import type { DocumentType } from '@/types/oscal';

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  VULNERABILITY_SCAN: 'Vulnerability Scan',
  PENETRATION_TEST: 'Penetration Test',
  ASSET_INVENTORY: 'Asset Inventory',
  SSP: 'System Security Plan',
  SAR: 'Security Assessment Report',
  CONFIGURATION_BASELINE: 'Configuration Baseline',
  CONTINGENCY_PLAN: 'Contingency Plan',
  INCIDENT_RESPONSE_PLAN: 'Incident Response Plan',
  AUDIT_REPORT: 'Audit Report',
  AUTHORIZATION_LETTER: 'Authorization Letter',
  CHANGE_NOTICE_TICKET: 'Change Notice / Ticket',
  RISK_ASSESSMENT: 'Risk Assessment',
  BUSINESS_CONTINUITY_PLAN: 'Business Continuity Plan',
  DISASTER_RECOVERY_PLAN: 'Disaster Recovery Plan',
  BUSINESS_IMPACT_ASSESSMENT: 'Business Impact Assessment',
  OTHER: 'Other',
};

export const ALL_DOCUMENT_TYPES: DocumentType[] = [
  'VULNERABILITY_SCAN',
  'PENETRATION_TEST',
  'ASSET_INVENTORY',
  'SSP',
  'SAR',
  'CONFIGURATION_BASELINE',
  'CONTINGENCY_PLAN',
  'INCIDENT_RESPONSE_PLAN',
  'AUDIT_REPORT',
  'AUTHORIZATION_LETTER',
  'CHANGE_NOTICE_TICKET',
  'RISK_ASSESSMENT',
  'BUSINESS_CONTINUITY_PLAN',
  'DISASTER_RECOVERY_PLAN',
  'BUSINESS_IMPACT_ASSESSMENT',
  'OTHER',
];

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
