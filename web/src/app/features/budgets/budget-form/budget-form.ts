import { formatDate } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';

import { Category } from '../../categories/category.model';
import { CategoryService } from '../../categories/category.service';
import { BudgetService } from '../budget.service';

const MONEY_PATTERN = /^\d{1,17}(\.\d{1,2})?$/;

import { DecimalInput } from '../../../shared/decimal-input';

@Component({
  selector: 'app-budget-form',
  imports: [DecimalInput, ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './budget-form.html',
})
export class BudgetForm {
  private readonly service = inject(BudgetService);
  private readonly categoryService = inject(CategoryService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly snapshot = inject(ActivatedRoute).snapshot;

  protected readonly budgetId = this.snapshot.paramMap.get('id');

  protected readonly loading = signal(true);
  protected readonly loadFailed = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly categories = signal<Category[]>([]);

  protected readonly form = this.formBuilder.group({
    categoryId: ['', [Validators.required]],
    month: [
      this.snapshot.queryParamMap.get('month') ?? formatDate(new Date(), 'yyyy-MM', 'en-US'),
      [Validators.required],
    ],
    limit: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.min(0.01),
      Validators.pattern(MONEY_PATTERN),
    ]),
  });

  constructor() {
    if (this.budgetId !== null) {
      // Depois de criado, só o limite do orçamento pode mudar.
      this.form.controls.categoryId.disable();
      this.form.controls.month.disable();
    }
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.loadFailed.set(false);
    forkJoin({
      categories: this.categoryService.list('EXPENSE'),
      budget: this.budgetId === null ? of(null) : this.service.get(this.budgetId),
    }).subscribe({
      next: ({ categories, budget }) => {
        this.categories.set(categories);
        if (budget !== null) {
          this.form.setValue({
            categoryId: budget.categoryId,
            month: budget.month,
            limit: budget.limit,
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

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { categoryId, month, limit } = this.form.getRawValue();
    const operation =
      this.budgetId === null
        ? this.service.create({ categoryId, month, limit: limit! })
        : this.service.update(this.budgetId, { limit: limit! });

    this.saving.set(true);
    this.saveError.set(null);
    operation.subscribe({
      next: () => void this.router.navigate(['/orcamentos'], { queryParams: { month } }),
      error: (error: HttpErrorResponse) => this.handleSaveError(error),
    });
  }

  private handleSaveError(error: HttpErrorResponse): void {
    let mapped = false;
    for (const [field, message] of Object.entries<string>(error.error?.errors ?? {})) {
      const control = this.form.get(field);
      if (control) {
        control.setErrors({ server: message });
        control.markAsTouched();
        mapped = true;
      }
    }
    if (!mapped) {
      this.saveError.set('Não foi possível salvar o orçamento. Tente novamente.');
    }
    this.saving.set(false);
  }
}
