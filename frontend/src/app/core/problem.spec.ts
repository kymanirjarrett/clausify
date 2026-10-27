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
    expect(toApiError(new HttpErrorResponse({ status: 500, error: '<html>' })).message)
      .toBe('The request could not be completed. Please try again.');
  });

  it('treats error pages from the host or proxy as the API being unreachable', () => {
    expect(toApiError(new HttpErrorResponse({ status: 404, error: '<html>Not Found</html>' })).message).toBe(UNREACHABLE);
    expect(toApiError(new HttpErrorResponse({ status: 502, error: '<html>' })).message).toBe(UNREACHABLE);
    expect(toApiError(new HttpErrorResponse({ status: 200, error: { error: new SyntaxError(), text: '<!doctype html>' } })).message)
      .toBe(UNREACHABLE);
  });

  it('keeps the API message for its own 404s', () => {
    const error = new HttpErrorResponse({ status: 404, error: { title: 'Not Found', status: 404, detail: 'Contract not found.' } });
    expect(toApiError(error).message).toBe('Contract not found.');
  });
});
