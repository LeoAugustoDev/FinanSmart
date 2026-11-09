import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { MonthlyReport } from '../report.model';
import { ReportService } from '../report.service';
import { MonthlyReportPage } from './monthly-report';

describe('MonthlyReportPage', () => {
  const service = { monthly: vi.fn() };

  const report: MonthlyReport = {
    month: '2026-11',
    previousMonth: '2026-10',
    income: { current: 3000, previous: 2500, change: 500, changePercent: 20 },
    expense: { current: 1200, previous: 1500, change: -300, changePercent: -20 },
    result: { current: 1800, previous: 1000, change: 800, changePercent: 80 },
    expensesByCategory: [
      {
        categoryId: 'food',
        categoryName: 'Alimentação',
        total: 900,
        share: 75,
        previousTotal: 600,
        change: 300,
        changePercent: 50,
      },
      {
        categoryId: 'leisure',
        categoryName: 'Lazer',
        total: 300,
        share: 25,
        previousTotal: 0,
        change: 300,
        changePercent: null,
      },
      {
        categoryId: 'transport',
        categoryName: 'Transporte',
        total: 0,
        share: 0,
        previousTotal: 900,
        change: -900,
        changePercent: -100,
      },
    ],
  };

  const zero = { current: 0, previous: 0, change: 0, changePercent: null };
  const empty: MonthlyReport = {
    ...report,
    income: zero,
    expense: zero,
    result: zero,
    expensesByCategory: [],
  };

  function configure(month: string | null = '2026-11'): void {
    TestBed.configureTestingModule({
      imports: [MonthlyReportPage],
      providers: [
        provideRouter([]),
        { provide: ReportService, useValue: service },
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
    const fixture = TestBed.createComponent(MonthlyReportPage);
    await fixture.whenStable();
    return { fixture, element: fixture.nativeElement as HTMLElement };
  }

  beforeEach(() => {
    vi.resetAllMocks();
    service.monthly.mockReturnValue(of(report));
  });

  it('usa o mês recebido na rota', async () => {
    configure();

    await render();

    expect(service.monthly).toHaveBeenCalledWith('2026-11');
  });

  it('usa o mês atual quando a rota não informa', async () => {
    configure(null);

    await render();

    expect(service.monthly).toHaveBeenCalledWith(expect.stringMatching(/^\d{4}-\d{2}$/));
  });

  it('mostra os totais com a variação sobre o mês anterior', async () => {
    configure();

    const { element } = await render();

    const income = element.querySelector('#income')!;
    expect(income.textContent).toContain('3,000.00');
    expect(income.textContent).toContain('500.00');
    expect(income.textContent).toContain('(20%)');
    expect(income.textContent).toContain('2,500.00');
    expect(element.querySelector('#result')?.textContent).toContain('1,800.00');
  });

  it('trata aumento de receita como bom e aumento de despesa como ruim', async () => {
    service.monthly.mockReturnValue(
      of({ ...report, expense: { current: 1500, previous: 1200, change: 300, changePercent: 25 } }),
    );
    configure();

    const { element } = await render();

    expect(element.querySelector('#income .change')?.classList).toContain('change-good');
    expect(element.querySelector('#expense .change')?.classList).toContain('change-bad');
    expect(element.querySelector('#expense .change')?.textContent).toContain('Aumento de');
  });

  it('trata queda de despesa como boa', async () => {
    configure();

    const { element } = await render();

    expect(element.querySelector('#expense .change')?.classList).toContain('change-good');
    expect(element.querySelector('#expense .change')?.textContent).toContain('Queda de');
  });

  it('desenha receitas e despesas na mesma escala', async () => {
    configure();

    const { element } = await render();

    const widths = Array.from(element.querySelectorAll<HTMLElement>('#flowChart .bar')).map((bar) =>
      Math.round(parseFloat(bar.style.width)),
    );
    expect(widths).toEqual([100, 83, 40, 50]);
    expect(element.querySelectorAll('#flowChart .chart-legend span').length).toBe(2);
  });

  it('desenha uma barra por categoria com gasto no mês', async () => {
    configure();

    const { element } = await render();

    const groups = element.querySelectorAll<HTMLElement>('#categoryChart .bar-group');
    expect(groups.length).toBe(2);
    expect(groups[0].textContent).toContain('Alimentação');
    expect(groups[0].textContent).toContain('75%');
    expect(parseFloat(groups[1].querySelector<HTMLElement>('.bar')!.style.width)).toBeCloseTo(33.33, 1);
  });

  it('lista na tabela também a categoria que só teve gasto no mês anterior', async () => {
    configure();

    const { element } = await render();

    const rows = element.querySelectorAll('#categories tbody tr');
    expect(rows.length).toBe(3);
    expect(rows[2].textContent).toContain('Transporte');
    expect(rows[2].textContent).toContain('(100%)');
    expect(rows[2].querySelector('.change-good')).not.toBeNull();
    expect(rows[0].querySelector('.change-bad')).not.toBeNull();
  });

  it('omite o percentual quando não há base de comparação', async () => {
    configure();

    const { element } = await render();

    const leisure = element.querySelectorAll('#categories tbody tr')[1];
    expect(leisure.children[3].textContent).toContain('300.00');
    expect(leisure.children[3].textContent).not.toContain('%');
  });

  it('mostra o estado vazio', async () => {
    service.monthly.mockReturnValue(of(empty));
    configure();

    const { element } = await render();

    expect(element.textContent).toContain('Nenhum lançamento neste mês nem no mês anterior');
    expect(element.querySelector('#flowChart')).toBeNull();
  });

  it('avisa quando só há receitas', async () => {
    service.monthly.mockReturnValue(of({ ...empty, income: report.income, result: report.income }));
    configure();

    const { element } = await render();

    expect(element.querySelector('#flowChart')).not.toBeNull();
    expect(element.textContent).toContain('Nenhuma despesa neste mês nem no mês anterior');
    expect(element.querySelector('#categories')).toBeNull();
  });

  it('busca de novo ao trocar o mês', async () => {
    configure();
    const { fixture, element } = await render();

    const input = element.querySelector<HTMLInputElement>('#month')!;
    input.value = '2026-10';
    input.dispatchEvent(new Event('change'));
    await fixture.whenStable();

    expect(service.monthly).toHaveBeenLastCalledWith('2026-10');
  });

  it('oferece nova tentativa quando não carrega', async () => {
    service.monthly.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    configure();

    const { element } = await render();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Não foi possível carregar');
    expect(element.textContent).toContain('Tentar de novo');
  });
});
