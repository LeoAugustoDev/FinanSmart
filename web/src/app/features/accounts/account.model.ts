export type AccountType = 'CHECKING' | 'SAVINGS' | 'WALLET' | 'INVESTMENT';

export interface Account {
  id: string;
  name: string;
  type: AccountType;
  initialBalance: number;
  currentBalance: number;
  currency: string;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AccountRequest {
  name: string;
  type: AccountType;
  initialBalance: number;
}

export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  CHECKING: 'Conta corrente',
  SAVINGS: 'Poupança',
  WALLET: 'Carteira',
  INVESTMENT: 'Investimento',
};

export const ACCOUNT_TYPES = Object.keys(ACCOUNT_TYPE_LABELS) as AccountType[];
