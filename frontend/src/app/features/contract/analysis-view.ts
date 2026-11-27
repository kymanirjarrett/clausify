import { Finding, RiskCategory, RiskLevel } from '../../core/api.models';

/** Filter chips on the results screen (FR-6 example 2). */
export type LevelFilter = 'ALL' | RiskLevel;

export const LEVEL_FILTERS: readonly LevelFilter[] = ['ALL', 'HIGH', 'MEDIUM', 'LOW'];

const CATEGORY_LABELS: Record<RiskCategory, string> = {
  PAYMENT_TERMS: 'Payment terms',
  LIABILITY: 'Liability',
  INDEMNIFICATION: 'Indemnification',
  IP_RIGHTS: 'IP rights',
  TERMINATION: 'Termination',
  NON_COMPETE: 'Non-compete',
  CONFIDENTIALITY: 'Confidentiality',
  JURISDICTION: 'Jurisdiction',
};

const LEVEL_LABELS: Record<LevelFilter, string> = { ALL: 'All', HIGH: 'High', MEDIUM: 'Medium', LOW: 'Low' };

export function categoryLabel(category: RiskCategory): string {
  return CATEGORY_LABELS[category];
}

export function levelLabel(level: LevelFilter): string {
  return LEVEL_LABELS[level];
}

/** Findings at the chosen level, keeping the API's riskiest-first order. */
export function filterFindings(findings: readonly Finding[], filter: LevelFilter): Finding[] {
  return filter === 'ALL' ? [...findings] : findings.filter((f) => f.riskLevel === filter);
}

/** How many findings each chip would show, e.g. "High · 2". */
export function levelCounts(findings: readonly Finding[]): Record<LevelFilter, number> {
  const counts: Record<LevelFilter, number> = { ALL: findings.length, HIGH: 0, MEDIUM: 0, LOW: 0 };
  for (const f of findings) counts[f.riskLevel]++;
  return counts;
}

/** Position on the 0 to 100 gauge as a CSS percentage, clamped so a bad value cannot draw off the scale. */
export function gaugePercent(score: number): string {
  return `${Math.min(100, Math.max(0, score))}%`;
}

/** "19.3 s" for the document panel; the API reports milliseconds. */
export function formatDuration(ms: number): string {
  return ms < 1000 ? `${ms} ms` : `${(ms / 1000).toFixed(1)} s`;
}
