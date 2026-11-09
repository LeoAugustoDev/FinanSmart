import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
  },
  {
    path: 'painel',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'contas',
    canActivate: [authGuard],
    loadChildren: () => import('./features/accounts/accounts.routes').then((m) => m.accountRoutes),
  },
  {
    path: 'categorias',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/categories/categories.routes').then((m) => m.categoryRoutes),
  },
  {
    path: 'lancamentos',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/transactions/transactions.routes').then((m) => m.transactionRoutes),
  },
  {
    path: 'orcamentos',
    canActivate: [authGuard],
    loadChildren: () => import('./features/budgets/budgets.routes').then((m) => m.budgetRoutes),
  },
  {
    path: 'relatorio',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/reports/monthly-report/monthly-report').then((m) => m.MonthlyReportPage),
  },
  { path: '**', redirectTo: '' },
];
