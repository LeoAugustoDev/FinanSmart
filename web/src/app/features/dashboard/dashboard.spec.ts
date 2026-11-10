import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { environment } from '../../../environments/environment';
import { LeakService } from '../leaks/leak.service';
import { Dashboard } from './dashboard';
import { DashboardData } from './dashboard.model';
import { DashboardService } from './dashboard.service';

describe('Dashboard', () => {
  const service = { get: vi.fn() };
  const leakService = { detect: vi.fn() };

  const data: DashboardData = {
    month: '2026-11',
    totalBalance: -150.5,
    income: 1000,
    expense: 1150.5,
    result: -150.5,
    accounts: [
      {
        id: 'a',
        name: 'Nubank',
        type: 'CHECKING',
        initialBalance: 0,
        currentBalance: -150.5,
        currency: 'BRL',
        archived: false,
        createdAt: '',
        updatedAt: '',
      },
    ],
    recentTransactions: [
      {
        id: 't',
        accountId: 'a',
        accountName: 'Nubank',
        categoryId: 'c',
        categoryName: 'Lazer',
        type: 'EXPENSE',
        amount: 50,
        occurredOn: '2026-11-09',
        description: 'Cinema',
        createdAt: '',
        updatedAt: '',
      },
    ],
  };

  const empty: DashboardData = {
    ...data,
    totalBalance: 0,
    income: 0,
    expense: 0,
    result: 0,
    accounts: [],
    recentTransactions: [],
  };

  beforeEach(() => {
    vi.resetAllMocks();
    leakService.detect.mockReturnValue(of({ total: 0 }));
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: DashboardService, useValue: service },
        { provide: LeakService, useValue: leakService },
      ],
    });
  });

  async function render(me: 'ok' | 'error' = 'ok'): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(Dashboard);
    TestBed.tick();
    const call = TestBed.inject(HttpTestingController).expectOne(`${environment.apiUrl}/me`);
    if (me === 'ok') {
      call.flush({ id: '1', username: 'maria', name: 'Maria Silva', email: 'maria@example.com' });
    } else {
      call.flush(null, { status: 500, statusText: 'Erro' });
    }
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('pede os dados do mês atual', async () => {
    service.get.mockReturnValue(of(data));

    await render();

    expect(service.get).toHaveBeenCalledWith(expect.stringMatching(/^\d{4}-\d{2}$/));
  });

  it('mostra saudação, resumo, contas e últimos lançamentos', async () => {
    service.get.mockReturnValue(of(data));

    const element = await render();

    expect(element.querySelector('h2')?.textContent).toContain('Maria Silva');
    expect(element.querySelector('#totalBalance')?.textContent).toContain('150.50');
    expect(element.querySelector('#income')?.textContent).toContain('1,000.00');
    expect(element.querySelector('#accounts tbody')?.textContent).toContain('Nubank');
    expect(element.querySelector('#recent tbody')?.textContent).toContain('Cinema');
  });

  it('destaca saldo negativo', async () => {
    service.get.mockReturnValue(of(data));

    expect((await render()).querySelector('#totalBalance')?.classList).toContain('amount-expense');
  });

  it('orienta o usuário que ainda não tem dados', async () => {
    service.get.mockReturnValue(of(empty));

    const element = await render();

    expect(element.textContent).toContain('Nenhuma conta ainda');
    expect(element.textContent).toContain('Nenhum lançamento ainda');
    expect(element.querySelector('#accounts')).toBeNull();
  });

  it('mostra quanto está vazando no mês, com link para os detalhes', async () => {
    service.get.mockReturnValue(of(data));
    leakService.detect.mockReturnValue(of({ total: 282.4 }));

    const card = (await render()).querySelector('#leaking');

    expect(leakService.detect).toHaveBeenCalledWith(expect.stringMatching(/^\d{4}-\d{2}$/));
    expect(card?.textContent).toContain('282.40 vazando este mês');
    expect(card?.getAttribute('href')).toBe('/vazamentos');
  });

  it('esconde o card de vazamentos quando não há nada vazando', async () => {
    service.get.mockReturnValue(of(data));

    expect((await render()).querySelector('#leaking')).toBeNull();
  });

  it('mostra o painel mesmo se os vazamentos não carregarem', async () => {
    service.get.mockReturnValue(of(data));
    leakService.detect.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    const element = await render();

    expect(element.querySelector('#leaking')).toBeNull();
    expect(element.querySelector('#accounts tbody')?.textContent).toContain('Nubank');
  });

  it('oferece nova tentativa quando o painel não carrega', async () => {
    service.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    const element = await render();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Não foi possível carregar');
    expect(element.textContent).toContain('Tentar de novo');
  });

  it('mostra o painel mesmo se o nome do usuário não carregar', async () => {
    service.get.mockReturnValue(of(data));

    const element = await render('error');

    expect(element.querySelector('h2')?.textContent).toContain('Painel');
    expect(element.querySelector('#accounts tbody')?.textContent).toContain('Nubank');
  });
});
