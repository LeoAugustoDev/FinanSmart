import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Category } from '../../categories/category.model';
import { CategoryService } from '../../categories/category.service';
import { Budget } from '../budget.model';
import { BudgetService } from '../budget.service';
import { BudgetForm } from './budget-form';

describe('BudgetForm', () => {
  const service = { get: vi.fn(), create: vi.fn(), update: vi.fn() };
  const categories = { list: vi.fn() };

  const category: Category = {
    id: 'lazer',
    name: 'Lazer',
    type: 'EXPENSE',
    system: true,
    createdAt: '',
    updatedAt: '',
  };

  const budget: Budget = {
    id: '1',
    categoryId: 'lazer',
    categoryName: 'Lazer',
    month: '2026-11',
    limit: 300,
    spent: 0,
    remaining: 300,
    percentUsed: 0,
    status: 'OK',
  };

  function configure(budgetId: string | null): void {
    TestBed.configureTestingModule({
      imports: [BudgetForm],
      providers: [
        provideRouter([]),
        { provide: BudgetService, useValue: service },
        { provide: CategoryService, useValue: categories },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(budgetId ? { id: budgetId } : {}),
              queryParamMap: convertToParamMap({ month: '2026-11' }),
            },
          },
        },
      ],
    });
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  }

  async function render() {
    const fixture = TestBed.createComponent(BudgetForm);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    const type = (selector: string, value: string) => {
      const input = element.querySelector<HTMLInputElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    const select = async (selector: string, value: string) => {
      const input = element.querySelector<HTMLSelectElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('change'));
      await fixture.whenStable();
    };
    const submit = async () => {
      element.querySelector('form')!.dispatchEvent(new Event('submit'));
      fixture.changeDetectorRef.markForCheck();
      await fixture.whenStable();
    };

    return { element, type, select, submit };
  }

  beforeEach(() => {
    vi.resetAllMocks();
    categories.list.mockReturnValue(of([category]));
    service.create.mockReturnValue(of(budget));
    service.update.mockReturnValue(of(budget));
  });

  it('pede somente categorias de despesa', async () => {
    configure(null);

    await render();

    expect(categories.list).toHaveBeenCalledWith('EXPENSE');
  });

  it('não envia o formulário incompleto', async () => {
    configure(null);
    const { element, submit } = await render();

    await submit();

    expect(service.create).not.toHaveBeenCalled();
    expect(element.querySelectorAll('.field-error').length).toBe(2);
  });

  it('rejeita limite zero', async () => {
    configure(null);
    const { element, type, select, submit } = await render();
    await select('#categoryId', 'lazer');
    type('#limit', '0');

    await submit();

    expect(service.create).not.toHaveBeenCalled();
    expect(element.querySelector('.field-error')?.textContent).toContain('maior que zero');
  });

  it('cria o orçamento e volta para o mês dele', async () => {
    configure(null);
    const { type, select, submit } = await render();
    await select('#categoryId', 'lazer');
    type('#limit', '150.5');

    await submit();

    expect(service.create).toHaveBeenCalledWith({
      categoryId: 'lazer',
      month: '2026-11',
      limit: 150.5,
    });
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/orcamentos'], {
      queryParams: { month: '2026-11' },
    });
  });

  it('mostra na categoria o erro de orçamento repetido', async () => {
    configure(null);
    service.create.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { errors: { categoryId: 'Já existe um orçamento dessa categoria neste mês' } },
          }),
      ),
    );
    const { element, type, select, submit } = await render();
    await select('#categoryId', 'lazer');
    type('#limit', '100');

    await submit();

    expect(element.querySelector('.field-error')?.textContent).toContain('Já existe um orçamento');
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
  });

  it('na edição bloqueia categoria e mês e envia só o limite', async () => {
    configure('1');
    service.get.mockReturnValue(of(budget));
    const { element, type, submit } = await render();

    expect(element.querySelector<HTMLSelectElement>('#categoryId')?.disabled).toBe(true);
    expect(element.querySelector<HTMLInputElement>('#month')?.disabled).toBe(true);
    expect(element.querySelector<HTMLInputElement>('#limit')?.value).toBe('300');

    type('#limit', '400');
    await submit();

    expect(service.update).toHaveBeenCalledWith('1', { limit: 400 });
  });

  it('oferece nova tentativa quando não carrega', async () => {
    configure('1');
    service.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));

    const { element } = await render();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Não foi possível');
    expect(element.querySelector('form')).toBeNull();
  });
});
