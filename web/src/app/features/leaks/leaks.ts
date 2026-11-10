import { CurrencyPipe, formatDate } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { LeakService } from './leak.service';

@Component({
  selector: 'app-leaks',
  imports: [CurrencyPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './leaks.html',
})
export class LeaksPage {
  private readonly service = inject(LeakService);

  private readonly currentMonth = formatDate(new Date(), 'yyyy-MM', 'en-US');

  protected readonly month = signal(
    inject(ActivatedRoute).snapshot.queryParamMap.get('month') ?? this.currentMonth,
  );

  protected readonly leaks = rxResource({
    params: () => this.month() || undefined,
    stream: ({ params: month }) => this.service.detect(month),
  });

  protected readonly period = computed(() =>
    this.month() === this.currentMonth ? 'este mês' : 'no mês',
  );

  protected changeMonth(event: Event): void {
    this.month.set((event.target as HTMLInputElement).value);
  }
}
