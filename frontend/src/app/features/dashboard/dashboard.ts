import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ContractSummary, Page } from '../../core/api.models';
import { ContractsService, formatBytes } from '../../core/contracts.service';
import { toApiError } from '../../core/problem';
import { contractNumber, formatDateTime } from '../../shared/format';
import { Icon } from '../../shared/icon';
import { StatusStamp } from '../../shared/status-stamp';
import { UploadSheet } from './upload-sheet';
import { Breakable } from '../../shared/breakable';

const PAGE_SIZE = 20;
const POLL_MS = 5000;

/** FR-3 and FR-4: upload, and the register of the user's contracts with live status. */
@Component({
  selector: 'cl-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, StatusStamp, UploadSheet, Breakable],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly contracts = inject(ContractsService);

  protected readonly page = signal<Page<ContractSummary> | null>(null);
  protected readonly pageIndex = signal(0);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly justUploaded = signal<number | null>(null);
  protected readonly announcement = signal('');

  protected readonly rows = computed(() => this.page()?.content ?? []);
  protected readonly inProgress = computed(() =>
    this.rows().some((c) => c.status === 'UPLOADED' || c.status === 'ANALYZING'));
  protected readonly range = computed(() => {
    const p = this.page();
    if (!p || p.totalElements === 0) return '';
    const from = p.page * p.size + 1;
    return `${from}–${from + p.content.length - 1} of ${p.totalElements}`;
  });

  protected readonly contractNumber = contractNumber;
  protected readonly formatBytes = formatBytes;
  protected readonly formatDateTime = formatDateTime;

  private pollTimer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    this.load();
    const onVisible = () => { if (!document.hidden) this.schedulePoll(); };
    document.addEventListener('visibilitychange', onVisible);
    inject(DestroyRef).onDestroy(() => {
      clearTimeout(this.pollTimer);
      document.removeEventListener('visibilitychange', onVisible);
    });
  }

  protected load(quiet = false): void {
    if (!quiet) {
      this.loading.set(true);
      this.error.set(null);
    }
    this.contracts.list(this.pageIndex(), PAGE_SIZE).subscribe({
      next: (page) => {
        this.announceChanges(this.page(), page);
        this.page.set(page);
        this.loading.set(false);
        this.error.set(null);
        this.schedulePoll();
      },
      error: (error) => {
        this.loading.set(false);
        if (!quiet) this.error.set(toApiError(error).message);
      },
    });
  }

  protected goTo(index: number): void {
    this.pageIndex.set(index);
    this.load();
  }

  protected onUploaded(contract: ContractSummary): void {
    this.justUploaded.set(contract.id);
    this.announcement.set(`${contract.filename} uploaded and queued for analysis.`);
    this.pageIndex.set(0);
    this.load(true);
  }

  /** Refresh while any visible contract is still in progress and the tab is visible. */
  private schedulePoll(): void {
    clearTimeout(this.pollTimer);
    if (this.inProgress() && !document.hidden) {
      this.pollTimer = setTimeout(() => this.load(true), POLL_MS);
    }
  }

  private announceChanges(before: Page<ContractSummary> | null, after: Page<ContractSummary>): void {
    if (!before) return;
    const previous = new Map(before.content.map((c) => [c.id, c.status]));
    const changed = after.content.find((c) => previous.has(c.id) && previous.get(c.id) !== c.status);
    if (changed) {
      const words = { UPLOADED: 'uploaded', ANALYZING: 'being analyzed', ANALYZED: 'analyzed', FAILED: 'failed' };
      this.announcement.set(`${changed.filename} is now ${words[changed.status]}.`);
    }
  }
}
