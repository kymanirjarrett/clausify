import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

// The health ping carries no token, so a stale one can't trigger a sign-out from a public page.
const PUBLIC = ['/api/auth/login', '/api/auth/register', '/api/health'];

/** Attaches the bearer token to API calls, and signs out when the API rejects it. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const isApi = request.url.startsWith('/api/');
  const isPublic = PUBLIC.includes(request.url);

  const authorized = isApi && !isPublic && token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authorized).pipe(
    catchError((error: unknown) => {
      // Only if this request's token is still the session's: a late 401 for an old session must not sign out a new one.
      if (error instanceof HttpErrorResponse && error.status === 401 && isApi && !isPublic && token && auth.token() === token) {
        auth.signOut('expired');
      }
      return throwError(() => error);
    }),
  );
};
