import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Long identifiers (filenames, emails) that may wrap only after _ . @ - so a line never splits
 * mid-word or mid-number on narrow screens.
 */
@Component({
  selector: 'cl-breakable',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'break-words' },
  template: `@for (part of parts(); track $index) {{{ part }}<wbr />}`,
})
export class Breakable {
  readonly text = input.required<string>();
  protected readonly parts = computed(() => this.text().split(/(?<=[_.@-])/));
}
