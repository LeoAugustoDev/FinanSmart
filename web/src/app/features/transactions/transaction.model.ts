import { CategoryType } from '../categories/category.model';

export interface Transaction {
  id: string;
  accountId: string;
  accountName: string;
  categoryId: string;
  categoryName: string;
  type: CategoryType;
  amount: number;
  occurredOn: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionRequest {
  accountId: string;
  categoryId: string;
  amount: number;
  occurredOn: string;
  description: string | null;
}

export interface TransactionFilters {
  from: string;
  to: string;
  accountId: string;
  categoryId: string;
  page: number;
  size: number;
}

export interface TransactionPage {
  content: Transaction[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
