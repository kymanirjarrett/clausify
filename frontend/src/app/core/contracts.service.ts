import { HttpClient, HttpEvent, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AnalysisResult, Comparison, ContractDetail, ContractSummary, Page } from './api.models';

export const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

@Injectable({ providedIn: 'root' })
export class ContractsService {
  private readonly http = inject(HttpClient);

  /** Emits progress events, then the response (202 with the new contract). */
  upload(file: File): Observable<HttpEvent<ContractSummary>> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<ContractSummary>('/api/contracts', form, { reportProgress: true, observe: 'events' });
  }

  list(page: number, size: number): Observable<Page<ContractSummary>> {
    return this.http.get<Page<ContractSummary>>('/api/contracts', { params: { page, size } });
  }

  get(id: number): Observable<ContractDetail> {
    return this.http.get<ContractDetail>(`/api/contracts/${id}`);
  }

  /** FR-7: queues a FAILED contract for analysis again (202). 409 if it did not fail; 429 shares the upload budget. */
  retry(id: number): Observable<ContractSummary> {
    return this.http.post<ContractSummary>(`/api/contracts/${id}/retry`, null);
  }

  /** FR-11: the report PDF with its response headers, so the caller can read the file name. 409 until ANALYZED. */
  report(id: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`/api/contracts/${id}/report`, { responseType: 'blob', observe: 'response' });
  }

  /** FR-10: both must be analyzed (409 otherwise). Makes no AI call. */
  compare(id: number, otherId: number): Observable<Comparison> {
    return this.http.post<Comparison>(`/api/contracts/${id}/compare/${otherId}`, null);
  }

  /** 409 while the contract is not ANALYZED yet; call it once the status says ANALYZED. */
  analysis(id: number): Observable<AnalysisResult> {
    return this.http.get<AnalysisResult>(`/api/contracts/${id}/analysis`);
  }
}

/** Checked before uploading, so the user is told immediately; the API enforces the same rules. */
export function validatePdf(file: File): string | null {
  const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
  if (!isPdf) {
    return 'Only PDF files are supported.';
  }
  if (file.size > MAX_UPLOAD_BYTES) {
    return 'Files must be 10 MB or smaller.';
  }
  if (file.size === 0) {
    return 'This file is empty.';
  }
  return null;
}

/** The name from a Content-Disposition header, or a fallback when it is missing or unreadable. */
export function downloadName(disposition: string | null, fallback: string): string {
  const match = disposition?.match(/filename="?([^";]+)"?/i);
  return match ? match[1] : fallback;
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
