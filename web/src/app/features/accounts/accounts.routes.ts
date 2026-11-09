import { Routes } from '@angular/router';

const loadForm = () => import('./account-form/account-form').then((m) => m.AccountForm);

export const accountRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./account-list/account-list').then((m) => m.AccountList),
  },
  { path: 'nova', loadComponent: loadForm },
  { path: ':id/editar', loadComponent: loadForm },
];
