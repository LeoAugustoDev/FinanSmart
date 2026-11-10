import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Leaks } from './leak.model';

@Injectable({ providedIn: 'root' })
export class LeakService {
  private readonly http = inject(HttpClient);

  detect(month: string): Observable<Leaks> {
    return this.http.get<Leaks>(`${environment.apiUrl}/leaks`, { params: { month } });
  }
}
