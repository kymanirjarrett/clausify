import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { MonoTypeOperatorFunction, finalize, retry, throwError, timer } from 'rxjs';

/** A request slower than this gets the "waking up" message. */
export const SLOW_AFTER_MS = 5_000;
const RETRY_DELAY_MS = 5_000;
/** Render's free plan takes about two minutes to boot; keep trying a little longer than that (3 minutes). */
const MAX_RETRIES = 36;

export const STILL_STARTING =
  'The server is still starting up. Wait a moment and try again.';

/**
 * The API sleeps after 15 idle minutes on Render's free plan and takes about two minutes to start again.
 * warmUp() pings it as soon as the app loads, so it is usually awake before anyone submits a form, and
 * `waking` tells the UI when that ping is slow.
 */
@Injectable({ providedIn: 'root' })
export class ServerStatus {
  private readonly http = inject(HttpClient);
  private started = false;

  readonly waking = signal(false);

  warmUp(): void {
    if (this.started) {
      return;
    }
    this.started = true;
    const slow = setTimeout(() => this.waking.set(true), SLOW_AFTER_MS);
    this.http
      .get('/api/health')
      .pipe(
        retryWhileStarting(),
        finalize(() => {
          clearTimeout(slow);
          this.waking.set(false);
        }),
      )
      // Failures need no handling here: the forms report their own errors.
      .subscribe({ error: () => undefined });
  }
}

/** Retries every 5 s, for up to 3 minutes, while the API is starting or unreachable; other errors pass through. */
export function retryWhileStarting<T>(): MonoTypeOperatorFunction<T> {
  return retry({
    count: MAX_RETRIES,
    delay: (error) => (isStarting(error) || isOffline(error) ? timer(RETRY_DELAY_MS) : throwError(() => error)),
  });
}

/** While Render boots, Vercel's proxy answers 502 (no response within 120 s), 503, or 504. */
export function isStarting(error: unknown): boolean {
  return error instanceof HttpErrorResponse && [502, 503, 504].includes(error.status);
}

function isOffline(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 0;
}
