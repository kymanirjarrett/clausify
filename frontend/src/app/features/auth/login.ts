import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { toApiError } from '../../core/problem';
import { Icon } from '../../shared/icon';
import { AuthFrame } from './auth-frame';
import { FieldError } from './field-error';

@Component({
  selector: 'cl-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, RouterLink, AuthFrame, FieldError, Icon],
  template: `
    <cl-auth-frame heading="Log in">
      @if (expired) {
        <p role="status" class="mt-5 rounded-[3px] border border-blue bg-blue-wash px-3.5 py-3 text-sm text-blue-ink">
          Your session has ended. Log in again to continue.
        </p>
      }
      @if (formError()) {
        <p role="alert" class="mt-5 flex items-start gap-2 rounded-[3px] border border-fail bg-fail-wash px-3.5 py-3 text-sm text-fail">
          <cl-icon name="alert" [size]="18" />{{ formError() }}
        </p>
      }

      <form class="mt-7 space-y-5" [formGroup]="form" (ngSubmit)="submit()" novalidate>
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
                   autocomplete="current-password"
                   [attr.aria-invalid]="!!errorFor('password')" [attr.aria-describedby]="errorFor('password') ? 'password-error' : null" />
            <button type="button" class="btn btn-quiet absolute right-1 top-1/2 -translate-y-1/2 px-2"
                    (click)="showPassword.set(!showPassword())"
                    [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'" [attr.aria-pressed]="showPassword()">
              <cl-icon [name]="showPassword() ? 'eyeOff' : 'eye'" [size]="18" />
            </button>
          </div>
          <cl-field-error id="password-error" [message]="errorFor('password')" />
        </div>
        <button type="submit" class="btn btn-primary w-full" [disabled]="pending()">
          {{ pending() ? 'Logging in…' : 'Log in' }}
        </button>
      </form>

      <p class="mt-7 border-t border-rule pt-5 text-sm text-ink-2">
        New to Clausify? <a routerLink="/signup" class="font-medium text-blue-ink underline">Create an account</a>
      </p>
    </cl-auth-frame>
  `,
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly expired = this.route.snapshot.queryParamMap.get('reason') === 'expired';
  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  protected readonly pending = signal(false);
  protected readonly submitted = signal(false);
  protected readonly showPassword = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected errorFor(field: 'email' | 'password'): string | null {
    const control = this.form.controls[field];
    if (!this.submitted() || control.valid) return null;
    if (field === 'password') return 'Enter your password.';
    return control.hasError('required') ? 'Enter your email address.' : 'Enter a valid email address.';
  }

  protected submit(): void {
    this.submitted.set(true);
    this.formError.set(null);
    if (this.form.invalid) {
      document.getElementById(this.errorFor('email') ? 'email' : 'password')?.focus();
      return;
    }
    this.pending.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => void this.router.navigateByUrl(this.safeReturnUrl()),
      error: (error) => {
        this.pending.set(false);
        this.formError.set(toApiError(error).message);
      },
    });
  }

  /** Only same-app paths, so a crafted link cannot send the user elsewhere after login. */
  private safeReturnUrl(): string {
    const url = this.route.snapshot.queryParamMap.get('returnUrl');
    return url && url.startsWith('/app') ? url : '/app';
  }
}
