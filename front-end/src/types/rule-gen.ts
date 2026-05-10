export type RuleGenPhase = 'clarify' | 'proposal' | 'exhausted';

export interface TestCase {
  description: string;
  fragmentJson: string;
  expected: 'pass' | 'fail';
}

export interface TestResult {
  index: number;
  description: string;
  expected: 'pass' | 'fail';
  actual: 'pass' | 'fail';
  passed: boolean;
  violationMessage: string | null;
}

export interface RuleProposal {
  name: string;
  description: string;
  severity: 'error' | 'warning' | 'info';
  fieldPath: string;
  constraintXml: string;
  testCases: TestCase[];
}

export interface RuleGenTurnResponse {
  phase: RuleGenPhase;
  clarifyingQuestion: string | null;
  proposal: RuleProposal | null;
  testResults: TestResult[] | null;
  lastProposal: RuleProposal | null;
  message: string | null;
  iterations: number;
  totalTokensIn: number;
  totalTokensOut: number;
}

export interface ChatEntry {
  role: 'user' | 'assistant';
  text: string;
}

export type OscalModelType =
  | 'catalog'
  | 'profile'
  | 'system-security-plan'
  | 'component-definition'
  | 'assessment-plan'
  | 'assessment-results'
  | 'plan-of-action-and-milestones';
