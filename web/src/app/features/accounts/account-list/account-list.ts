import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { ACCOUNT_TYPE_LABELS, Account } from '../account.model';
import { AccountService } from '../account.service';

@Component({
  selector: 'app-account-list',
  imports: [CurrencyPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './account-list.html',
})
export class AccountList {
  private readonly service = inject(AccountService);

  protected readonly typeLabels = ACCOUNT_TYPE_LABELS;
  protected readonly showArchived = signal(false);
  protected readonly actionError = signal<string | null>(null);

  protected readonly accounts = rxResource({
    params: () => this.showArchived(),
    stream: ({ params: includeArchived }) => this.service.list(includeArchived),
    defaultValue: [] as Account[],
  });

  protected toggleArchived(event: Event): void {
    this.actionError.set(null);
    this.showArchived.set((event.target as HTMLInputElement).checked);
  }

  protected archive(account: Account): void {
    if (window.confirm(`Arquivar a conta "${account.name}"?`)) {
      this.run(this.service.archive(account.id), 'Não foi possível arquivar a conta.');
    }
  }

  protected unarchive(account: Account): void {
    this.run(
      this.service.unarchive(account.id),
      'Não foi possível desarquivar a conta.',
      'Já existe uma conta ativa com esse nome. Renomeie uma delas antes de desarquivar.',
    );
  }

  protected remove(account: Account): void {
    if (window.confirm(`Excluir a conta "${account.name}"? Essa ação não pode ser desfeita.`)) {
      this.run(
        this.service.delete(account.id),
        'Não foi possível excluir a conta.',
        'Esta conta tem lançamentos. Arquive-a em vez de excluir.',
      );
    }
  }

  private run(operation: Observable<void>, failure: string, conflict = failure): void {
    this.actionError.set(null);
    operation.subscribe({
      next: () => this.accounts.reload(),
      error: (error: HttpErrorResponse) => {
        this.actionError.set(error.status === 409 ? conflict : failure);
        this.accounts.reload();
      },
    });
  }
}
