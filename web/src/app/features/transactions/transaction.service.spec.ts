import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { TransactionFilters, TransactionRequest } from './transaction.model';
import { TransactionService } from './transaction.service';

describe('TransactionService', () => {
  const baseUrl = `${environment.apiUrl}/transactions`;
  const noFilters: TransactionFilters = {
    from: '',
    to: '',
    accountId: '',
    categoryId: '',
    page: 0,
    size: 20,
  };
  const request: TransactionRequest = {
    accountId: 'a',
    categoryId: 'c',
    amount: 10,
    occurredOn: '2026-11-09',
    description: null,
  };

  let service: TransactionService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TransactionService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('lista sem enviar filtros vazios', () => {
    service.list(noFilters).subscribe();

    const call = backend.expectOne(`${baseUrl}?page=0&size=20`);
    expect(call.request.method).toBe('GET');
    call.flush({});
  });

  it('envia os filtros preenchidos', () => {
    service
      .list({ ...noFilters, from: '2026-01-01', to: '2026-01-31', accountId: 'a', categoryId: 'c', page: 2 })
      .subscribe();

    const call = backend.expectOne((req) => req.url === baseUrl);
    expect(call.request.params.get('from')).toBe('2026-01-01');
    expect(call.request.params.get('to')).toBe('2026-01-31');
    expect(call.request.params.get('accountId')).toBe('a');
    expect(call.request.params.get('categoryId')).toBe('c');
    expect(call.request.params.get('page')).toBe('2');
    call.flush({});
  });

  it('busca um lançamento', () => {
    service.get('1').subscribe();

    expect(backend.expectOne(`${baseUrl}/1`).request.method).toBe('GET');
  });

  it('cria um lançamento', () => {
    service.create(request).subscribe();

    const call = backend.expectOne(baseUrl);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
  });

  it('atualiza um lançamento', () => {
    service.update('1', request).subscribe();

    expect(backend.expectOne(`${baseUrl}/1`).request.method).toBe('PUT');
  });

  it('exclui um lançamento', () => {
    service.delete('1').subscribe();

    expect(backend.expectOne(`${baseUrl}/1`).request.method).toBe('DELETE');
  });
});
