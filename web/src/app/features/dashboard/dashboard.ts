import { CurrencyPipe, DatePipe, formatDate } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { environment } from '../../../environments/environment';
import { ACCOUNT_TYPE_LABELS } from '../accounts/account.model';
import { DashboardService } from './dashboard.service';

interface CurrentUser {
  id: string;
  username: string;
  name: string | null;
  email: string | null;
}

@Component({
  selector: 'app-dashboard',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.html',
})
export class Dashboard {
  private readonly service = inject(DashboardService);

  protected readonly typeLabels = ACCOUNT_TYPE_LABELS;

  protected readonly user = httpResource<CurrentUser>(() => `${environment.apiUrl}/me`);
  protected readonly greeting = computed(() => {
    const user = this.user.hasValue() ? this.user.value() : undefined;
    return user ? `Olá, ${user.name || user.username}` : 'Painel';
  });

  protected readonly dashboard = rxResource({
    stream: () => this.service.get(formatDate(new Date(), 'yyyy-MM', 'en-US')),
  });
}
