import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AuthService } from '../../core/auth.service';
import { Breakable } from '../../shared/breakable';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'cl-account',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon, Breakable],
  template: `
    <h1 class="text-[2rem] font-semibold leading-tight tracking-[-0.03em]">Account</h1>

    @if (auth.user(); as user) {
      <dl class="mt-8 max-w-[44rem] border-t border-ink">
        <div class="row"><dt>Name</dt><dd>{{ user.name }}</dd></div>
        <div class="row"><dt>Email</dt><dd><cl-breakable [text]="user.email" /></dd></div>
      </dl>
    } @else {
      <div class="ghost mt-8 h-24 max-w-[44rem]" aria-busy="true" aria-label="Loading account"></div>
    }

    <section class="mt-12 max-w-[44rem]" aria-labelledby="privacy-title">
      <h2 id="privacy-title" class="text-lg font-semibold">Your data</h2>
      <ul class="mt-4 space-y-3 leading-relaxed text-ink-2">
        <li class="flex gap-2.5"><cl-icon name="lock" [size]="18" class="mt-0.5 text-blue-ink" />Uploaded PDFs are read in memory and never stored. Only the extracted text is kept.</li>
        <li class="flex gap-2.5"><cl-icon name="lock" [size]="18" class="mt-0.5 text-blue-ink" />Your contracts and analyses are visible only to your account.</li>
      </ul>
    </section>

    <section class="mt-12 max-w-[44rem]" aria-labelledby="history-title">
      <div class="flex items-baseline justify-between border-b border-ink pb-3">
        <h2 id="history-title" class="text-lg font-semibold">Statistics</h2>
        <p class="measure text-ink-3">Not yet available</p>
      </div>
      <div class="mt-5 grid grid-cols-2 gap-4">
        <div class="ghost grid h-20 place-items-center rounded-[3px]"><span class="measure text-ink-3">Contracts analyzed</span></div>
        <div class="ghost grid h-20 place-items-center rounded-[3px]"><span class="measure text-ink-3">Average risk</span></div>
      </div>
      <p class="mt-4 text-sm text-ink-3">Contracts analyzed and average risk will appear here in a later release.</p>
    </section>

    <button type="button" class="btn btn-secondary mt-12" (click)="auth.signOut()">
      <cl-icon name="signOut" [size]="18" /> Sign out
    </button>
  `,
  styles: `
    :host { display: block; }
    .row { display: grid; grid-template-columns: 8rem minmax(0, 1fr); gap: 1rem; padding: 0.95rem 0.25rem; border-bottom: 1px solid var(--color-rule); }
    dt { font-family: var(--font-mono); font-size: 0.6875rem; letter-spacing: 0.08em; text-transform: uppercase; color: var(--color-ink-3); padding-top: 0.15rem; }
    dd { font-weight: 500; }
  `,
})
export class Account {
  protected readonly auth = inject(AuthService);
}
