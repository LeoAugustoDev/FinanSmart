import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Category,
  CategoryType,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from './category.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/categories`;

  list(type?: CategoryType): Observable<Category[]> {
    return this.http.get<Category[]>(this.baseUrl, { params: type ? { type } : {} });
  }

  get(id: string): Observable<Category> {
    return this.http.get<Category>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateCategoryRequest): Observable<Category> {
    return this.http.post<Category>(this.baseUrl, request);
  }

  update(id: string, request: UpdateCategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
