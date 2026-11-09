import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ReportService } from './report.service';

describe('ReportService', () => {
  it('busca o relatório do mês', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const backend = TestBed.inject(HttpTestingController);

    TestBed.inject(ReportService).monthly('2026-11').subscribe();

    const call = backend.expectOne(`${environment.apiUrl}/reports/monthly?month=2026-11`);
    expect(call.request.method).toBe('GET');
    call.flush({});
    backend.verify();
  });
});
