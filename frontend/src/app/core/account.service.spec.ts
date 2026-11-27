import { describe, expect, it } from 'vitest';
import { PHONE_PATTERN, accountChanges } from './account.service';

const saved = { id: 1, name: 'Maria Lopez', email: 'maria@example.com', phoneNumber: '5135550142' };

describe('accountChanges', () => {
  it('sends nothing when nothing changed, ignoring surrounding spaces', () => {
    expect(accountChanges(saved, ' Maria Lopez ', '5135550142 ')).toEqual({});
  });

  it('sends only the fields that changed, trimmed', () => {
    expect(accountChanges(saved, '  Maria L. Lopez ', '5135550142')).toEqual({ name: 'Maria L. Lopez' });
    expect(accountChanges(saved, 'Maria Lopez', '+1 513-555-0199')).toEqual({ phoneNumber: '+1 513-555-0199' });
  });

  it('sends an empty phone to clear it, but never a blank name', () => {
    expect(accountChanges(saved, '   ', '')).toEqual({ phoneNumber: '' });
  });

  it('treats a missing saved phone as empty', () => {
    expect(accountChanges({ ...saved, phoneNumber: null }, 'Maria Lopez', '')).toEqual({});
  });
});

describe('PHONE_PATTERN', () => {
  it('matches what the API accepts', () => {
    for (const ok of ['', '5135550142', '+1 513-555-0142', '(513) 555.0142']) expect(PHONE_PATTERN.test(ok)).toBe(true);
    for (const bad of ['123456', 'call me', '+1 513 555 0142 99', '513-555-0142 x9']) expect(PHONE_PATTERN.test(bad)).toBe(false);
  });
});
