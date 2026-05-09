export type TicketType = 'BUG' | 'FEATURE';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketStatus =
  | 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'WONT_FIX' | 'DUPLICATE';

export interface AttachmentResponse {
  id: number;
  filename: string;
  contentType: string;
  sizeBytes: number;
}

export interface CommentResponse {
  id: number;
  authorUsername: string;
  body: string;
  statusChange: boolean;
  oldStatus: TicketStatus | null;
  newStatus: TicketStatus | null;
  createdAt: string;
  attachments: AttachmentResponse[];
}

export interface TicketSummaryResponse {
  id: number;
  type: TicketType;
  title: string;
  priority: TicketPriority;
  status: TicketStatus;
  reporterUsername: string;
  createdAt: string;
  updatedAt: string;
}

export interface TicketDetailResponse {
  id: number;
  type: TicketType;
  title: string;
  description: string;
  priority: TicketPriority;
  status: TicketStatus;
  metadata: Record<string, unknown>;
  reporterUsername: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  originalAttachments: AttachmentResponse[];
  comments: CommentResponse[];
}

export interface BugMetadata {
  stepsToReproduce?: string;
  expectedBehavior?: string;
  actualBehavior?: string;
  severity?: 'MINOR' | 'MAJOR' | 'CRITICAL';
  browser?: string;
  viewport?: string;
  url?: string;
}

export interface FeatureMetadata {
  useCase?: string;
}
