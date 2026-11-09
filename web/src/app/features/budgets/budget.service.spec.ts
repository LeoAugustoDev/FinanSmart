import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { BudgetService } from './budget.service';

describe('BudgetService', () => {
  const baseUrl = `${environment.apiUrl}/budgets`;

  let service: BudgetService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BudgetService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('lista os orçamentos do mês', () => {
    service.list('2026-11').subscribe();

    const call = backend.expectOne(`${baseUrl}?month=2026-11`);
    expect(call.request.method).toBe('GET');
    call.flush([]);
  });

  it('busca um orçamento', () => {
    service.get('1').subscribe();

    expect(backend.expectOne(`${baseUrl}/1`).request.method).toBe('GET');
  });

  it('cria um orçamento', () => {
    const request = { categoryId: 'c', month: '2026-11', limit: 100 };

    service.create(request).subscribe();

    const call = backend.expectOne(baseUrl);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
  });

  it('altera o limite', () => {
    service.update('1', { limit: 200 }).subscribe();

    const call = backend.expectOne(`${baseUrl}/1`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual({ limit: 200 });
  });

  it('exclui um orçamento', () => {
    service.delete('1').subscribe();

    expect(backend.expectOne(`${baseUrl}/1`).request.method).toBe('DELETE');
  });
});
