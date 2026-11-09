import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { LeakService } from './leak.service';

describe('LeakService', () => {
  it('busca os vazamentos do mês', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const backend = TestBed.inject(HttpTestingController);

    TestBed.inject(LeakService).detect('2026-11').subscribe();

    const call = backend.expectOne(`${environment.apiUrl}/leaks?month=2026-11`);
    expect(call.request.method).toBe('GET');
    call.flush({});
    backend.verify();
  });
});
