import { provideZonelessChangeDetection, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { Landing, pathLength } from './landing';

const STEP = 2600;

async function render(options: { reducedMotion?: boolean; signedIn?: boolean } = {}) {
  vi.stubGlobal('matchMedia', (query: string) => ({ matches: !!options.reducedMotion, media: query }));
  vi.stubGlobal('ResizeObserver', class { observe() {} disconnect() {} });
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [Landing],
    providers: [
      provideZonelessChangeDetection(),
      provideRouter([]),
      { provide: AuthService, useValue: { isSignedIn: signal(!!options.signedIn) } },
    ],
  });
  const fixture = TestBed.createComponent(Landing);
  await fixture.whenStable();
  return fixture;
}

function activeWord(el: HTMLElement): string | undefined {
  return el.querySelector('.rotator .on')?.textContent?.trim();
}

describe('Landing', () => {
  beforeEach(() => vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] }));
  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('rotates the headline word', async () => {
    const fixture = await render();
    expect(activeWord(fixture.nativeElement)).toBe('liability');
    vi.advanceTimersByTime(STEP);
    await fixture.whenStable();
    expect(activeWord(fixture.nativeElement)).toBe('payment');
  });

  it('stops after one pass and rests on the first word', async () => {
    const fixture = await render();
    vi.advanceTimersByTime(STEP * 5);
    await fixture.whenStable();
    expect(activeWord(fixture.nativeElement)).toBe('liability');
    expect(vi.getTimerCount()).toBe(0);
  });

  it('keeps the word still under reduced motion', async () => {
    const fixture = await render({ reducedMotion: true });
    expect(vi.getTimerCount()).toBe(0);
    vi.advanceTimersByTime(STEP * 3);
    await fixture.whenStable();
    expect(activeWord(fixture.nativeElement)).toBe('liability');
  });

  it('clears the timer when the page is left mid-pass', async () => {
    const fixture = await render();
    vi.advanceTimersByTime(STEP);
    const running = vi.getTimerCount();
    fixture.destroy();
    // Only the rotation interval belongs to the page; the test environment may hold others.
    expect(vi.getTimerCount()).toBe(running - 1);
  });

  it('offers log in and sign up to visitors', async () => {
    const nav = (await render()).nativeElement.querySelector('header') as HTMLElement;
    expect(nav.textContent).toContain('Log in');
    expect(nav.textContent).toContain('Create an account');
    expect(nav.textContent).not.toContain('My contracts');
  });

  it('links signed-in users to their contracts', async () => {
    const nav = (await render({ signedIn: true })).nativeElement.querySelector('header') as HTMLElement;
    expect(nav.querySelector('a[href="/app"]')?.textContent).toContain('My contracts');
    expect(nav.textContent).not.toContain('Log in');
  });
});

describe('pathLength', () => {
  it.each([
    ['M 0 0 H 3 V 4', 7],
    ['M 0 0 L 3 4', 5],
    ['M 1 1', 1],
  ])('measures %s as %d', (d, expected) => {
    expect(pathLength(d)).toBe(expected);
  });
});
