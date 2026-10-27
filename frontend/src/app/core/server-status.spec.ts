import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SLOW_AFTER_MS, ServerStatus, isStarting } from './server-status';

describe('ServerStatus.warmUp', () => {
  let status: ServerStatus;
  let http: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    status = TestBed.inject(ServerStatus);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  it('stays quiet when the API answers quickly', () => {
    status.warmUp();
    http.expectOne('/api/health').flush({ status: 'UP' });

    vi.advanceTimersByTime(SLOW_AFTER_MS);
    expect(status.waking()).toBe(false);
  });

  it('reports waking while slow, retries through 502s, and clears once the API answers', () => {
    status.warmUp();
    vi.advanceTimersByTime(SLOW_AFTER_MS);
    expect(status.waking()).toBe(true);

    http.expectOne('/api/health').flush('Bad Gateway', { status: 502, statusText: 'Bad Gateway' });
    vi.advanceTimersByTime(5_000);
    http.expectOne('/api/health').flush({ status: 'UP' });

    expect(status.waking()).toBe(false);
  });

  it('pings only once however many times it is called', () => {
    status.warmUp();
    status.warmUp();
    http.expectOne('/api/health').flush({ status: 'UP' });
  });
});

describe('isStarting', () => {
  it('matches the statuses a sleeping API produces behind the proxy', () => {
    for (const status of [502, 503, 504]) {
      expect(isStarting(new HttpErrorResponse({ status }))).toBe(true);
    }
    for (const status of [0, 401, 404, 500]) {
      expect(isStarting(new HttpErrorResponse({ status }))).toBe(false);
    }
  });
});
