import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, WritableSignal, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { Account } from '../../accounts/account.model';
import { AccountService } from '../../accounts/account.service';
import { Category } from '../../categories/category.model';
import { CategoryService } from '../../categories/category.service';
import { Transaction, TransactionFilters } from '../transaction.model';
import { TransactionService } from '../transaction.service';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-transaction-list',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './transaction-list.html',
})
export class TransactionList {
  private readonly service = inject(TransactionService);
  private readonly accountService = inject(AccountService);
  private readonly categoryService = inject(CategoryService);

  protected readonly from = signal('');
  protected readonly to = signal('');
  protected readonly accountId = signal('');
  protected readonly categoryId = signal('');
  protected readonly page = signal(0);
  protected readonly actionError = signal<string | null>(null);

  protected readonly invalidPeriod = computed(
    () => this.from() !== '' && this.to() !== '' && this.from() > this.to(),
  );
  protected readonly hasFilters = computed(
    () => !!(this.from() || this.to() || this.accountId() || this.categoryId()),
  );

  protected readonly accounts = rxResource({
    stream: () => this.accountService.list(true),
    defaultValue: [] as Account[],
  });

  protected readonly categories = rxResource({
    stream: () => this.categoryService.list(),
    defaultValue: [] as Category[],
  });

  // Ler value() de um recurso com erro lança exceção; sem opções, os filtros apenas ficam vazios.
  protected readonly accountOptions = computed(() =>
    this.accounts.hasValue() ? this.accounts.value() : [],
  );
  protected readonly categoryOptions = computed(() =>
    this.categories.hasValue() ? this.categories.value() : [],
  );

  protected readonly transactions = rxResource({
    params: (): TransactionFilters | undefined =>
      this.invalidPeriod()
        ? undefined
        : {
            from: this.from(),
            to: this.to(),
            accountId: this.accountId(),
            categoryId: this.categoryId(),
            page: this.page(),
            size: PAGE_SIZE,
          },
    stream: ({ params }) => this.service.list(params),
  });

  protected updateFilter(filter: WritableSignal<string>, event: Event): void {
    filter.set((event.target as HTMLInputElement | HTMLSelectElement).value);
    this.page.set(0);
    this.actionError.set(null);
  }

  protected clearFilters(): void {
    for (const filter of [this.from, this.to, this.accountId, this.categoryId]) {
      filter.set('');
    }
    this.page.set(0);
    this.actionError.set(null);
  }

  protected goToPage(page: number): void {
    this.actionError.set(null);
    this.page.set(page);
  }

  protected remove(transaction: Transaction, isLastOnPage: boolean): void {
    const label = transaction.description ?? transaction.categoryName;
    if (!window.confirm(`Excluir o lançamento "${label}"? Essa ação não pode ser desfeita.`)) {
      return;
    }

    this.actionError.set(null);
    this.service.delete(transaction.id).subscribe({
      next: () => {
        if (isLastOnPage && this.page() > 0) {
          this.page.update((page) => page - 1);
        } else {
          this.transactions.reload();
        }
      },
      error: () => {
        this.actionError.set('Não foi possível excluir o lançamento.');
        this.transactions.reload();
      },
    });
  }
}
