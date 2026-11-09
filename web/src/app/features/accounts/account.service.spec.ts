import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AccountRequest } from './account.model';
import { AccountService } from './account.service';

describe('AccountService', () => {
  const baseUrl = `${environment.apiUrl}/accounts`;
  const request: AccountRequest = { name: 'Nubank', type: 'CHECKING', initialBalance: 1500 };

  let service: AccountService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AccountService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('lista apenas as ativas por padrão', () => {
    service.list().subscribe();

    const call = backend.expectOne(`${baseUrl}?includeArchived=false`);
    expect(call.request.method).toBe('GET');
    call.flush([]);
  });

  it('lista incluindo as arquivadas', () => {
    service.list(true).subscribe();

    backend.expectOne(`${baseUrl}?includeArchived=true`).flush([]);
  });

  it('busca uma conta', () => {
    service.get('1').subscribe();

    expect(backend.expectOne(`${baseUrl}/1`).request.method).toBe('GET');
  });

  it('cria uma conta', () => {
    service.create(request).subscribe();

    const call = backend.expectOne(baseUrl);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
  });

  it('atualiza uma conta', () => {
    service.update('1', request).subscribe();

    const call = backend.expectOne(`${baseUrl}/1`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual(request);
  });

  it('arquiva uma conta', () => {
    service.archive('1').subscribe();

    expect(backend.expectOne(`${baseUrl}/1/archive`).request.method).toBe('PATCH');
  });

  it('desarquiva uma conta', () => {
    service.unarchive('1').subscribe();

    expect(backend.expectOne(`${baseUrl}/1/unarchive`).request.method).toBe('PATCH');
  });

  it('exclui uma conta', () => {
    service.delete('1').subscribe();

    expect(backend.expectOne(`${baseUrl}/1`).request.method).toBe('DELETE');
  });
});
