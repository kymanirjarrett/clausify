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

/** GET/PATCH /api/users/me (FR-12). */
export interface AccountDetails extends User {
  phoneNumber: string | null;
}

/** GET /api/users/me/stats (FR-12). averageRiskScore is null before the first analysis. */
export interface AccountStats {
  contractsUploaded: number;
  contractsAnalyzed: number;
  averageRiskScore: number | null;
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

export interface DiffSegment {
  text: string;
  changed: boolean;
}

/** One stretch of text in a version comparison, as each version shows it. */
export interface DiffRow {
  type: 'EQUAL' | 'CHANGED' | 'ADDED' | 'REMOVED';
  original: DiffSegment[];
  revised: DiffSegment[];
}

export interface ComparedVersion {
  id: number;
  filename: string;
  uploadedAt: string;
  overallScore: number;
  riskLevel: RiskLevel;
}

/** POST /api/contracts/{id}/compare/{otherId} (FR-10). A null category score means no risky finding there. */
export interface Comparison {
  original: ComparedVersion;
  revised: ComparedVersion;
  scoreChange: number;
  similarity: number;
  unrelated: boolean;
  categoryChanges: { category: RiskCategory; before: number | null; after: number | null }[];
  counts: { added: number; removed: number; changed: number };
  rows: DiffRow[];
}

/** A clause from the standard library (FR-9). */
export interface StandardClause {
  id: number;
  category: RiskCategory;
  title: string;
  text: string;
  favorable: boolean;
  notes: string | null;
}

/** GET /api/clauses/{findingId}/similar: the 3 closest standard clauses, most similar first. */
export interface SimilarClauses {
  finding: { id: number; category: RiskCategory; title: string; originalText: string; suggestedRevision: string | null };
  matches: { clause: StandardClause; similarity: number }[];
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
