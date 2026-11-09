import { Routes } from '@angular/router';

const loadForm = () => import('./budget-form/budget-form').then((m) => m.BudgetForm);

export const budgetRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./budget-list/budget-list').then((m) => m.BudgetList),
  },
  { path: 'novo', loadComponent: loadForm },
  { path: ':id/editar', loadComponent: loadForm },
];
