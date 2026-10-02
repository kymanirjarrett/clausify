import { describe, expect, it } from 'vitest';
import { tokenExpiry } from './auth.service';

const encode = (payload: object) =>
  `header.${btoa(JSON.stringify(payload)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')}.signature`;

describe('tokenExpiry', () => {
  it('reads the exp claim in milliseconds', () => {
    expect(tokenExpiry(encode({ sub: '7', exp: 1_790_000_000 }))).toBe(1_790_000_000_000);
  });

  it('returns null for malformed tokens or a missing exp', () => {
    expect(tokenExpiry('not-a-jwt')).toBeNull();
    expect(tokenExpiry(encode({ sub: '7' }))).toBeNull();
  });
});
