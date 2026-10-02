import { describe, expect, it } from 'vitest';
import { MAX_UPLOAD_BYTES, formatBytes, validatePdf } from './contracts.service';

const file = (name: string, size: number, type = 'application/pdf') =>
  ({ name, size, type }) as File;

describe('validatePdf', () => {
  it('accepts a PDF within the limit', () => {
    expect(validatePdf(file('nda.pdf', 2048))).toBeNull();
    expect(validatePdf(file('scan.PDF', 2048, ''))).toBeNull();
  });

  it('rejects other file types', () => {
    expect(validatePdf(file('contract.docx', 2048, 'application/msword'))).toBe('Only PDF files are supported.');
  });

  it('rejects files over 10 MB and empty files', () => {
    expect(validatePdf(file('big.pdf', MAX_UPLOAD_BYTES + 1))).toBe('Files must be 10 MB or smaller.');
    expect(validatePdf(file('empty.pdf', 0))).toBe('This file is empty.');
  });
});

describe('formatBytes', () => {
  it('formats sizes for the register', () => {
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(2048)).toBe('2 KB');
    expect(formatBytes(1_887_437)).toBe('1.8 MB');
  });
});
