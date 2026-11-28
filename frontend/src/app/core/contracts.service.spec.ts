import { describe, expect, it } from 'vitest';
import { MAX_UPLOAD_BYTES, downloadName, formatBytes, validatePdf } from './contracts.service';

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

describe('downloadName', () => {
  it('reads the file name from Content-Disposition', () => {
    expect(downloadName('attachment; filename="Clausify_Report_NDA_v1.pdf"', 'x.pdf')).toBe('Clausify_Report_NDA_v1.pdf');
    expect(downloadName('attachment; filename=Clausify_Report_NDA.pdf', 'x.pdf')).toBe('Clausify_Report_NDA.pdf');
  });

  it('falls back when the header is missing or has no name', () => {
    expect(downloadName(null, 'Clausify_Report.pdf')).toBe('Clausify_Report.pdf');
    expect(downloadName('attachment', 'Clausify_Report.pdf')).toBe('Clausify_Report.pdf');
  });
});
