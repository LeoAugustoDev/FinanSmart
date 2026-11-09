import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ACCOUNT_TYPES, ACCOUNT_TYPE_LABELS, AccountRequest, AccountType } from '../account.model';
import { AccountService } from '../account.service';

const NAME_MAX_LENGTH = 60;
const MONEY_PATTERN = /^-?\d{1,17}(\.\d{1,2})?$/;

import { DecimalInput } from '../../../shared/decimal-input';

@Component({
  selector: 'app-account-form',
  imports: [DecimalInput, ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './account-form.html',
})
export class AccountForm {
  private readonly service = inject(AccountService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly accountId = inject(ActivatedRoute).snapshot.paramMap.get('id');
  protected readonly types = ACCOUNT_TYPES;
  protected readonly typeLabels = ACCOUNT_TYPE_LABELS;
  protected readonly nameMaxLength = NAME_MAX_LENGTH;

  protected readonly loading = signal(this.accountId !== null);
  protected readonly loadFailed = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);

  protected readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(NAME_MAX_LENGTH)]],
    type: ['CHECKING' as AccountType, [Validators.required]],
    initialBalance: this.formBuilder.control<number | null>(null, [
      Validators.required,
      Validators.pattern(MONEY_PATTERN),
    ]),
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    if (this.accountId === null) {
      return;
    }

    this.loading.set(true);
    this.loadFailed.set(false);
    this.service.get(this.accountId).subscribe({
      next: ({ name, type, initialBalance }) => {
        this.form.setValue({ name, type, initialBalance });
        this.loading.set(false);
      },
      error: () => {
        this.loadFailed.set(true);
        this.loading.set(false);
      },
    });
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { name, type, initialBalance } = this.form.getRawValue();
    const request: AccountRequest = { name: name.trim(), type, initialBalance: initialBalance! };
    const operation =
      this.accountId === null
        ? this.service.create(request)
        : this.service.update(this.accountId, request);

    this.saving.set(true);
    this.saveError.set(null);
    operation.subscribe({
      next: () => void this.router.navigate(['/contas']),
      error: (error: HttpErrorResponse) => this.handleSaveError(error),
    });
  }

  private handleSaveError(error: HttpErrorResponse): void {
    if (error.status === 409) {
      this.form.controls.name.setErrors({ duplicate: true });
      this.form.controls.name.markAsTouched();
    } else {
      this.saveError.set('Não foi possível salvar a conta. Tente novamente.');
    }
    this.saving.set(false);
  }
}
