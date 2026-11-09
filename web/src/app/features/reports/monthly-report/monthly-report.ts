import { CurrencyPipe, DatePipe, DecimalPipe, formatDate } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Comparison, MonthlyReport } from '../report.model';
import { ReportService } from '../report.service';

interface SummaryCard {
  id: string;
  label: string;
  comparison: Comparison;
  /** Em despesas, subir é ruim; em receitas e resultado, subir é bom. */
  increaseIsGood: boolean;
}

@Component({
  selector: 'app-monthly-report',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './monthly-report.html',
})
export class MonthlyReportPage {
  private readonly service = inject(ReportService);

  protected readonly month = signal(
    inject(ActivatedRoute).snapshot.queryParamMap.get('month') ??
      formatDate(new Date(), 'yyyy-MM', 'en-US'),
  );

  protected readonly report = rxResource({
    params: () => this.month() || undefined,
    stream: ({ params: month }) => this.service.monthly(month),
  });

  // Ler value() de um recurso com erro lança exceção.
  private readonly data = computed<MonthlyReport | undefined>(() =>
    this.report.hasValue() ? this.report.value() : undefined,
  );

  protected readonly cards = computed<SummaryCard[]>(() => {
    const data = this.data();
    if (!data) {
      return [];
    }
    return [
      { id: 'income', label: 'Receitas', comparison: data.income, increaseIsGood: true },
      { id: 'expense', label: 'Despesas', comparison: data.expense, increaseIsGood: false },
      { id: 'result', label: 'Resultado', comparison: data.result, increaseIsGood: true },
    ];
  });

  protected readonly isEmpty = computed(() => {
    const data = this.data();
    return (
      !!data &&
      data.expensesByCategory.length === 0 &&
      [data.income, data.expense].every((item) => item.current === 0 && item.previous === 0)
    );
  });

  /** As quatro barras de receitas e despesas usam a mesma escala. */
  protected readonly flowMax = computed(() => {
    const data = this.data();
    return data
      ? Math.max(data.income.current, data.income.previous, data.expense.current, data.expense.previous)
      : 0;
  });

  protected readonly spentCategories = computed(() =>
    (this.data()?.expensesByCategory ?? []).filter((category) => category.total > 0),
  );

  protected readonly categoryMax = computed(() =>
    Math.max(0, ...this.spentCategories().map((category) => category.total)),
  );

  protected changeMonth(event: Event): void {
    this.month.set((event.target as HTMLInputElement).value);
  }

  protected width(value: number, max: number): number {
    return max > 0 ? (value / max) * 100 : 0;
  }

  protected abs(value: number): number {
    return Math.abs(value);
  }

  protected arrow(change: number): string {
    return change > 0 ? '▲' : change < 0 ? '▼' : '=';
  }

  protected direction(change: number): string {
    return change > 0 ? 'Aumento de' : change < 0 ? 'Queda de' : 'Sem variação:';
  }
}
