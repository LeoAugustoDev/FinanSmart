export type CategoryType = 'INCOME' | 'EXPENSE';

export interface Category {
  id: string;
  name: string;
  type: CategoryType;
  system: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCategoryRequest {
  name: string;
  type: CategoryType;
}

export interface UpdateCategoryRequest {
  name: string;
}

export const CATEGORY_TYPE_LABELS: Record<CategoryType, string> = {
  EXPENSE: 'Despesa',
  INCOME: 'Receita',
};

export const CATEGORY_TYPES = Object.keys(CATEGORY_TYPE_LABELS) as CategoryType[];
