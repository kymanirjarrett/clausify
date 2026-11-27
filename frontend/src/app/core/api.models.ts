/** Shapes returned by the Clausify API (see /v3/api-docs). */

export interface User {
  id: number;
  name: string;
  email: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export type ContractStatus = 'UPLOADED' | 'ANALYZING' | 'ANALYZED' | 'FAILED';

export interface ContractSummary {
  id: number;
  filename: string;
  sizeBytes: number;
  pageCount: number;
  status: ContractStatus;
  uploadedAt: string;
}

export interface ContractDetail extends ContractSummary {
  failureReason: string | null;
  analyzedAt: string | null;
}

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export type RiskCategory =
  | 'PAYMENT_TERMS'
  | 'LIABILITY'
  | 'INDEMNIFICATION'
  | 'IP_RIGHTS'
  | 'TERMINATION'
  | 'NON_COMPETE'
  | 'CONFIDENTIALITY'
  | 'JURISDICTION';

export interface Finding {
  id: number;
  category: RiskCategory;
  riskScore: number;
  riskLevel: RiskLevel;
  title: string;
  sectionRef: string | null;
  originalText: string;
  explanation: string;
  suggestedRevision: string | null;
}

export interface CategoryRisk {
  category: RiskCategory;
  highestScore: number;
  riskLevel: RiskLevel;
  findingCount: number;
}

/** GET /api/contracts/{id}/analysis (FR-6). Findings are riskiest first; favorable clauses are listed apart. */
export interface AnalysisResult {
  contractId: number;
  overallScore: number;
  riskLevel: RiskLevel;
  contractType: string | null;
  summary: string | null;
  document: {
    filename: string;
    pageCount: number;
    sizeBytes: number;
    uploadedAt: string;
    analyzedAt: string | null;
    analysisMs: number;
  };
  categories: CategoryRisk[];
  findings: Finding[];
  favorable: Finding[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** RFC 9457 problem details, as produced by the API's GlobalExceptionHandler. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: Record<string, string>;
}
