import { Routes } from '@angular/router';

const loadForm = () => import('./category-form/category-form').then((m) => m.CategoryForm);

export const categoryRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./category-list/category-list').then((m) => m.CategoryList),
  },
  { path: 'nova', loadComponent: loadForm },
  { path: ':id/editar', loadComponent: loadForm },
];
