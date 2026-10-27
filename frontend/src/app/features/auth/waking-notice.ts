import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Shown while a sign-in or sign-up waits on the API waking from Render's free-tier sleep. */
@Component({
  selector: 'cl-waking-notice',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (show()) {
      <p role="status" class="mt-5 rounded-[3px] border border-blue bg-blue-wash px-3.5 py-3 text-sm text-blue-ink">
        Waking up the server. Clausify's free hosting sleeps when it's idle, so this can take up to two minutes.
      </p>
    }
  `,
})
export class WakingNotice {
  readonly show = input(false);
}
