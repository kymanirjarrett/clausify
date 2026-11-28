import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Comparison } from '../../core/api.models';
import { ContractsService } from '../../core/contracts.service';
import { ApiError, toApiError } from '../../core/problem';
import { Breakable } from '../../shared/breakable';
import { contractNumber } from '../../shared/format';
import { Icon } from '../../shared/icon';
import { categoryLabel, levelLabel } from '../contract/analysis-view';
import { categoryChangeLabel, fold, scoreChangeLabel, worsened } from './compare-view';

/** FR-10: an original and a revised contract side by side, with the risk change and the text that changed. */
@Component({
  selector: 'cl-compare',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, Breakable],
  templateUrl: './compare.html',
  styleUrl: './compare.css',
})
export class ComparePage {
  /** Route parameters, bound by withComponentInputBinding. */
  readonly id = input.required<string>();
  readonly otherId = input.required<string>();

  private readonly contracts = inject(ContractsService);
  protected readonly comparison = signal<Comparison | null>(null);
  protected readonly error = signal<ApiError | null>(null);
  /** FR-10 example 2: unrelated documents show a warning first; the diff waits for this. */
  protected readonly showAnyway = signal(false);
  /** Indexes of unchanged stretches the reader expanded. */
  protected readonly expanded = signal<ReadonlySet<number>>(new Set());

  protected readonly contractNumber = contractNumber;
  protected readonly categoryLabel = categoryLabel;
  protected readonly levelLabel = levelLabel;
  protected readonly scoreChangeLabel = scoreChangeLabel;
  protected readonly categoryChangeLabel = categoryChangeLabel;
  protected readonly worsened = worsened;
  protected readonly fold = fold;

  constructor() {
    queueMicrotask(() => this.load());
  }

  protected load(): void {
    this.error.set(null);
    this.contracts.compare(Number(this.id()), Number(this.otherId())).subscribe({
      next: (c) => this.comparison.set(c),
      error: (e) => this.error.set(toApiError(e)),
    });
  }

  protected expand(index: number): void {
    this.expanded.update((set) => new Set(set).add(index));
  }
}
