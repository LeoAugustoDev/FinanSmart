import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Transaction,
  TransactionFilters,
  TransactionPage,
  TransactionRequest,
} from './transaction.model';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/transactions`;

  list(filters: TransactionFilters): Observable<TransactionPage> {
    let params = new HttpParams().set('page', filters.page).set('size', filters.size);
    for (const key of ['from', 'to', 'accountId', 'categoryId'] as const) {
      if (filters[key]) {
        params = params.set(key, filters[key]);
      }
    }
    return this.http.get<TransactionPage>(this.baseUrl, { params });
  }

  get(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.baseUrl}/${id}`);
  }

  create(request: TransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(this.baseUrl, request);
  }

  update(id: string, request: TransactionRequest): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
