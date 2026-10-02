import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { UNREACHABLE, toApiError } from './problem';

describe('toApiError', () => {
  it('uses the ProblemDetail detail and field errors', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { title: 'Bad Request', detail: 'One or more fields are invalid.', errors: { email: 'Email must be a valid address' } },
    });
    expect(toApiError(error)).toEqual({
      status: 400,
      message: 'One or more fields are invalid.',
      fieldErrors: { email: 'Email must be a valid address' },
    });
  });

  it('explains when the API cannot be reached', () => {
    expect(toApiError(new HttpErrorResponse({ status: 0 })).message).toBe(UNREACHABLE);
  });

  it('falls back to the title, then a generic message', () => {
    expect(toApiError(new HttpErrorResponse({ status: 500, error: { title: 'Internal Server Error' } })).message)
      .toBe('Internal Server Error');
    expect(toApiError(new HttpErrorResponse({ status: 502, error: '<html>' })).message)
      .toBe('The request could not be completed. Please try again.');
  });
});
