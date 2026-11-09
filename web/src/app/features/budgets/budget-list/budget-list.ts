import { CurrencyPipe, DecimalPipe, formatDate } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { BUDGET_STATUS_LABELS, Budget, BudgetStatus } from '../budget.model';
import { BudgetService } from '../budget.service';

@Component({
  selector: 'app-budget-list',
  imports: [CurrencyPipe, DecimalPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './budget-list.html',
})
export class BudgetList {
  private readonly service = inject(BudgetService);

  protected readonly statusLabels = BUDGET_STATUS_LABELS;
  protected readonly month = signal(
    inject(ActivatedRoute).snapshot.queryParamMap.get('month') ??
      formatDate(new Date(), 'yyyy-MM', 'en-US'),
  );
  protected readonly actionError = signal<string | null>(null);

  protected readonly budgets = rxResource({
    params: () => this.month() || undefined,
    stream: ({ params: month }) => this.service.list(month),
    defaultValue: [] as Budget[],
  });

  protected readonly exceededCount = computed(() => this.countByStatus('EXCEEDED'));
  protected readonly warningCount = computed(() => this.countByStatus('WARNING'));

  protected changeMonth(event: Event): void {
    this.actionError.set(null);
    this.month.set((event.target as HTMLInputElement).value);
  }

  protected barWidth(budget: Budget): number {
    return Math.min(budget.percentUsed, 100);
  }

  protected remove(budget: Budget): void {
    if (!window.confirm(`Excluir o orçamento de "${budget.categoryName}"?`)) {
      return;
    }

    this.actionError.set(null);
    this.service.delete(budget.id).subscribe({
      next: () => this.budgets.reload(),
      error: () => {
        this.actionError.set('Não foi possível excluir o orçamento.');
        this.budgets.reload();
      },
    });
  }

  private countByStatus(status: BudgetStatus): number {
    // Ler value() de um recurso com erro lança exceção.
    const budgets = this.budgets.hasValue() ? this.budgets.value() : [];
    return budgets.filter((budget) => budget.status === status).length;
  }
}
