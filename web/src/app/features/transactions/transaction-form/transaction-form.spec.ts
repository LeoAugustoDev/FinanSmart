import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Account } from '../../accounts/account.model';
import { AccountService } from '../../accounts/account.service';
import { Category } from '../../categories/category.model';
import { CategoryService } from '../../categories/category.service';
import { Transaction } from '../transaction.model';
import { TransactionService } from '../transaction.service';
import { TransactionForm } from './transaction-form';

function account(id: string, archived = false): Account {
  return {
    id,
    name: `Conta ${id}`,
    type: 'CHECKING',
    initialBalance: 0,
    currency: 'BRL',
    archived,
    createdAt: '',
    updatedAt: '',
  };
}

function category(id: string, type: Category['type']): Category {
  return { id, name: id, type, system: true, createdAt: '', updatedAt: '' };
}

describe('TransactionForm', () => {
  const service = { get: vi.fn(), create: vi.fn(), update: vi.fn() };
  const accounts = { list: vi.fn() };
  const categories = { list: vi.fn() };

  const transaction: Transaction = {
    id: '1',
    accountId: 'a2',
    accountName: 'Conta a2',
    categoryId: 'salario',
    categoryName: 'salario',
    type: 'INCOME',
    amount: 100,
    occurredOn: '2026-11-01',
    description: null,
    createdAt: '',
    updatedAt: '',
  };

  function configure(transactionId: string | null): void {
    TestBed.configureTestingModule({
      imports: [TransactionForm],
      providers: [
        provideRouter([]),
        { provide: TransactionService, useValue: service },
        { provide: AccountService, useValue: accounts },
        { provide: CategoryService, useValue: categories },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap(transactionId ? { id: transactionId } : {}) },
          },
        },
      ],
    });
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  }

  async function render() {
    const fixture = TestBed.createComponent(TransactionForm);
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
    const fillValid = async () => {
      await select('#categoryId', 'lazer');
      await select('#accountId', 'a1');
      type('#amount', '10.5');
      type('#occurredOn', '2026-11-09');
    };
    const optionValues = (selector: string) =>
      [...element.querySelectorAll<HTMLOptionElement>(`${selector} option`)].map((o) => o.value);

    return { element, type, select, submit, fillValid, optionValues };
  }

  beforeEach(() => {
    vi.resetAllMocks();
    accounts.list.mockReturnValue(of([account('a1'), account('a2', true)]));
    categories.list.mockReturnValue(of([category('lazer', 'EXPENSE'), category('salario', 'INCOME')]));
    service.create.mockReturnValue(of(transaction));
    service.update.mockReturnValue(of(transaction));
  });

  it('não envia o formulário incompleto e mostra os erros', async () => {
    configure(null);
    const { element, submit } = await render();

    await submit();

    expect(service.create).not.toHaveBeenCalled();
    expect(element.querySelectorAll('.field-error').length).toBe(3);
  });

  it('rejeita valor zero', async () => {
    configure(null);
    const { element, type, fillValid, submit } = await render();
    await fillValid();
    type('#amount', '0');

    await submit();

    expect(service.create).not.toHaveBeenCalled();
    expect(element.querySelector('.field-error')?.textContent).toContain('maior que zero');
  });

  it('rejeita valor com mais de 2 casas decimais', async () => {
    configure(null);
    const { element, type, fillValid, submit } = await render();
    await fillValid();
    type('#amount', '1.999');

    await submit();

    expect(service.create).not.toHaveBeenCalled();
    expect(element.querySelector('.field-error')?.textContent).toContain('2 casas');
  });

  it('não oferece contas arquivadas em um novo lançamento', async () => {
    configure(null);
    const { optionValues } = await render();

    expect(optionValues('#accountId')).toEqual(['', 'a1']);
  });

  it('mostra só as categorias do tipo escolhido e limpa a seleção ao trocar', async () => {
    configure(null);
    const { element, select, optionValues } = await render();
    expect(optionValues('#categoryId')).toEqual(['', 'lazer']);
    await select('#categoryId', 'lazer');

    await select('#type', 'INCOME');

    expect(optionValues('#categoryId')).toEqual(['', 'salario']);
    expect(element.querySelector<HTMLSelectElement>('#categoryId')?.value).toBe('');
  });

  it('cria o lançamento sem descrição vazia e volta para a lista', async () => {
    configure(null);
    const { type, fillValid, submit } = await render();
    await fillValid();
    type('#description', '   ');

    await submit();

    expect(service.create).toHaveBeenCalledWith({
      accountId: 'a1',
      categoryId: 'lazer',
      amount: 10.5,
      occurredOn: '2026-11-09',
      description: null,
    });
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/lancamentos']);
  });

  it('mostra no campo o erro devolvido pela API', async () => {
    configure(null);
    service.create.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: { errors: { accountId: 'Contas arquivadas não recebem lançamentos' } },
          }),
      ),
    );
    const { element, fillValid, submit } = await render();
    await fillValid();

    await submit();

    expect(element.querySelector('.field-error')?.textContent).toContain('arquivadas');
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
  });

  it('mostra erro geral nas demais falhas', async () => {
    configure(null);
    service.create.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    const { element, fillValid, submit } = await render();
    await fillValid();

    await submit();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Não foi possível salvar');
  });

  it('carrega um lançamento existente, mantendo a conta arquivada dele', async () => {
    configure('1');
    service.get.mockReturnValue(of(transaction));
    const { element, type, submit, optionValues } = await render();

    expect(optionValues('#accountId')).toEqual(['', 'a1', 'a2']);
    expect(element.querySelector<HTMLSelectElement>('#accountId')?.value).toBe('a2');
    expect(element.querySelector<HTMLSelectElement>('#categoryId')?.value).toBe('salario');
    expect(element.querySelector<HTMLSelectElement>('#type')?.value).toBe('INCOME');

    type('#amount', '200');
    await submit();

    expect(service.update).toHaveBeenCalledWith('1', {
      accountId: 'a2',
      categoryId: 'salario',
      amount: 200,
      occurredOn: '2026-11-01',
      description: null,
    });
  });

  it('avisa quando não há conta ativa', async () => {
    configure(null);
    accounts.list.mockReturnValue(of([account('a2', true)]));
    const { element } = await render();

    expect(element.querySelector('form')).toBeNull();
    expect(element.textContent).toContain('conta ativa');
  });

  it('oferece nova tentativa quando os dados não carregam', async () => {
    configure(null);
    categories.list.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    const { element } = await render();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Não foi possível');
  });
});
