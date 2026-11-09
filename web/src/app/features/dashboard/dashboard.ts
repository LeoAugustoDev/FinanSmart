import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { environment } from '../../../environments/environment';

interface CurrentUser {
  id: string;
  username: string;
  name: string | null;
  email: string | null;
}

@Component({
  selector: 'app-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="card">
      @if (user.isLoading()) {
        <p>Carregando...</p>
      } @else if (user.error()) {
        <p role="alert">Não foi possível carregar seus dados.</p>
        <button type="button" class="primary" (click)="user.reload()">Tentar de novo</button>
      } @else if (user.hasValue()) {
        <h2>Olá, {{ user.value().name || user.value().username }}</h2>
        <p>{{ user.value().email }}</p>
      }
    </section>
  `,
})
export class Dashboard {
  protected readonly user = httpResource<CurrentUser>(() => `${environment.apiUrl}/me`);
}
