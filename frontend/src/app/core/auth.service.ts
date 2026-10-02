import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, firstValueFrom, tap } from 'rxjs';
import { AuthResponse, User } from './api.models';

const TOKEN_KEY = 'clausify.token';

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

/**
 * Signed-in state. The API is stateless: the JWT from register or login is the whole session.
 * It is kept in localStorage so a reload stays signed in, and cleared on sign-out or expiry.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly tokenSignal = signal<string | null>(readToken());
  private readonly userSignal = signal<User | null>(null);
  private expiryTimer: ReturnType<typeof setTimeout> | undefined;

  readonly token = this.tokenSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly isSignedIn = computed(() => this.tokenSignal() !== null);

  constructor() {
    this.scheduleExpiry(this.tokenSignal());
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', request).pipe(tap((r) => this.signIn(r)));
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', request).pipe(tap((r) => this.signIn(r)));
  }

  /** Called once at startup: confirms a stored token still works and loads the user. */
  async restore(): Promise<void> {
    if (!this.tokenSignal()) {
      return;
    }
    try {
      this.userSignal.set(await firstValueFrom(this.http.get<User>('/api/auth/me')));
    } catch {
      this.clear();
    }
  }

  signOut(reason?: 'expired'): void {
    this.clear();
    void this.router.navigate(['/login'], reason ? { queryParams: { reason } } : {});
  }

  private signIn(response: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    this.tokenSignal.set(response.token);
    this.userSignal.set(response.user);
    this.scheduleExpiry(response.token);
  }

  private clear(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.tokenSignal.set(null);
    this.userSignal.set(null);
    clearTimeout(this.expiryTimer);
  }

  /** Sign out the moment the token expires, rather than on the next failed request. */
  private scheduleExpiry(token: string | null): void {
    clearTimeout(this.expiryTimer);
    const expiresAt = token ? tokenExpiry(token) : null;
    if (expiresAt === null) {
      return;
    }
    const remaining = expiresAt - Date.now();
    if (remaining <= 0) {
      this.clear();
      return;
    }
    // setTimeout overflows above ~24.8 days; tokens last 24 hours.
    this.expiryTimer = setTimeout(() => this.signOut('expired'), Math.min(remaining, 2 ** 31 - 1));
  }
}

function readToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

/** Reads the `exp` claim (seconds) without verifying it; the API verifies every request. */
export function tokenExpiry(token: string): number | null {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const exp = JSON.parse(atob(payload)).exp;
    return typeof exp === 'number' ? exp * 1000 : null;
  } catch {
    return null;
  }
}
