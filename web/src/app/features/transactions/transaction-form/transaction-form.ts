import { formatDate } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';

import { Account } from '../../accounts/account.model';
import { AccountService } from '../../accounts/account.service';
import {
  CATEGORY_TYPES,
  CATEGORY_TYPE_LABELS,
  Category,
  CategoryType,
} from '../../categories/category.model';
import { CategoryService } from '../../categories/category.service';
import { TransactionRequest } from '../transaction.model';
import { TransactionService } from '../transaction.service';

const DESCRIPTION_MAX_LENGTH = 255;
const MONEY_PATTERN = /^\d{1,17}(\.\d{1,2})?$/;

import { DecimalInput } from '../../../shared/decimal-input';

@Component({
  selector: 'app-transaction-form',
  imports: [DecimalInput, ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './transaction-form.html',
})
export class TransactionForm {
  private readonly service = inject(TransactionService);
  private readonly accountService = inject(AccountService);
  private readonly categoryService = inject(CategoryService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly transactionId = inject(ActivatedRoute).snapshot.paramMap.get('id');
  protected readonly types = CATEGORY_TYPES;
  protected readonly typeLabels = CATEGORY_TYPE_LABELS;

  protected readonly loading = signal(true);
  protected readonly loadFailed = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);

  protected readonly type = signal<CategoryType>('EXPENSE');
  private readonly accounts = signal<Account[]>([]);
  private readonly categories = signal<Category[]>([]);
  private readonly currentAccountId = signal<string | null>(null);

  /** Contas arquivadas só aparecem para o lançamento que já está nelas. */
  protected readonly selectableAccounts = computed(() =>
    this.accounts().filter((account) => !account.archived || account.id === this.currentAccountId()),
  );
  protected readonly visibleCategories = computed(() =>
    this.categories().filter((category) => category.type === this.type()),
  );

  protected readonly form = this.formBuilder.group({
    accountId: ['', [Validators.required]],
    categoryId: ['', [Validators.required]],
    amount: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      Validators.pattern(MONEY_PATTERN),
    ]),
    occurredOn: [formatDate(new Date(), 'yyyy-MM-dd', 'en-US'), [Validators.required]],
    description: ['', [Validators.maxLength(DESCRIPTION_MAX_LENGTH)]],
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.loadFailed.set(false);
    forkJoin({
      accounts: this.accountService.list(true),
      categories: this.categoryService.list(),
      transaction: this.transactionId === null ? of(null) : this.service.get(this.transactionId),
    }).subscribe({
      next: ({ accounts, categories, transaction }) => {
        this.accounts.set(accounts);
        this.categories.set(categories);
        if (transaction !== null) {
          this.currentAccountId.set(transaction.accountId);
          this.type.set(transaction.type);
          this.form.setValue({
            accountId: transaction.accountId,
            categoryId: transaction.categoryId,
            amount: transaction.amount,
            occurredOn: transaction.occurredOn,
            description: transaction.description ?? '',
          });
        }
        this.loading.set(false);
      },
      error: () => {
        this.loadFailed.set(true);
        this.loading.set(false);
      },
    });
  }

  protected changeType(event: Event): void {
    this.type.set((event.target as HTMLSelectElement).value as CategoryType);
    this.form.controls.categoryId.reset();
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { accountId, categoryId, amount, occurredOn, description } = this.form.getRawValue();
    const request: TransactionRequest = {
      accountId,
      categoryId,
      amount: amount!,
      occurredOn,
      description: description.trim() || null,
    };
    const operation =
      this.transactionId === null
        ? this.service.create(request)
        : this.service.update(this.transactionId, request);

    this.saving.set(true);
    this.saveError.set(null);
    operation.subscribe({
      next: () => void this.router.navigate(['/lancamentos']),
      error: (error: HttpErrorResponse) => this.handleSaveError(error),
    });
  }

  private handleSaveError(error: HttpErrorResponse): void {
    const fieldErrors = Object.entries<string>(error.error?.errors ?? {});
    let mapped = false;
    for (const [field, message] of fieldErrors) {
      const control = this.form.get(field);
      if (control) {
        control.setErrors({ server: message });
        control.markAsTouched();
        mapped = true;
      }
    }
    if (!mapped) {
      this.saveError.set('Não foi possível salvar o lançamento. Tente novamente.');
    }
    this.saving.set(false);
  }
}
