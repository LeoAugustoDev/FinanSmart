import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { vi } from 'vitest';

import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  const authenticated = signal(false);
  const available = signal(true);
  const login = vi.fn().mockResolvedValue(undefined);

  const runGuard = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/painel?x=1' } as RouterStateSnapshot),
    );

  beforeEach(() => {
    authenticated.set(false);
    available.set(true);
    login.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { isAuthenticated: authenticated, isAvailable: available, login },
        },
      ],
    });
  });

  it('libera o usuário autenticado', async () => {
    authenticated.set(true);

    expect(await runGuard()).toBe(true);
    expect(login).not.toHaveBeenCalled();
  });

  it('leva ao login preservando a rota de destino', async () => {
    expect(await runGuard()).toBe(false);
    expect(login).toHaveBeenCalledWith('/painel?x=1');
  });

  it('volta ao início quando o Keycloak está indisponível', async () => {
    available.set(false);

    const result = await runGuard();

    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/');
    expect(login).not.toHaveBeenCalled();
  });
});
