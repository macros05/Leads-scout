import { Routes } from '@angular/router';
import {authGuard} from './guards/auth.guard';
import {LoginComponent} from './auth/login/login';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./leads/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'leads',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./leads/leads-list/leads-list.component').then(m => m.LeadsListComponent)
  },
  {
    path: 'scout-search',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./leads/scout-search/scout-search.component').then(m => m.ScoutSearchComponent)
  },
  {
    path: 'audit/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./leads/audit-detail/audit-detail.component').then(m => m.AuditDetailComponent)
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
