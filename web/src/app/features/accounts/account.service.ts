import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Account, AccountRequest } from './account.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/accounts`;

  list(includeArchived = false): Observable<Account[]> {
    return this.http.get<Account[]>(this.baseUrl, { params: { includeArchived } });
  }

  get(id: string): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/${id}`);
  }

  create(request: AccountRequest): Observable<Account> {
    return this.http.post<Account>(this.baseUrl, request);
  }

  update(id: string, request: AccountRequest): Observable<Account> {
    return this.http.put<Account>(`${this.baseUrl}/${id}`, request);
  }

  archive(id: string): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/archive`, null);
  }

  unarchive(id: string): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/unarchive`, null);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
