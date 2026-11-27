import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { AnalysisResult, Finding, RiskLevel } from '../../core/api.models';
import { Icon } from '../../shared/icon';
import {
  LEVEL_FILTERS,
  LevelFilter,
  categoryLabel,
  filterFindings,
  formatDuration,
  gaugePercent,
  levelCounts,
  levelLabel,
} from './analysis-view';

/** FR-6: the analyzed contract's score, risk by category, and findings with filters and Copy. */
@Component({
  selector: 'cl-analysis-results',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  templateUrl: './analysis-results.html',
  styleUrl: './analysis-results.css',
})
export class AnalysisResults {
  readonly analysis = input.required<AnalysisResult>();

  protected readonly filters = LEVEL_FILTERS;
  protected readonly filter = signal<LevelFilter>('ALL');
  protected readonly counts = computed(() => levelCounts(this.analysis().findings));
  protected readonly shown = computed(() => filterFindings(this.analysis().findings, this.filter()));
  /** Finding id whose revision was just copied; drives the "Copied" confirmation. */
  protected readonly copied = signal<number | null>(null);
  protected readonly copyFailed = signal<number | null>(null);
  private copyTimer: ReturnType<typeof setTimeout> | undefined;

  protected readonly categoryLabel = categoryLabel;
  protected readonly levelLabel = levelLabel;
  protected readonly gaugePercent = gaugePercent;
  protected readonly formatDuration = formatDuration;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.copyTimer));
  }

  /** Stamp wording: High and Medium are out of tolerance (amber); Low is within it. */
  protected outOfTolerance(level: RiskLevel): boolean {
    return level !== 'LOW';
  }

  protected async copy(finding: Finding): Promise<void> {
    clearTimeout(this.copyTimer);
    try {
      await navigator.clipboard.writeText(finding.suggestedRevision ?? '');
      this.copyFailed.set(null);
      this.copied.set(finding.id);
    } catch {
      this.copied.set(null);
      this.copyFailed.set(finding.id);
    }
    this.copyTimer = setTimeout(() => {
      this.copied.set(null);
      this.copyFailed.set(null);
    }, 2500);
  }
}
