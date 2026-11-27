import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PHONE_PATTERN, AccountService, accountChanges } from '../../core/account.service';
import { AccountDetails, AccountStats, ContractSummary } from '../../core/api.models';
import { AuthService } from '../../core/auth.service';
import { ContractsService } from '../../core/contracts.service';
import { toApiError } from '../../core/problem';
import { Breakable } from '../../shared/breakable';
import { contractNumber, formatDate } from '../../shared/format';
import { Icon } from '../../shared/icon';
import { StatusStamp } from '../../shared/status-stamp';
import { FieldError } from '../auth/field-error';

const RECENT = 5;

/** FR-12: profile details with Save, statistics, and the most recent uploads. */
@Component({
  selector: 'cl-account',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon, Breakable, ReactiveFormsModule, FieldError, RouterLink, StatusStamp],
  template: `
    <h1 class="text-[2rem] font-semibold leading-tight tracking-[-0.03em]">Account</h1>

    @if (account(); as a) {
      <form class="mt-8 max-w-[44rem] border-t border-ink" [formGroup]="form" (ngSubmit)="save(a)" novalidate>
        <div class="row">
          <label for="name">Name</label>
          <div>
            <input id="name" class="field-input" formControlName="name" autocomplete="name"
                   [attr.aria-invalid]="!!errorFor('name')" [attr.aria-describedby]="errorFor('name') ? 'name-error' : null" />
            <cl-field-error id="name-error" [message]="errorFor('name')" />
          </div>
        </div>
        <div class="row">
          <span class="label" id="email-label">Email</span>
          <div aria-labelledby="email-label">
            <p class="pt-2 font-medium"><cl-breakable [text]="a.email" /></p>
            <p class="mt-1 text-sm text-ink-3">Your sign-in address cannot be changed here.</p>
          </div>
        </div>
        <div class="row">
          <label for="phone">Phone</label>
          <div>
            <input id="phone" type="tel" class="field-input" formControlName="phoneNumber" autocomplete="tel"
                   placeholder="Optional"
                   [attr.aria-invalid]="!!errorFor('phoneNumber')"
                   [attr.aria-describedby]="errorFor('phoneNumber') ? 'phone-error' : null" />
            <cl-field-error id="phone-error" [message]="errorFor('phoneNumber')" />
          </div>
        </div>
        <div class="flex flex-wrap items-center gap-4 pt-5">
          <button type="submit" class="btn btn-primary" [disabled]="saving()">{{ saving() ? 'Saving…' : 'Save' }}</button>
          <p class="text-sm" role="status" [class.text-pass]="saved()" [class.text-fail]="!!saveError()">
            @if (saved()) { Saved. } @else if (saveError()) { {{ saveError() }} }
          </p>
        </div>
      </form>
    } @else if (loadError(); as message) {
      <div class="mt-8 max-w-[44rem]" role="alert">
        <p class="font-medium">Your account could not be loaded.</p>
        <p class="mt-1 text-ink-2">{{ message }}</p>
        <button type="button" class="btn btn-secondary mt-4" (click)="load()"><cl-icon name="refresh" [size]="18" /> Try again</button>
      </div>
    } @else {
      <div class="ghost mt-8 h-24 max-w-[44rem]" aria-busy="true" aria-label="Loading account"></div>
    }

    <section class="mt-12 max-w-[44rem]" aria-labelledby="stats-title">
      <h2 id="stats-title" class="border-b border-ink pb-3 text-lg font-semibold">Statistics</h2>
      @if (stats(); as s) {
        <dl class="title-block mt-5 grid grid-cols-3">
          <div><dt>Uploaded</dt><dd class="tabular">{{ s.contractsUploaded }}</dd></div>
          <div><dt>Analyzed</dt><dd class="tabular">{{ s.contractsAnalyzed }}</dd></div>
          <div>
            <dt>Average risk</dt>
            <dd class="tabular">{{ s.averageRiskScore ?? '—' }}@if (s.averageRiskScore !== null) {<span class="measure ml-1 text-ink-3">/ 100</span>}</dd>
          </div>
        </dl>
        @if (s.averageRiskScore === null) {
          <p class="mt-3 text-sm text-ink-3">The average appears after your first analysis.</p>
        }
      } @else {
        <div class="ghost mt-5 h-20 rounded-[3px]" aria-busy="true" aria-label="Loading statistics"></div>
      }
    </section>

    <section class="mt-12 max-w-[44rem]" aria-labelledby="recent-title">
      <div class="flex items-baseline justify-between border-b border-ink pb-3">
        <h2 id="recent-title" class="text-lg font-semibold">Recent contracts</h2>
        <a routerLink="/app" class="text-sm text-ink-2 hover:text-ink">All contracts</a>
      </div>
      @if (recent(); as list) {
        @if (list.length) {
          <ul>
            @for (c of list; track c.id) {
              <li class="border-b border-rule">
                <a [routerLink]="['/app/contracts', c.id]" class="recent-row">
                  <span class="min-w-0 font-medium"><cl-breakable [text]="c.filename" /></span>
                  <span class="measure text-ink-3">{{ contractNumber(c.id) }} · {{ formatDate(c.uploadedAt) }}</span>
                  <cl-status-stamp [status]="c.status" />
                </a>
              </li>
            }
          </ul>
        } @else {
          <p class="mt-4 text-ink-2">No contracts yet. Upload one from the Contracts page.</p>
        }
      } @else {
        <div class="ghost mt-4 h-16 rounded-[3px]" aria-busy="true" aria-label="Loading recent contracts"></div>
      }
    </section>

    <section class="mt-12 max-w-[44rem]" aria-labelledby="privacy-title">
      <h2 id="privacy-title" class="text-lg font-semibold">Your data</h2>
      <ul class="mt-4 space-y-3 leading-relaxed text-ink-2">
        <li class="flex gap-2.5"><cl-icon name="lock" [size]="18" class="mt-0.5 text-blue-ink" />Uploaded PDFs are read in memory and never stored. Only the extracted text is kept, and it is sent to Groq, a third-party AI provider, for analysis.</li>
        <li class="flex gap-2.5"><cl-icon name="lock" [size]="18" class="mt-0.5 text-blue-ink" />Your contracts and analyses are visible only to your account.</li>
      </ul>
    </section>

    <button type="button" class="btn btn-secondary mt-12" (click)="auth.signOut()">
      <cl-icon name="signOut" [size]="18" /> Sign out
    </button>
  `,
  styles: `
    :host { display: block; }
    .row { display: grid; grid-template-columns: 8rem minmax(0, 1fr); gap: 1rem; padding: 0.95rem 0.25rem; border-bottom: 1px solid var(--color-rule); }
    .row > label, .row > .label { font-family: var(--font-mono); font-size: 0.6875rem; letter-spacing: 0.08em; text-transform: uppercase;
      color: var(--color-ink-3); padding-top: 0.85rem; }
    @media (max-width: 479px) { .row { grid-template-columns: minmax(0, 1fr); gap: 0.4rem; } .row > label, .row > .label { padding-top: 0; } }
    .title-block { border: 1px solid var(--color-rule-strong); background: var(--color-sheet); overflow: hidden; }
    .title-block > div { padding: 0.8rem 1rem; margin: -1px 0 0 -1px; border-left: 1px solid var(--color-rule); border-top: 1px solid var(--color-rule); }
    .title-block dt { font-family: var(--font-mono); font-size: 0.6875rem; letter-spacing: 0.08em; text-transform: uppercase; color: var(--color-ink-3); }
    .title-block dd { margin-top: 0.3rem; font-size: 1.5rem; font-weight: 600; }
    .recent-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 0.25rem 1rem; align-items: center; padding: 0.85rem 0.25rem; }
    .recent-row:hover { background: var(--color-sheet); }
    .recent-row > :nth-child(2) { grid-row: 2; }
    .recent-row > cl-status-stamp { grid-row: 1 / span 2; grid-column: 2; }
  `,
})
export class Account {
  protected readonly auth = inject(AuthService);
  private readonly accounts = inject(AccountService);
  private readonly contracts = inject(ContractsService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly account = signal<AccountDetails | null>(null);
  protected readonly loadError = signal<string | null>(null);
  protected readonly stats = signal<AccountStats | null>(null);
  protected readonly recent = signal<ContractSummary[] | null>(null);
  protected readonly saving = signal(false);
  protected readonly saved = signal(false);
  protected readonly saveError = signal<string | null>(null);
  private readonly serverErrors = signal<Record<string, string>>({});
  private readonly submitted = signal(false);

  protected readonly contractNumber = contractNumber;
  protected readonly formatDate = formatDate;

  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255), Validators.pattern(/\S/)]],
    phoneNumber: ['', [Validators.maxLength(15), Validators.pattern(PHONE_PATTERN)]],
  });

  constructor() {
    this.load();
    this.accounts.stats().subscribe({ next: (s) => this.stats.set(s), error: () => this.stats.set(null) });
    this.contracts.list(0, RECENT).subscribe({ next: (page) => this.recent.set(page.content), error: () => this.recent.set([]) });
    this.form.valueChanges.subscribe(() => {
      this.saved.set(false);
      this.serverErrors.set({});
    });
  }

  protected load(): void {
    this.loadError.set(null);
    this.accounts.get().subscribe({
      next: (a) => this.show(a),
      error: (error) => this.loadError.set(toApiError(error).message),
    });
  }

  protected errorFor(field: 'name' | 'phoneNumber'): string | null {
    const server = this.serverErrors()[field];
    if (server) return server;
    const control = this.form.controls[field];
    if (!this.submitted() || control.valid) return null;
    if (field === 'name') {
      return control.hasError('maxlength') ? 'Name must be 255 characters or fewer.' : 'Enter your name.';
    }
    return 'Enter 7 to 15 digits; spaces and + ( ) . - are allowed.';
  }

  protected save(saved: AccountDetails): void {
    this.submitted.set(true);
    this.saveError.set(null);
    if (this.form.invalid) {
      document.getElementById(this.errorFor('name') ? 'name' : 'phone')?.focus();
      return;
    }
    const { name, phoneNumber } = this.form.getRawValue();
    const changes = accountChanges(saved, name, phoneNumber);
    if (Object.keys(changes).length === 0) {
      this.saved.set(true);
      return;
    }
    this.saving.set(true);
    this.accounts.update(changes).subscribe({
      next: (a) => {
        this.saving.set(false);
        this.show(a);
        this.auth.updateUser({ id: a.id, name: a.name, email: a.email });
        this.saved.set(true);
      },
      error: (error) => {
        this.saving.set(false);
        const apiError = toApiError(error);
        this.serverErrors.set(apiError.fieldErrors);
        this.saveError.set(apiError.message);
      },
    });
  }

  private show(a: AccountDetails): void {
    this.account.set(a);
    this.form.setValue({ name: a.name, phoneNumber: a.phoneNumber ?? '' }, { emitEvent: false });
    this.submitted.set(false);
  }
}
