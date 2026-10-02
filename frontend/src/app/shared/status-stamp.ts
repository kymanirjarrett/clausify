import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ContractStatus } from '../core/api.models';

const STAMPS: Record<ContractStatus, { label: string; tone: string; description: string }> = {
  UPLOADED: { label: 'Uploaded', tone: 'text-ink-2 bg-sheet', description: 'Uploaded; text extracted' },
  ANALYZING: { label: 'Analyzing', tone: 'text-blue-ink bg-blue-wash', description: 'Analysis in progress' },
  ANALYZED: { label: 'Analyzed', tone: 'text-pass bg-pass-wash', description: 'Analysis complete' },
  FAILED: { label: 'Failed', tone: 'text-fail bg-fail-wash', description: 'Analysis failed' },
};

/** A contract's status as a revision stamp. Text always carries the meaning; color only supports it. */
@Component({
  selector: 'cl-status-stamp',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="stamp" [class]="stamp().tone" [attr.title]="stamp().description">
      @if (inProgress()) {
        <span class="relative flex size-1.5" aria-hidden="true">
          <span class="absolute inline-flex size-full animate-ping rounded-full bg-current opacity-60"></span>
          <span class="relative inline-flex size-1.5 rounded-full bg-current"></span>
        </span>
      }
      {{ stamp().label }}
    </span>
  `,
})
export class StatusStamp {
  readonly status = input.required<ContractStatus>();
  protected readonly stamp = computed(() => STAMPS[this.status()]);
  protected readonly inProgress = computed(() => this.status() === 'ANALYZING');
}
