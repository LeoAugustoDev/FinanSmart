import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { environment } from '../../../environments/environment';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  const accessToken = vi.fn<() => Promise<string | undefined>>();
  let http: HttpClient;
  let backend: HttpTestingController;

  const flushPromises = () => new Promise((resolve) => setTimeout(resolve));

  beforeEach(() => {
    accessToken.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { accessToken } },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('anexa o token nas chamadas da API', async () => {
    accessToken.mockResolvedValue('token-abc');

    http.get(`${environment.apiUrl}/me`).subscribe();
    await flushPromises();

    const call = backend.expectOne(`${environment.apiUrl}/me`);
    expect(call.request.headers.get('Authorization')).toBe('Bearer token-abc');
    call.flush({});
  });

  it('segue sem cabeçalho quando não há sessão', async () => {
    accessToken.mockResolvedValue(undefined);

    http.get(`${environment.apiUrl}/me`).subscribe();
    await flushPromises();

    const call = backend.expectOne(`${environment.apiUrl}/me`);
    expect(call.request.headers.has('Authorization')).toBe(false);
    call.flush({});
  });

  it.each(['https://outro-site.example/api', `${environment.apiUrl}-falso/me`])(
    'não envia o token para %s',
    (url) => {
      http.get(url).subscribe();

      const call = backend.expectOne(url);
      expect(call.request.headers.has('Authorization')).toBe(false);
      expect(accessToken).not.toHaveBeenCalled();
      call.flush({});
    },
  );
});
