import { describe, expect, it } from 'vitest';
import { categoryChangeLabel, fold, scoreChangeLabel, worsened } from './compare-view';

describe('scoreChangeLabel', () => {
  it('says which way risk moved and by how much', () => {
    expect(scoreChangeLabel(-24)).toBe('Risk down 24 points');
    expect(scoreChangeLabel(10)).toBe('Risk up 10 points');
    expect(scoreChangeLabel(1)).toBe('Risk up 1 point');
    expect(scoreChangeLabel(0)).toBe('Risk unchanged');
  });
});

describe('categoryChangeLabel and worsened', () => {
  it('describes new, resolved, and moved categories', () => {
    expect(categoryChangeLabel(null, 55)).toBe('New risk · 55');
    expect(categoryChangeLabel(85, null)).toBe('Resolved · was 85');
    expect(categoryChangeLabel(65, 20)).toBe('65 → 20');
  });

  it('flags only changes that add risk', () => {
    expect(worsened(null, 55)).toBe(true);
    expect(worsened(20, 65)).toBe(true);
    expect(worsened(85, null)).toBe(false);
    expect(worsened(65, 20)).toBe(false);
  });
});

describe('fold', () => {
  it('leaves short text alone', () => {
    expect(fold('Governing law. This Agreement is governed by Ohio law.')).toBeNull();
  });

  it('keeps the first and last words of a long stretch and counts the rest', () => {
    const text = Array.from({ length: 100 }, (_, i) => `w${i}`).join(' ');
    expect(fold(text, 3)).toEqual({ head: 'w0 w1 w2', tail: 'w97 w98 w99', hiddenWords: 94 });
  });
});
