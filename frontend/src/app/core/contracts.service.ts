import { HttpClient, HttpEvent } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ContractDetail, ContractSummary, Page } from './api.models';

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

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
