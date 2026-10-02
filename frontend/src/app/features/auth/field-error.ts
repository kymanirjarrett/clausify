import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'cl-field-error',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  template: `
    @if (message()) {
      <p [id]="id()" class="mt-1.5 flex items-start gap-1.5 text-sm text-fail">
        <cl-icon name="alert" [size]="16" class="mt-0.5" />{{ message() }}
      </p>
    }
  `,
})
export class FieldError {
  readonly id = input.required<string>();
  readonly message = input<string | null | undefined>();
}
