import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { MonthlyReport } from './report.model';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);

  monthly(month: string): Observable<MonthlyReport> {
    return this.http.get<MonthlyReport>(`${environment.apiUrl}/reports/monthly`, {
      params: { month },
    });
  }
}
