import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="card">
      <h2>Suas finanças em um só lugar</h2>

      @if (!auth.isAvailable()) {
        <p role="alert">Não foi possível conectar ao serviço de login.</p>
        <button type="button" class="primary" (click)="reload()">Tentar de novo</button>
      } @else if (auth.isAuthenticated()) {
        <p>Você já está conectado.</p>
        <a class="button primary" routerLink="/painel">Ir para o painel</a>
      } @else {
        <p>Acompanhe contas, lançamentos e orçamentos do seu dinheiro.</p>
        <div class="actions">
          <button type="button" class="primary" (click)="auth.login()">Entrar</button>
          <button type="button" (click)="auth.register()">Criar conta</button>
        </div>
      }
    </section>
  `,
})
export class Home {
  protected readonly auth = inject(AuthService);

  protected reload(): void {
    window.location.reload();
  }
}
