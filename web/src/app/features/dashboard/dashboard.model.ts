import { Account } from '../accounts/account.model';
import { Transaction } from '../transactions/transaction.model';

export interface DashboardData {
  month: string;
  totalBalance: number;
  income: number;
  expense: number;
  result: number;
  accounts: Account[];
  recentTransactions: Transaction[];
}
