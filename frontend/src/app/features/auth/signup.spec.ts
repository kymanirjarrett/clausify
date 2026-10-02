import { describe, expect, it } from 'vitest';
import { PASSWORD_PATTERN } from './signup';

describe('PASSWORD_PATTERN (mirrors the API rule)', () => {
  it.each(['Str0ng!Passw0rd', 'abcdefg1', 'pässwört9'])('accepts %s', (password) => {
    expect(PASSWORD_PATTERN.test(password)).toBe(true);
  });

  it.each(['abc123', 'OnlyLettersHere', '12345678', 'a1'.repeat(37)])('rejects %s', (password) => {
    expect(PASSWORD_PATTERN.test(password)).toBe(false);
  });
});
