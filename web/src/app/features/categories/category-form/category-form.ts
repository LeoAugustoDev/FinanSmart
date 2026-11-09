import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { CATEGORY_TYPES, CATEGORY_TYPE_LABELS, CategoryType } from '../category.model';
import { CategoryService } from '../category.service';

const NAME_MAX_LENGTH = 60;

@Component({
  selector: 'app-category-form',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './category-form.html',
})
export class CategoryForm {
  private readonly service = inject(CategoryService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(NonNullableFormBuilder);

  protected readonly categoryId = inject(ActivatedRoute).snapshot.paramMap.get('id');
  protected readonly types = CATEGORY_TYPES;
  protected readonly typeLabels = CATEGORY_TYPE_LABELS;
  protected readonly nameMaxLength = NAME_MAX_LENGTH;

  protected readonly loading = signal(this.categoryId !== null);
  protected readonly loadFailed = signal(false);
  protected readonly readOnly = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);

  protected readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.pattern(/\S/), Validators.maxLength(NAME_MAX_LENGTH)]],
    type: ['EXPENSE' as CategoryType, [Validators.required]],
  });

  constructor() {
    if (this.categoryId !== null) {
      // O tipo define o sinal dos lançamentos já feitos, então não muda depois de criado.
      this.form.controls.type.disable();
    }
    this.load();
  }

  protected load(): void {
    if (this.categoryId === null) {
      return;
    }

    this.loading.set(true);
    this.loadFailed.set(false);
    this.service.get(this.categoryId).subscribe({
      next: ({ name, type, system }) => {
        this.form.setValue({ name, type });
        this.readOnly.set(system);
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

    const { name, type } = this.form.getRawValue();
    const trimmedName = name.trim();
    const operation =
      this.categoryId === null
        ? this.service.create({ name: trimmedName, type })
        : this.service.update(this.categoryId, { name: trimmedName });

    this.saving.set(true);
    this.saveError.set(null);
    operation.subscribe({
      next: () => void this.router.navigate(['/categorias']),
      error: (error: HttpErrorResponse) => this.handleSaveError(error),
    });
  }

  private handleSaveError(error: HttpErrorResponse): void {
    if (error.status === 409) {
      this.form.controls.name.setErrors({ duplicate: true });
      this.form.controls.name.markAsTouched();
    } else {
      this.saveError.set('Não foi possível salvar a categoria. Tente novamente.');
    }
    this.saving.set(false);
  }
}
