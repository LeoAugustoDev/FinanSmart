import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Leaks } from './leak.model';
import { LeakService } from './leak.service';
import { LeaksPage } from './leaks';

describe('LeaksPage', () => {
  const service = { detect: vi.fn() };

  const leaks: Leaks = {
    month: '2026-01',
    total: 282.4,
    subscriptions: {
      total: 89.9,
      items: [
        { description: 'Clube do vinho', categoryName: 'Lazer', amount: 50, recurring: true },
        { description: null, categoryName: 'Assinaturas', amount: 39.9, recurring: false },
      ],
    },
    fees: {
      total: 42.5,
      items: [
        { description: 'Juros do cartão', categoryName: 'Tarifas e juros', amount: 42.5, recurring: false },
      ],
    },
    aboveAverage: {
      total: 150,
      items: [{ categoryId: 'food', categoryName: 'Alimentação', spent: 350, average: 200, excess: 150 }],
    },
  };

  const none = { total: 0, items: [] };
  const clear: Leaks = { ...leaks, total: 0, subscriptions: none, fees: none, aboveAverage: none };

  function configure(month: string | null = '2026-01'): void {
    TestBed.configureTestingModule({
      imports: [LeaksPage],
      providers: [
        provideRouter([]),
        { provide: LeakService, useValue: service },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap(month ? { month } : {}) },
          },
        },
      ],
    });
  }

  async function render() {
    const fixture = TestBed.createComponent(LeaksPage);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  beforeEach(() => {
    vi.resetAllMocks();
    service.detect.mockReturnValue(of(leaks));
  });

  it('usa o mês recebido na rota', async () => {
    configure();

    await render();

    expect(service.detect).toHaveBeenCalledWith('2026-01');
  });

  it('mostra quanto está vazando neste mês quando a rota não informa o mês', async () => {
    configure(null);

    const { element } = await render();

    expect(service.detect).toHaveBeenCalledWith(expect.stringMatching(/^\d{4}-\d{2}$/));
    expect(element.querySelector('#leakTotal')?.textContent).toContain('282.40 vazando este mês');
  });

  it('não chama de "este mês" um mês que não é o atual', async () => {
    configure();

    const { element } = await render();

    expect(element.querySelector('#leakTotal')?.textContent).toContain('282.40 vazando no mês');
  });

  it('mostra o total de cada detector', async () => {
    configure();

    const { element } = await render();

    expect(element.querySelector('#subscriptionsTotal')?.textContent).toContain('89.90');
    expect(element.querySelector('#feesTotal')?.textContent).toContain('42.50');
    expect(element.querySelector('#aboveAverageTotal')?.textContent).toContain('150.00');
  });

  it('lista as assinaturas e marca as que se repetem todo mês', async () => {
    configure();

    const { element } = await render();

    const rows = element.querySelectorAll('#subscriptions tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Clube do vinho');
    expect(rows[0].textContent).toContain('Todo mês');
    expect(rows[1].textContent).toContain('Sem descrição');
    expect(rows[1].textContent).not.toContain('Todo mês');
  });

  it('lista tarifas e categorias acima da média', async () => {
    configure();

    const { element } = await render();

    expect(element.querySelector('#fees tbody')?.textContent).toContain('Juros do cartão');
    const overspend = element.querySelector('#aboveAverage tbody tr')!;
    expect(overspend.textContent).toContain('Alimentação');
    expect(overspend.textContent).toContain('350.00');
    expect(overspend.textContent).toContain('200.00');
    expect(overspend.textContent).toContain('150.00');
  });

  it('explica cada detector que não encontrou nada', async () => {
    service.detect.mockReturnValue(of({ ...leaks, subscriptions: none, aboveAverage: none }));
    configure();

    const { element } = await render();

    expect(element.textContent).toContain('Nenhuma assinatura neste mês');
    expect(element.textContent).toContain('Nenhuma categoria passou');
    expect(element.querySelector('#subscriptions')).toBeNull();
    expect(element.querySelector('#fees')).not.toBeNull();
  });

  it('mostra o estado sem vazamentos', async () => {
    service.detect.mockReturnValue(of(clear));
    configure();

    const { element } = await render();

    expect(element.querySelector('#leakTotal')?.textContent).toContain('Nenhum vazamento encontrado');
    expect(element.querySelector('#subscriptionsTotal')).toBeNull();
    expect(element.querySelector('table')).toBeNull();
  });

  it('busca de novo ao trocar o mês', async () => {
    configure();
    const { fixture, element } = await render();

    const input = element.querySelector<HTMLInputElement>('#month')!;
    input.value = '2025-12';
    input.dispatchEvent(new Event('change'));
    await fixture.whenStable();

    expect(service.detect).toHaveBeenLastCalledWith('2025-12');
  });

  it('oferece nova tentativa quando não carrega', async () => {
    service.detect.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    configure();

    const { element } = await render();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Não foi possível carregar');
    expect(element.textContent).toContain('Tentar de novo');
  });
});
