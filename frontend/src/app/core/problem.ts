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

export function toApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) {
    return { status: -1, message: UNEXPECTED, fieldErrors: {} };
  }
  if (error.status === 0) {
    return { status: 0, message: UNREACHABLE, fieldErrors: {} };
  }
  const body = (typeof error.error === 'object' && error.error) ? (error.error as ProblemDetail) : {};
  return {
    status: error.status,
    message: body.detail || body.title || UNEXPECTED,
    fieldErrors: body.errors ?? {},
  };
}
