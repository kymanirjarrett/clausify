import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Wordmark } from '../../shared/wordmark';

/** Shared frame for sign-up and login: one drawing sheet on plain vellum. */
@Component({
  selector: 'cl-auth-frame',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Wordmark],
  host: { class: 'block' },
  template: `
    <main id="main" class="grid-ground flex min-h-[100svh] flex-col items-center px-5 py-10 sm:justify-center">
      <a routerLink="/" class="mb-8 rounded-sm" aria-label="Clausify home"><cl-wordmark /></a>
      <section class="sheet w-full max-w-[27rem] px-6 pb-8 pt-7 sm:px-9 sm:pb-10 sm:pt-9" [attr.aria-labelledby]="'auth-title'">
        <h1 id="auth-title" class="text-[1.75rem] font-semibold leading-tight tracking-[-0.025em]">{{ heading() }}</h1>
        <ng-content />
      </section>
      <p class="mt-8 max-w-[27rem] text-center text-xs leading-relaxed text-ink-3">
        Clausify provides information, not legal advice.
      </p>
    </main>
  `,
  styles: `.grid-ground { background-color: var(--color-vellum); }`,
})
export class AuthFrame {
  readonly heading = input.required<string>();
}
