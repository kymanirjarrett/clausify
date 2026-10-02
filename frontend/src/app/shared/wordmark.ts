import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** The mark: a dimension line with an amber callout point, followed by the name. */
@Component({
  selector: 'cl-wordmark',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'inline-flex items-center gap-2.5' },
  template: `
    <svg width="28" height="28" viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="5" [attr.fill]="inverse() ? '#F2F4F1' : '#1A1E22'" />
      <g [attr.stroke]="inverse() ? '#1A1E22' : '#F2F4F1'" [attr.fill]="inverse() ? '#1A1E22' : '#F2F4F1'"
         stroke-width="1.6" stroke-linejoin="round">
        <path d="M7 21h18M7 18v6M25 18v6" fill="none" />
        <path d="M10 21l-3-1.6v3.2zM22 21l3-1.6v3.2z" />
      </g>
      <path d="M21 10H13" stroke="#D7832A" stroke-width="1.6" />
      <circle cx="21" cy="10" r="2.4" fill="#D7832A" />
    </svg>
    <span class="text-[1.0625rem] font-semibold tracking-[-0.01em]">Clausify</span>
  `,
})
export class Wordmark {
  readonly inverse = input(false);
}
