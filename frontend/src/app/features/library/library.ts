import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RiskCategory, StandardClause } from '../../core/api.models';
import { ClausesService } from '../../core/clauses.service';
import { toApiError } from '../../core/problem';
import { categoryLabel } from '../contract/analysis-view';

const CATEGORIES: readonly RiskCategory[] = [
  'PAYMENT_TERMS', 'LIABILITY', 'INDEMNIFICATION', 'IP_RIGHTS', 'TERMINATION', 'NON_COMPETE', 'CONFIDENTIALITY', 'JURISDICTION',
];

/** FR-9 example 2: browse standard clauses, filtered by category, with fair and unfair wording side by side. */
@Component({
  selector: 'cl-library',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-[2rem] font-semibold leading-tight tracking-[-0.03em]">Clause library</h1>
    <p class="mt-3 max-w-[44rem] leading-relaxed text-ink-2">
      Standard wording for each risk category: favorable clauses protect the freelancer, unfavorable ones are what to
      push back on. Use them to judge your contract's terms and to propose fairer language.
    </p>

    <div class="mt-8 flex flex-wrap gap-1.5" role="group" aria-label="Filter by category">
      <button type="button" class="chip" [attr.aria-pressed]="category() === null" (click)="category.set(null)">All</button>
      @for (c of categories; track c) {
        <button type="button" class="chip" [attr.aria-pressed]="category() === c" (click)="category.set(c)">{{ categoryLabel(c) }}</button>
      }
    </div>

    @if (error(); as message) {
      <p class="mt-8 text-fail" role="alert">{{ message }}</p>
    } @else if (clauses(); as all) {
      <div class="mt-8 grid gap-10 lg:grid-cols-2">
        @for (group of groups(); track group.favorable) {
          <section [attr.aria-labelledby]="'group-' + group.favorable">
            <h2 [id]="'group-' + group.favorable" class="border-b border-ink pb-3 text-lg font-semibold">
              {{ group.favorable ? 'Favorable' : 'Unfavorable' }} <span class="measure text-ink-3">· {{ group.items.length }}</span>
            </h2>
            <ul>
              @for (c of group.items; track c.id) {
                <li class="border-b border-rule py-4">
                  <div class="flex flex-wrap items-center gap-x-3 gap-y-1">
                    <span class="stamp" [class]="c.favorable ? 'text-pass bg-pass-wash' : 'text-amber-ink bg-amber-wash'">
                      {{ c.favorable ? 'Favorable' : 'Unfavorable' }}
                    </span>
                    <span class="measure text-ink-3">{{ categoryLabel(c.category) }}</span>
                  </div>
                  <h3 class="mt-2 font-semibold">{{ c.title }}</h3>
                  <p class="mt-1.5 leading-relaxed">{{ c.text }}</p>
                  @if (c.notes) {
                    <p class="mt-1.5 text-sm text-ink-2">{{ c.notes }}</p>
                  }
                </li>
              }
            </ul>
          </section>
        }
      </div>
    } @else {
      <div class="ghost mt-8 h-40 rounded-[3px]" aria-busy="true" aria-label="Loading the clause library"></div>
    }
  `,
  styles: `
    :host { display: block; }
    .chip { display: inline-flex; align-items: center; height: 2rem; padding: 0 0.7rem; border: 1px solid var(--color-rule-strong);
      border-radius: 3px; font-size: 0.875rem; font-weight: 500; color: var(--color-ink-2); background: transparent; }
    .chip:hover { background: var(--color-ghost); color: var(--color-ink); }
    .chip[aria-pressed='true'] { background: var(--color-ink); border-color: var(--color-ink); color: var(--color-sheet); }
  `,
})
export class Library {
  private readonly clausesService = inject(ClausesService);
  protected readonly categories = CATEGORIES;
  protected readonly categoryLabel = categoryLabel;
  protected readonly category = signal<RiskCategory | null>(null);
  protected readonly all = signal<StandardClause[] | null>(null);
  protected readonly error = signal<string | null>(null);

  /** The whole library is small (about 100 clauses), so it loads once and filters in the browser. */
  protected readonly clauses = computed(() => {
    const all = this.all();
    const category = this.category();
    return all && (category ? all.filter((c) => c.category === category) : all);
  });

  protected readonly groups = computed(() => {
    const clauses = this.clauses() ?? [];
    return [true, false].map((favorable) => ({ favorable, items: clauses.filter((c) => c.favorable === favorable) }));
  });

  constructor() {
    this.clausesService.library().subscribe({
      next: (clauses) => this.all.set(clauses),
      error: (error) => this.error.set(toApiError(error).message),
    });
  }
}
