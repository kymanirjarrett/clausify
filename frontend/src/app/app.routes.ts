import { Routes } from '@angular/router';
import { signedInGuard, signedOutGuard } from './core/auth.guards';

export const routes: Routes = [
  { path: '', title: 'Clausify · Contract review for freelancers', loadComponent: () => import('./features/landing/landing').then((m) => m.Landing) },
  { path: 'login', title: 'Log in · Clausify', canActivate: [signedOutGuard], loadComponent: () => import('./features/auth/login').then((m) => m.Login) },
  { path: 'signup', title: 'Create an account · Clausify', canActivate: [signedOutGuard], loadComponent: () => import('./features/auth/signup').then((m) => m.Signup) },
  {
    path: 'app',
    canActivate: [signedInGuard],
    loadComponent: () => import('./features/shell/shell').then((m) => m.Shell),
    children: [
      { path: '', title: 'Contracts · Clausify', loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard) },
      { path: 'contracts/:id', title: 'Contract · Clausify', loadComponent: () => import('./features/contract/contract-detail').then((m) => m.ContractDetailPage) },
      { path: 'account', title: 'Account · Clausify', loadComponent: () => import('./features/account/account').then((m) => m.Account) },
    ],
  },
  { path: '**', redirectTo: '' },
];
