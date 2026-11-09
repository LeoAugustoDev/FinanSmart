import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { CATEGORY_TYPES, CATEGORY_TYPE_LABELS, Category, CategoryType } from '../category.model';
import { CategoryService } from '../category.service';

@Component({
  selector: 'app-category-list',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './category-list.html',
})
export class CategoryList {
  private readonly service = inject(CategoryService);

  protected readonly types = CATEGORY_TYPES;
  protected readonly typeLabels = CATEGORY_TYPE_LABELS;
  protected readonly typeFilter = signal<CategoryType | undefined>(undefined);
  protected readonly actionError = signal<string | null>(null);

  protected readonly categories = rxResource({
    params: () => ({ type: this.typeFilter() }),
    stream: ({ params }) => this.service.list(params.type),
    defaultValue: [] as Category[],
  });

  protected changeFilter(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.actionError.set(null);
    this.typeFilter.set(value ? (value as CategoryType) : undefined);
  }

  protected remove(category: Category): void {
    if (!window.confirm(`Excluir a categoria "${category.name}"? Essa ação não pode ser desfeita.`)) {
      return;
    }

    this.actionError.set(null);
    this.service.delete(category.id).subscribe({
      next: () => this.categories.reload(),
      error: (error: HttpErrorResponse) => {
        this.actionError.set(
          error.status === 409
            ? 'Esta categoria está em uso por lançamentos e não pode ser excluída.'
            : 'Não foi possível excluir a categoria.',
        );
        this.categories.reload();
      },
    });
  }
}
