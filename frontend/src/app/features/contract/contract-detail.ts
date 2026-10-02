import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContractDetail } from '../../core/api.models';
import { ContractsService, formatBytes } from '../../core/contracts.service';
import { ApiError, toApiError } from '../../core/problem';
import { contractNumber, formatDateTime } from '../../shared/format';
import { Icon } from '../../shared/icon';
import { StatusStamp } from '../../shared/status-stamp';
import { Breakable } from '../../shared/breakable';

const POLL_MS = 4000;

interface Revision {
  rev: string;
  label: string;
  state: 'done' | 'current' | 'absent' | 'failed';
  note: string;
}

/** FR-4: one contract's status and details. Analysis results (FR-6) render here once the API provides them. */
@Component({
  selector: 'cl-contract-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, StatusStamp, Breakable],
  templateUrl: './contract-detail.html',
  styleUrl: './contract-detail.css',
})
export class ContractDetailPage {
  /** Route parameter, bound by withComponentInputBinding. */
  readonly id = input.required<string>();

  private readonly contracts = inject(ContractsService);
  protected readonly contract = signal<ContractDetail | null>(null);
  protected readonly error = signal<ApiError | null>(null);
  private timer: ReturnType<typeof setTimeout> | undefined;

  protected readonly contractNumber = contractNumber;
  protected readonly formatBytes = formatBytes;
  protected readonly formatDateTime = formatDateTime;

  /** Revision block rows: what has happened to this contract, in order. */
  protected readonly revisions = computed<Revision[]>(() => {
    const c = this.contract();
    if (!c) return [];
    const analysis: Record<ContractDetail['status'], Revision> = {
      UPLOADED: { rev: 'B', label: 'Analysis', state: 'absent', note: 'Not active in this release' },
      ANALYZING: { rev: 'B', label: 'Analysis', state: 'current', note: 'In progress' },
      ANALYZED: { rev: 'B', label: 'Analysis', state: 'done', note: formatDateTime(c.analyzedAt) },
      FAILED: { rev: 'B', label: 'Analysis failed', state: 'failed', note: 'See the reason below' },
    };
    return [
      { rev: 'A', label: 'Uploaded, text extracted', state: 'done', note: formatDateTime(c.uploadedAt) },
      analysis[c.status],
    ];
  });

  constructor() {
    queueMicrotask(() => this.load());
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timer));
  }

  protected load(): void {
    const id = Number(this.id());
    if (!Number.isInteger(id) || id <= 0) {
      this.error.set({ status: 404, message: 'Contract not found.', fieldErrors: {} });
      return;
    }
    this.contracts.get(id).subscribe({
      next: (contract) => {
        this.contract.set(contract);
        this.error.set(null);
        clearTimeout(this.timer);
        if (contract.status === 'UPLOADED' || contract.status === 'ANALYZING') {
          this.timer = setTimeout(() => this.load(), POLL_MS);
        }
      },
      error: (error) => this.error.set(toApiError(error)),
    });
  }
}
