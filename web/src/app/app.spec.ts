import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';
import { AuthService } from './core/auth/auth.service';

describe('App', () => {
  const authenticated = signal(false);

  beforeEach(async () => {
    authenticated.set(false);
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthenticated: authenticated } },
      ],
    }).compileComponents();
  });

  async function render(): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('mostra o nome do produto', async () => {
    expect((await render()).querySelector('h1')?.textContent).toContain('FinanSmart');
  });

  it('não mostra o botão de sair para visitantes', async () => {
    expect((await render()).querySelector('button')).toBeNull();
  });

  it('mostra o botão de sair para quem está conectado', async () => {
    authenticated.set(true);

    expect((await render()).querySelector('button')?.textContent).toContain('Sair');
  });
});
