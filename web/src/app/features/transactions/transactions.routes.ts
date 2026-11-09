import { Routes } from '@angular/router';

const loadForm = () => import('./transaction-form/transaction-form').then((m) => m.TransactionForm);

export const transactionRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./transaction-list/transaction-list').then((m) => m.TransactionList),
  },
  { path: 'novo', loadComponent: loadForm },
  { path: ':id/editar', loadComponent: loadForm },
];
