import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, afterNextRender, inject, signal, viewChild,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { Icon } from '../../shared/icon';
import { Wordmark } from '../../shared/wordmark';
import { HeroField } from './hero-field';

interface Category {
  name: string;
  checks: string;
}

@Component({
  selector: 'cl-landing',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, NgTemplateOutlet, Icon, Wordmark, HeroField],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
})
export class Landing {
  protected readonly auth = inject(AuthService);

  private readonly plate = viewChild.required<ElementRef<HTMLElement>>('plate');
  private readonly marker = viewChild.required<ElementRef<HTMLElement>>('marker');
  private readonly callout = viewChild.required<ElementRef<HTMLElement>>('callout');

  /** The leader line from the flagged phrase to its revision, measured from the live layout. */
  protected readonly leader = signal<{ d: string; length: number } | null>(null);
  protected readonly anchorEl = signal<HTMLElement | undefined>(undefined);
  protected readonly linked = signal(false);

  protected readonly categories: Category[] = [
    { name: 'Payment terms', checks: 'When and how you are paid, late fees, and how invoices may be disputed.' },
    { name: 'Liability', checks: 'Whether there is a ceiling on what you could owe, and which damages count.' },
    { name: 'Indemnification', checks: 'Whose losses you agree to cover, and whether fault matters.' },
    { name: 'IP rights', checks: 'What you transfer to the client, and what you keep, including prior work.' },
    { name: 'Termination', checks: 'How either party can end the work, and what you are paid when it ends.' },
    { name: 'Non-compete', checks: 'Limits on future work: what they cover, where, and for how long.' },
    { name: 'Confidentiality', checks: 'What you must keep private, for how long, and the usual exceptions.' },
    { name: 'Jurisdiction', checks: 'Which law governs the contract and where disputes are heard.' },
  ];

  constructor() {
    const destroyRef = inject(DestroyRef);
    afterNextRender(() => {
      this.anchorEl.set(this.marker().nativeElement);
      const measure = () => this.measureLeader();
      measure();
      const observer = new ResizeObserver(measure);
      observer.observe(this.plate().nativeElement);
      destroyRef.onDestroy(() => observer.disconnect());
    });
  }

  private measureLeader(): void {
    const plate = this.plate().nativeElement.getBoundingClientRect();
    const from = this.marker().nativeElement.getBoundingClientRect();
    const to = this.callout().nativeElement.getBoundingClientRect();
    const text = this.marker().nativeElement.closest('blockquote')!.getBoundingClientRect();
    const x1 = from.left + from.width / 2 - plate.left;
    const y1 = from.top + from.height / 2 - plate.top;
    const y2 = to.top - plate.top;
    let d: string;
    if (from.bottom >= text.bottom - 4) {
      // The flag ends the clause: drop straight down, then a 45-degree jog onto the callout.
      const x2 = Math.max(to.left - plate.left + 28, 0);
      const jog = Math.min(Math.abs(x2 - x1), Math.max(y2 - y1 - 18, 0));
      d = `M ${x1} ${y1} V ${y2 - jog} L ${x1 + Math.sign(x2 - x1) * jog} ${y2}`;
    } else {
      // Text continues below the flag: run along the line gap to the margin, down the margin,
      // then onto the callout's corner, so the leader never crosses a word.
      const margin = text.right - plate.left + (plate.right - text.right) / 2;
      const corner = to.right - plate.left - 18;
      d = `M ${x1} ${y1} H ${margin} V ${y2 - (margin - corner)} L ${corner} ${y2}`;
    }
    this.leader.set({ d, length: pathLength(d) });
  }
}

/** Length of an M/H/V/L polyline, for the stroke-dash draw-in. */
function pathLength(d: string): number {
  const tokens = d.trim().split(/\s+/);
  let x = 0, y = 0, length = 0;
  for (let i = 0; i < tokens.length;) {
    const op = tokens[i++];
    let nx = x, ny = y;
    if (op === 'M' || op === 'L') { nx = +tokens[i++]; ny = +tokens[i++]; }
    else if (op === 'H') { nx = +tokens[i++]; }
    else if (op === 'V') { ny = +tokens[i++]; }
    if (op !== 'M') length += Math.hypot(nx - x, ny - y);
    x = nx; y = ny;
  }
  return Math.max(length, 1);
}
