import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
  });

  it('mostra o usuário retornado pela API', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    TestBed.tick();

    TestBed.inject(HttpTestingController)
      .expectOne(`${environment.apiUrl}/me`)
      .flush({ id: '1', username: 'maria', name: 'Maria Silva', email: 'maria@example.com' });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('h2')?.textContent).toContain(
      'Maria Silva',
    );
  });

  it('oferece nova tentativa quando a API falha', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    TestBed.tick();

    TestBed.inject(HttpTestingController)
      .expectOne(`${environment.apiUrl}/me`)
      .flush(null, { status: 500, statusText: 'Erro' });
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).querySelector('[role=alert]')).toBeTruthy();
  });
});
