import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { toApiError } from '../../core/problem';
import { Icon } from '../../shared/icon';
import { AuthFrame } from './auth-frame';
import { FieldError } from './field-error';

/** Mirrors the API's RegisterRequest rules so mistakes show before submitting. */
export const PASSWORD_PATTERN = /^(?=.*\p{L})(?=.*\d).{8,72}$/u;
export const PASSWORD_RULE = 'Password must be at least 8 characters and include a letter and a number.';

@Component({
  selector: 'cl-signup',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, AuthFrame, FieldError, Icon],
  template: `
    <cl-auth-frame heading="Create your account">
      <p class="mt-2 text-[0.9375rem] leading-relaxed text-ink-2">
        Your contracts and their analyses are visible only to you.
      </p>

      @if (formError()) {
        <p role="alert" class="mt-6 flex items-start gap-2 rounded-[3px] border border-fail bg-fail-wash px-3.5 py-3 text-sm text-fail">
          <cl-icon name="alert" [size]="18" />{{ formError() }}
        </p>
      }

      <form class="mt-7 space-y-5" [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div>
          <label for="name" class="text-sm font-medium">Full name</label>
          <input id="name" class="field-input mt-1.5" formControlName="name" autocomplete="name"
                 [attr.aria-invalid]="!!errorFor('name')" [attr.aria-describedby]="errorFor('name') ? 'name-error' : null" />
          <cl-field-error id="name-error" [message]="errorFor('name')" />
        </div>
        <div>
          <label for="email" class="text-sm font-medium">Email</label>
          <input id="email" type="email" class="field-input mt-1.5" formControlName="email" autocomplete="email"
                 [attr.aria-invalid]="!!errorFor('email')" [attr.aria-describedby]="errorFor('email') ? 'email-error' : null" />
          <cl-field-error id="email-error" [message]="errorFor('email')" />
        </div>
        <div>
          <label for="password" class="text-sm font-medium">Password</label>
          <div class="relative mt-1.5">
            <input id="password" [type]="showPassword() ? 'text' : 'password'" class="field-input pr-12" formControlName="password"
                   autocomplete="new-password" aria-describedby="password-rule"
                   [attr.aria-invalid]="!!errorFor('password')" />
            <button type="button" class="btn btn-quiet absolute right-1 top-1/2 -translate-y-1/2 px-2"
                    (click)="showPassword.set(!showPassword())"
                    [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'" [attr.aria-pressed]="showPassword()">
              <cl-icon [name]="showPassword() ? 'eyeOff' : 'eye'" [size]="18" />
            </button>
          </div>
          @if (errorFor('password'); as message) {
            <cl-field-error id="password-rule" [message]="message" />
          } @else {
            <p id="password-rule" class="mt-1.5 text-sm text-ink-3">At least 8 characters, with a letter and a number.</p>
          }
        </div>
        <button type="submit" class="btn btn-primary w-full" [disabled]="pending()">
          {{ pending() ? 'Creating account…' : 'Create account' }}
        </button>
      </form>

      <p class="mt-7 border-t border-rule pt-5 text-sm text-ink-2">
        Already have an account? <a routerLink="/login" class="font-medium text-blue-ink underline">Log in</a>
      </p>
    </cl-auth-frame>
  `,
})
export class Signup {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
  });
  protected readonly pending = signal(false);
  protected readonly submitted = signal(false);
  protected readonly showPassword = signal(false);
  protected readonly formError = signal<string | null>(null);
  private readonly serverErrors = signal<Record<string, string>>({});

  protected errorFor(field: 'name' | 'email' | 'password'): string | null {
    const server = this.serverErrors()[field];
    if (server) return server;
    const control = this.form.controls[field];
    if (!this.submitted() || control.valid) return null;
    if (control.hasError('required')) {
      return { name: 'Enter your name.', email: 'Enter your email address.', password: 'Enter a password.' }[field];
    }
    if (field === 'email') return 'Enter a valid email address.';
    if (field === 'password') return PASSWORD_RULE;
    return 'Name must be 255 characters or fewer.';
  }

  protected submit(): void {
    this.submitted.set(true);
    this.serverErrors.set({});
    this.formError.set(null);
    if (this.form.invalid) {
      document.getElementById(['name', 'email', 'password'].find((f) => this.errorFor(f as never))!)?.focus();
      return;
    }
    this.pending.set(true);
    this.auth.register(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigate(['/app']),
      error: (error) => {
        const apiError = toApiError(error);
        this.pending.set(false);
        this.serverErrors.set(apiError.fieldErrors);
        if (!Object.keys(apiError.fieldErrors).length) this.formError.set(apiError.message);
      },
    });
  }
}
