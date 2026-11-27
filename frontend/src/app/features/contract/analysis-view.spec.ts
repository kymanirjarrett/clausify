import { describe, expect, it } from 'vitest';
import { Finding, RiskLevel } from '../../core/api.models';
import { categoryLabel, filterFindings, formatDuration, gaugePercent, levelCounts, levelLabel } from './analysis-view';

let nextId = 1;
const finding = (riskLevel: RiskLevel, riskScore: number): Finding => ({
  id: nextId++,
  category: 'LIABILITY',
  riskScore,
  riskLevel,
  title: 'Clause',
  sectionRef: '7.2',
  originalText: 'Original.',
  explanation: 'Why.',
  suggestedRevision: 'Fairer.',
});

// FR-6 example 2: 2 High, 2 Medium, 3 Low, already riskiest first as the API returns them.
const findings = [
  finding('HIGH', 90),
  finding('HIGH', 75),
  finding('MEDIUM', 60),
  finding('MEDIUM', 45),
  finding('LOW', 35),
  finding('LOW', 20),
  finding('LOW', 10),
];

describe('filterFindings', () => {
  it('shows every finding for All, in the API order', () => {
    expect(filterFindings(findings, 'ALL')).toEqual(findings);
  });

  it('shows only the chosen level and keeps riskiest first', () => {
    expect(filterFindings(findings, 'HIGH').map((f) => f.riskScore)).toEqual([90, 75]);
    expect(filterFindings(findings, 'LOW').map((f) => f.riskScore)).toEqual([35, 20, 10]);
  });

  it('returns a new array, so the input is never changed', () => {
    expect(filterFindings(findings, 'ALL')).not.toBe(findings);
  });
});

describe('levelCounts', () => {
  it('counts each level for the chips ("High · 2")', () => {
    expect(levelCounts(findings)).toEqual({ ALL: 7, HIGH: 2, MEDIUM: 2, LOW: 3 });
  });

  it('is all zeros with no findings', () => {
    expect(levelCounts([])).toEqual({ ALL: 0, HIGH: 0, MEDIUM: 0, LOW: 0 });
  });
});

describe('labels', () => {
  it('names every category in plain words', () => {
    expect(categoryLabel('IP_RIGHTS')).toBe('IP rights');
    expect(categoryLabel('NON_COMPETE')).toBe('Non-compete');
    expect(categoryLabel('PAYMENT_TERMS')).toBe('Payment terms');
  });

  it('names the levels and the All filter', () => {
    expect([levelLabel('ALL'), levelLabel('HIGH'), levelLabel('MEDIUM'), levelLabel('LOW')]).toEqual([
      'All',
      'High',
      'Medium',
      'Low',
    ]);
  });
});

describe('gaugePercent', () => {
  it('places the score on the 0 to 100 line and clamps bad values', () => {
    expect(gaugePercent(89)).toBe('89%');
    expect(gaugePercent(-5)).toBe('0%');
    expect(gaugePercent(140)).toBe('100%');
  });
});

describe('formatDuration', () => {
  it('reads milliseconds as seconds for the document panel', () => {
    expect(formatDuration(19304)).toBe('19.3 s');
    expect(formatDuration(850)).toBe('850 ms');
  });
});
