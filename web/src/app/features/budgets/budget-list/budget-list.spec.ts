import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Budget, BudgetStatus } from '../budget.model';
import { BudgetService } from '../budget.service';
import { BudgetList } from './budget-list';

function budget(id: string, status: BudgetStatus, percentUsed: number): Budget {
  return {
    id,
    categoryId: id,
    categoryName: `Categoria ${id}`,
    month: '2026-11',
    limit: 100,
    spent: percentUsed,
    remaining: 100 - percentUsed,
    percentUsed,
    status,
  };
}

describe('BudgetList', () => {
  const service = { list: vi.fn(), delete: vi.fn() };

  beforeEach(() => {
    vi.resetAllMocks();
    TestBed.configureTestingModule({
      imports: [BudgetList],
      providers: [
        provideRouter([]),
        { provide: BudgetService, useValue: service },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({ month: '2026-11' }) } },
        },
      ],
    });
  });

  async function render(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(BudgetList);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('usa o mês recebido na rota', async () => {
    service.list.mockReturnValue(of([]));

    await render();

    expect(service.list).toHaveBeenCalledWith('2026-11');
  });

  it('mostra o estado vazio', async () => {
    service.list.mockReturnValue(of([]));

    expect((await render()).textContent).toContain('Nenhum orçamento neste mês');
  });

  it('mostra situação e avisos de consumo', async () => {
    service.list.mockReturnValue(
      of([budget('a', 'OK', 10), budget('b', 'WARNING', 85), budget('c', 'EXCEEDED', 120)]),
    );

    const element = await render();

    expect(element.querySelector('.alert')?.textContent).toContain('1 orçamento estourado');
    expect(element.querySelector('.notice')?.textContent).toContain('1 orçamento passou');
    expect(element.querySelector('tbody')?.textContent).toContain('Estourado');
  });

  it('não mostra avisos quando está tudo dentro do limite', async () => {
    service.list.mockReturnValue(of([budget('a', 'OK', 10)]));

    const element = await render();

    expect(element.querySelector('.alert')).toBeNull();
    expect(element.querySelector('.notice')).toBeNull();
  });

  it('limita a barra a 100% quando o orçamento estoura', async () => {
    service.list.mockReturnValue(of([budget('c', 'EXCEEDED', 150)]));

    const element = await render();

    expect(element.querySelector('[role=progressbar]')?.getAttribute('aria-valuenow')).toBe('100');
  });

  it('oferece nova tentativa quando não carrega', async () => {
    service.list.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    const element = await render();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Não foi possível carregar');
    expect(element.textContent).toContain('Tentar de novo');
  });
});
