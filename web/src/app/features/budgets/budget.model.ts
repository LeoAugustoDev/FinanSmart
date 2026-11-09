export type BudgetStatus = 'OK' | 'WARNING' | 'EXCEEDED';

export interface Budget {
  id: string;
  categoryId: string;
  categoryName: string;
  month: string;
  limit: number;
  spent: number;
  remaining: number;
  percentUsed: number;
  status: BudgetStatus;
}

export interface CreateBudgetRequest {
  categoryId: string;
  month: string;
  limit: number;
}

export interface UpdateBudgetRequest {
  limit: number;
}

export const BUDGET_STATUS_LABELS: Record<BudgetStatus, string> = {
  OK: 'Dentro do limite',
  WARNING: 'Atenção',
  EXCEEDED: 'Estourado',
};
