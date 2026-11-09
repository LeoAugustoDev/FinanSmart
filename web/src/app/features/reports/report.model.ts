export interface Comparison {
  current: number;
  previous: number;
  change: number;
  /** Nulo quando o mês anterior é zero e não há base para o percentual. */
  changePercent: number | null;
}

export interface CategorySpending {
  categoryId: string;
  categoryName: string;
  total: number;
  /** Participação no total de despesas do mês, de 0 a 100. */
  share: number;
  previousTotal: number;
  change: number;
  changePercent: number | null;
}

export interface MonthlyReport {
  month: string;
  previousMonth: string;
  income: Comparison;
  expense: Comparison;
  result: Comparison;
  expensesByCategory: CategorySpending[];
}
