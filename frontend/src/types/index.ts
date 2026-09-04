export type Role = 'ROLE_ADMIN' | 'ROLE_ANALYST' | 'ROLE_VIEWER';

export interface User {
  id: number;
  username: string;
  email: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  username: string;
  email: string;
  role: Role;
  expiresIn: number;
}

export type BatchStatus = 'PENDING_FILES' | 'READY_FOR_RULES' | 'RULES_COMPLETED' | 'AI_COMPLETED' | 'FAILED';

export interface Batch {
  id: number;
  batchName: string;
  status: BatchStatus;
  uploadedBy: string;
  createdAt: string;
  totalRecords: number;
  pass1MatchedCount: number;
  pass2MatchedCount: number;
  manualReviewCount: number;
  totalReconciledAmount: number;
  gatewayUploaded: boolean;
  bankUploaded: boolean;
  ledgerUploaded: boolean;
  readyForRules: boolean;
}

export type MatchStatus = 'MATCHED' | 'NEEDS_REVIEW' | 'MANUAL_REVIEW' | 'DISCREPANCY';
export type MatchPass = 'PASS1_RULES' | 'PASS2_AI' | 'MANUAL';

export interface MatchResult {
  id: number;
  batchId: number;
  gatewayId?: number;
  gatewayTxnId?: string;
  gatewayAmount?: number;
  gatewayOrderId?: string;
  gatewayCustomerEmail?: string;
  bankId?: number;
  bankRefId?: string;
  bankAmount?: number;
  bankNarration?: string;
  bankFee?: number;
  ledgerId?: number;
  ledgerEntryId?: string;
  ledgerInternalRef?: string;
  ledgerAmount?: number;
  ledgerDescription?: string;
  matchStatus: MatchStatus;
  matchPass?: MatchPass;
  ruleFired?: string;
  confidence?: number;
  caseType?: string;
  reasoning?: string;
  suggestedMatchId?: string;
  matchedAmount?: number;
  discrepancyAmount?: number;
  createdAt: string;
}

export interface AuditLogEntry {
  id: number;
  batchId: number;
  matchResultId?: number;
  source: 'RULE_ENGINE' | 'AI_MODEL' | 'USER_MANUAL' | 'SYSTEM';
  action: string;
  ruleOrModel?: string;
  confidence?: number;
  inputSnapshot?: string;
  outputSnapshot?: string;
  reasoning?: string;
  timestamp: string;
}

export interface BatchMetrics {
  batchId: number;
  batchName: string;
  status: string;
  totalRecords: number;
  matchedCount: number;
  pass1MatchedCount: number;
  pass2MatchedCount: number;
  manualReviewCount: number;
  matchRatePercentage: number;
  totalReconciledAmount: number;
  totalDiscrepancyAmount: number;
  exceptionBreakdown: Record<string, number>;
  statusBreakdown: Record<string, number>;
  passBreakdown: Record<string, number>;
}

export interface ChatMessage {
  id: number;
  batchId: number;
  username: string;
  sender: 'USER' | 'ASSISTANT' | 'SYSTEM';
  message: string;
  timestamp: string;
}
