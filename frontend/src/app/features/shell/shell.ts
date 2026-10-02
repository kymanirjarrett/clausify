import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { Icon } from '../../shared/icon';
import { Wordmark } from '../../shared/wordmark';

@Component({
  selector: 'cl-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Wordmark, Icon],
  template: `
    <a href="#main" class="sr-only focus:not-sr-only focus:fixed focus:left-3 focus:top-3 focus:z-50 focus:bg-sheet focus:px-3 focus:py-2">
      Skip to content
    </a>
    <header class="sticky top-0 z-30 border-b border-rule bg-vellum/90 backdrop-blur-[6px]">
      <div class="mx-auto flex h-16 max-w-[76rem] items-center gap-6 px-5 sm:px-8">
        <a routerLink="/app" class="rounded-sm" aria-label="Clausify, my contracts"><cl-wordmark /></a>
        <nav class="flex items-center gap-1" aria-label="App">
          <a routerLink="/app" class="nav-link" [class.nav-active]="onContracts()"
             [attr.aria-current]="onContracts() ? 'page' : null">Contracts</a>
          <a routerLink="/app/account" routerLinkActive="nav-active" class="nav-link" ariaCurrentWhenActive="page">Account</a>
        </nav>
        <div class="ml-auto flex items-center gap-3">
          @if (auth.user(); as user) {
            <span class="hidden text-sm text-ink-2 md:inline">{{ user.name }}</span>
          }
          <button type="button" class="btn btn-quiet text-sm" (click)="auth.signOut()">
            <cl-icon name="signOut" [size]="18" /><span class="hidden sm:inline">Sign out</span>
            <span class="sr-only sm:hidden">Sign out</span>
          </button>
        </div>
      </div>
    </header>
    <main id="main" tabindex="-1" class="mx-auto w-full max-w-[76rem] px-5 pb-24 pt-10 outline-none sm:px-8">
      <router-outlet />
    </main>
  `,
  styles: `
    :host { display: block; min-height: 100dvh; }
    .nav-link { position: relative; padding: 0.45rem 0.7rem; font-size: 0.9375rem; color: var(--color-ink-2);
      border-radius: 3px; transition: color 160ms var(--ease-out-expo), background-color 160ms var(--ease-out-expo); }
    .nav-link:hover { color: var(--color-ink); background: var(--color-ghost); }
    .nav-active { color: var(--color-ink); font-weight: 550; }
    .nav-active::after { content: ''; position: absolute; left: 0.7rem; right: 0.7rem; bottom: -0.85rem; height: 2px;
      background: var(--color-blue); }
  `,
})
export class Shell {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly url = toSignal(this.router.events.pipe(
    filter((e) => e instanceof NavigationEnd),
    map(() => this.router.url),
    startWith(this.router.url),
  ), { requireSync: true });
  protected readonly onContracts = computed(() => {
    const path = this.url().split(/[?#]/)[0];
    return path === '/app' || path.startsWith('/app/contracts/');
  });
}
