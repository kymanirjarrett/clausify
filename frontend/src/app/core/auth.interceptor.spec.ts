import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

const TOKEN_KEY = 'clausify.token';
const USER_A = { id: 1, name: 'A', email: 'a@example.com' };
const USER_B = { id: 2, name: 'B', email: 'b@example.com' };

function setUp(storedToken: string | null) {
  localStorage.clear();
  if (storedToken) localStorage.setItem(TOKEN_KEY, storedToken);
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([{ path: 'login', children: [] }]),
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  return {
    http: TestBed.inject(HttpClient),
    httpMock: TestBed.inject(HttpTestingController),
    auth: TestBed.inject(AuthService),
  };
}

/** Signs in through the real login call, as a second tab or the login page would. */
async function signInAs(t: ReturnType<typeof setUp>, token: string, user: typeof USER_B) {
  const done = firstValueFrom(t.auth.login({ email: user.email, password: 'pw' }));
  t.httpMock.expectOne('/api/auth/login').flush({ token, user });
  await done;
}

describe('authInterceptor: Authorization header', () => {
  let t: ReturnType<typeof setUp>;
  beforeEach(() => (t = setUp('token-a')));
  afterEach(() => t.httpMock.verify());

  it('attaches the bearer token to a protected API call', () => {
    t.http.get('/api/contracts').subscribe();
    expect(t.httpMock.expectOne('/api/contracts').request.headers.get('Authorization')).toBe('Bearer token-a');
  });

  it('sends no token to the public auth routes or the health ping', () => {
    for (const url of ['/api/auth/login', '/api/auth/register', '/api/health']) {
      t.http.post(url, {}).subscribe();
      expect(t.httpMock.expectOne(url).request.headers.has('Authorization')).toBe(false);
    }
  });

  it('never sends the token to another host', () => {
    t.http.get('https://example.com/api/contracts').subscribe();
    expect(t.httpMock.expectOne('https://example.com/api/contracts').request.headers.has('Authorization')).toBe(false);
  });

  it('sends no header when signed out', () => {
    t = setUp(null);
    t.http.get('/api/contracts').subscribe();
    expect(t.httpMock.expectOne('/api/contracts').request.headers.has('Authorization')).toBe(false);
  });
});

describe('authInterceptor: 401 handling', () => {
  let t: ReturnType<typeof setUp>;
  beforeEach(() => (t = setUp('token-a')));
  afterEach(() => t.httpMock.verify());

  it('signs out when the current token is rejected', () => {
    t.http.get('/api/contracts').subscribe({ error: () => undefined });
    t.httpMock.expectOne('/api/contracts').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(t.auth.token()).toBeNull();
  });

  it('ignores a late 401 for an old token after a new sign-in', async () => {
    t.http.get('/api/contracts').subscribe({ error: () => undefined });
    const stale = t.httpMock.expectOne('/api/contracts');

    await signInAs(t, 'token-b', USER_B);
    stale.flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(t.auth.token()).toBe('token-b');
    expect(t.auth.user()).toEqual(USER_B);
  });
});

describe('AuthService.restore', () => {
  let t: ReturnType<typeof setUp>;
  beforeEach(() => (t = setUp('token-a')));
  afterEach(() => t.httpMock.verify());

  it('loads the user for the stored token', async () => {
    const restoring = t.auth.restore();
    t.httpMock.expectOne('/api/auth/me').flush(USER_A);
    await restoring;
    expect(t.auth.user()).toEqual(USER_A);
  });

  it('does not bring back the old user after the session changed', async () => {
    const restoring = t.auth.restore();
    const me = t.httpMock.expectOne('/api/auth/me');

    await signInAs(t, 'token-b', USER_B);
    me.flush(USER_A);
    await restoring;

    expect(t.auth.user()).toEqual(USER_B);
  });

  it('does not clear a new session when the old token is rejected late', async () => {
    const restoring = t.auth.restore();
    const me = t.httpMock.expectOne('/api/auth/me');

    await signInAs(t, 'token-b', USER_B);
    me.flush(null, { status: 404, statusText: 'Not Found' });
    await restoring;

    expect(t.auth.token()).toBe('token-b');
    expect(t.auth.user()).toEqual(USER_B);
  });

  it('does not restore a user after signing out', async () => {
    const restoring = t.auth.restore();
    const me = t.httpMock.expectOne('/api/auth/me');

    t.auth.signOut();
    me.flush(USER_A);
    await restoring;

    expect(t.auth.user()).toBeNull();
  });
});
