import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AccountService } from '../../accounts/account.service';
import { CategoryService } from '../../categories/category.service';
import { Transaction, TransactionPage } from '../transaction.model';
import { TransactionService } from '../transaction.service';
import { TransactionList } from './transaction-list';

describe('TransactionList', () => {
  const service = { list: vi.fn(), delete: vi.fn() };
  const accounts = { list: vi.fn() };
  const categories = { list: vi.fn() };

  const transaction: Transaction = {
    id: '1',
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
  };

  const page = (content: Transaction[], overrides: Partial<TransactionPage> = {}): TransactionPage => ({
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    ...overrides,
  });

  const failure = () => throwError(() => new HttpErrorResponse({ status: 500 }));

  async function render(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(TransactionList);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    vi.resetAllMocks();
    accounts.list.mockReturnValue(of([]));
    categories.list.mockReturnValue(of([]));
    service.list.mockReturnValue(of(page([transaction])));
    TestBed.configureTestingModule({
      imports: [TransactionList],
      providers: [
        provideRouter([]),
        { provide: TransactionService, useValue: service },
        { provide: AccountService, useValue: accounts },
        { provide: CategoryService, useValue: categories },
      ],
    });
  });

  it('mostra os lançamentos', async () => {
    const element = await render();

    expect(element.querySelector('tbody')?.textContent).toContain('Cinema');
  });

  it('mostra o estado vazio', async () => {
    service.list.mockReturnValue(of(page([])));

    expect((await render()).textContent).toContain('Nenhum lançamento ainda');
  });

  it('oferece nova tentativa quando os lançamentos não carregam', async () => {
    service.list.mockReturnValue(failure());
    const element = await render();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Não foi possível carregar');
    expect(element.textContent).toContain('Tentar de novo');
  });

  it('continua funcionando quando as opções dos filtros não carregam', async () => {
    accounts.list.mockReturnValue(failure());
    categories.list.mockReturnValue(failure());

    expect((await render()).querySelector('tbody')?.textContent).toContain('Cinema');
  });

  it('desabilita a navegação nos limites da paginação', async () => {
    const element = await render();
    const buttons = element.querySelectorAll<HTMLButtonElement>('.pagination button');

    expect(buttons[0].disabled).toBe(true);
    expect(buttons[1].disabled).toBe(true);
  });

  it('avança de página', async () => {
    service.list.mockReturnValue(of(page([transaction], { totalPages: 2 })));
    const fixture = TestBed.createComponent(TransactionList);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    element.querySelectorAll<HTMLButtonElement>('.pagination button')[1].click();
    await fixture.whenStable();

    expect(service.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));
  });
});
