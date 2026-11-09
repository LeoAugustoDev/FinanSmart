import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { CategoryService } from './category.service';

describe('CategoryService', () => {
  const baseUrl = `${environment.apiUrl}/categories`;

  let service: CategoryService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CategoryService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('lista todas as categorias', () => {
    service.list().subscribe();

    const call = backend.expectOne(baseUrl);
    expect(call.request.method).toBe('GET');
    call.flush([]);
  });

  it('filtra por tipo', () => {
    service.list('INCOME').subscribe();

    backend.expectOne(`${baseUrl}?type=INCOME`).flush([]);
  });

  it('busca uma categoria', () => {
    service.get('1').subscribe();

    expect(backend.expectOne(`${baseUrl}/1`).request.method).toBe('GET');
  });

  it('cria uma categoria', () => {
    service.create({ name: 'Pets', type: 'EXPENSE' }).subscribe();

    const call = backend.expectOne(baseUrl);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual({ name: 'Pets', type: 'EXPENSE' });
  });

  it('renomeia uma categoria', () => {
    service.update('1', { name: 'Animais' }).subscribe();

    const call = backend.expectOne(`${baseUrl}/1`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual({ name: 'Animais' });
  });

  it('exclui uma categoria', () => {
    service.delete('1').subscribe();

    expect(backend.expectOne(`${baseUrl}/1`).request.method).toBe('DELETE');
  });
});
