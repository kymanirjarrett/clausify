import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from './api.models';

/** An API failure reduced to what the UI shows: one message, plus per-field messages for forms. */
export interface ApiError {
  status: number;
  message: string;
  fieldErrors: Record<string, string>;
}

export const UNREACHABLE =
  'The Clausify service could not be reached. Check your connection and try again.';
const UNEXPECTED = 'The request could not be completed. Please try again.';

/**
 * Statuses that, without a ProblemDetail body, came from the host or proxy in front of the API
 * (Vercel's 404 page, Render's 502 while waking) rather than from the API itself. 200 is a
 * response that was not JSON at all, such as an HTML page.
 */
const NOT_FROM_API = new Set([200, 404, 405, 502, 503, 504]);

export function toApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return { status: -1, message: UNEXPECTED, fieldErrors: {} };
  }
  if (error.status === 0) {
    return { status: 0, message: UNREACHABLE, fieldErrors: {} };
  }
  const problem = isProblemDetail(error.error);
  if (!problem && NOT_FROM_API.has(error.status)) {
    return { status: error.status, message: UNREACHABLE, fieldErrors: {} };
  }
  const body = problem ? (error.error as ProblemDetail) : {};
  return {
    status: error.status,
    message: body.detail || body.title || UNEXPECTED,
    fieldErrors: body.errors ?? {},
  };
}

/** The API always answers errors with RFC 9457 JSON, which has at least a title or status. */
function isProblemDetail(body: unknown): boolean {
  return typeof body === 'object' && body !== null && ('title' in body || 'status' in body);
}
