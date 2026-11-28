/** "Risk down 24 points", "Risk up 10 points", or "Risk unchanged", from revised minus original. */
export function scoreChangeLabel(change: number): string {
  if (change === 0) return 'Risk unchanged';
  const points = Math.abs(change) === 1 ? 'point' : 'points';
  return `Risk ${change < 0 ? 'down' : 'up'} ${Math.abs(change)} ${points}`;
}

/** One category's highest risky score before and after; null means no risky finding in that version. */
export function categoryChangeLabel(before: number | null, after: number | null): string {
  if (before === null) return `New risk · ${after}`;
  if (after === null) return `Resolved · was ${before}`;
  return `${before} → ${after}`;
}

/** True when the revised version is riskier in this category, so the UI can mark it out of tolerance. */
export function worsened(before: number | null, after: number | null): boolean {
  return (after ?? 0) > (before ?? 0);
}

export interface Folded {
  head: string;
  tail: string;
  hiddenWords: number;
}

/**
 * Long unchanged stretches are folded to their first and last few words, so the page shows the changes, not the
 * whole contract. Returns null when the text is short enough to show in full.
 */
export function fold(text: string, keep = 12): Folded | null {
  const words = text.split(' ');
  if (words.length <= keep * 2 + 5) return null;
  return {
    head: words.slice(0, keep).join(' '),
    tail: words.slice(-keep).join(' '),
    hiddenWords: words.length - keep * 2,
  };
}
