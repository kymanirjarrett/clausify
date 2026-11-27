import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AccountDetails, AccountStats } from './api.models';

/** Same rule as the API's UpdateAccountRequest: empty, or 7 to 15 digits with optional + ( ) . - and spaces. */
export const PHONE_PATTERN = /^$|^\+?[0-9 ().-]{7,15}$/;

export interface AccountChanges {
  name?: string;
  phoneNumber?: string;
}

/**
 * Only the fields that changed, trimmed, so PATCH leaves the rest alone. An emptied phone is sent as "" to clear it;
 * a blank name is never sent (the form blocks it).
 */
export function accountChanges(saved: AccountDetails, name: string, phone: string): AccountChanges {
  const changes: AccountChanges = {};
  const trimmedName = name.trim();
  const trimmedPhone = phone.trim();
  if (trimmedName && trimmedName !== saved.name) changes.name = trimmedName;
  if (trimmedPhone !== (saved.phoneNumber ?? '')) changes.phoneNumber = trimmedPhone;
  return changes;
}

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);

  get(): Observable<AccountDetails> {
    return this.http.get<AccountDetails>('/api/users/me');
  }

  update(changes: AccountChanges): Observable<AccountDetails> {
    return this.http.patch<AccountDetails>('/api/users/me', changes);
  }

  stats(): Observable<AccountStats> {
    return this.http.get<AccountStats>('/api/users/me/stats');
  }
}
