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
