import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AnalysisResult, ContractDetail, ContractSummary } from '../../core/api.models';
import { ContractsService, downloadName, formatBytes } from '../../core/contracts.service';
import { ApiError, toApiError } from '../../core/problem';
import { contractNumber, formatDateTime } from '../../shared/format';
import { Icon } from '../../shared/icon';
import { StatusStamp } from '../../shared/status-stamp';
import { Breakable } from '../../shared/breakable';
import { AnalysisResults } from './analysis-results';

const POLL_MS = 4000;

interface Revision {
  rev: string;
  label: string;
  state: 'done' | 'current' | 'absent' | 'failed';
  note: string;
}

/** FR-4: one contract's status and details; FR-6: its analysis results once the status is ANALYZED. */
@Component({
  selector: 'cl-contract-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, StatusStamp, Breakable, AnalysisResults],
  templateUrl: './contract-detail.html',
  styleUrl: './contract-detail.css',
})
export class ContractDetailPage {
  /** Route parameter, bound by withComponentInputBinding. */
  readonly id = input.required<string>();

  private readonly contracts = inject(ContractsService);
  protected readonly contract = signal<ContractDetail | null>(null);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly analysis = signal<AnalysisResult | null>(null);
  protected readonly analysisError = signal<ApiError | null>(null);
  /** FR-10: the user's other analyzed contracts, offered under "Compare with…". */
  protected readonly compareOptions = signal<ContractSummary[]>([]);
  protected readonly compareWith = signal<number | null>(null);
  private readonly router = inject(Router);
  protected readonly downloading = signal(false);
  protected readonly downloadError = signal<string | null>(null);
  protected readonly retrying = signal(false);
  protected readonly retryError = signal<string | null>(null);
  private timer: ReturnType<typeof setTimeout> | undefined;

  protected readonly contractNumber = contractNumber;
  protected readonly formatBytes = formatBytes;
  protected readonly formatDateTime = formatDateTime;

  /** Revision block rows: what has happened to this contract, in order. */
  protected readonly revisions = computed<Revision[]>(() => {
    const c = this.contract();
    if (!c) return [];
    const analysis: Record<ContractDetail['status'], Revision> = {
      UPLOADED: { rev: 'B', label: 'Analysis', state: 'absent', note: 'Queued' },
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
        } else if (contract.status === 'ANALYZED' && !this.analysis()) {
          this.loadAnalysis(contract.id);
        }
      },
      error: (error) => this.error.set(toApiError(error)),
    });
  }

  /** FR-7: analyze the stored text again. The status moves to Uploaded, and polling follows it from there. */
  protected retry(id: number): void {
    this.retrying.set(true);
    this.retryError.set(null);
    this.contracts.retry(id).subscribe({
      next: () => {
        this.retrying.set(false);
        this.load();
      },
      error: (error) => {
        this.retrying.set(false);
        this.retryError.set(toApiError(error).message);
      },
    });
  }

  private loadCompareOptions(id: number): void {
    this.contracts.list(0, 100).subscribe({
      next: (page) => this.compareOptions.set(page.content.filter((c) => c.id !== id && c.status === 'ANALYZED')),
      error: () => this.compareOptions.set([]),
    });
  }

  protected compare(id: number): void {
    const otherId = this.compareWith();
    if (otherId !== null) {
      void this.router.navigate(['/app/contracts', id, 'compare', otherId]);
    }
  }

  /** FR-11: fetch the PDF with the auth header, then hand it to the browser as a file download. */
  protected downloadReport(contract: ContractDetail): void {
    this.downloading.set(true);
    this.downloadError.set(null);
    this.contracts.report(contract.id).subscribe({
      next: (response) => {
        this.downloading.set(false);
        const url = URL.createObjectURL(response.body!);
        const link = document.createElement('a');
        link.href = url;
        link.download = downloadName(response.headers.get('Content-Disposition'), 'Clausify_Report.pdf');
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      error: (error) => {
        this.downloading.set(false);
        this.downloadError.set(toApiError(error).message);
      },
    });
  }

  protected loadAnalysis(id: number): void {
    this.analysisError.set(null);
    this.contracts.analysis(id).subscribe({
      next: (analysis) => {
        this.analysis.set(analysis);
        this.loadCompareOptions(id);
      },
      error: (error) => this.analysisError.set(toApiError(error)),
    });
  }
}
