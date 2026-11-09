import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Category } from '../category.model';
import { CategoryService } from '../category.service';
import { CategoryForm } from './category-form';

describe('CategoryForm', () => {
  const service = { get: vi.fn(), create: vi.fn(), update: vi.fn() };

  const category: Category = {
    id: '1',
    name: 'Pets',
    type: 'EXPENSE',
    system: false,
    createdAt: '',
    updatedAt: '',
  };

  function configure(categoryId: string | null): void {
    TestBed.configureTestingModule({
      imports: [CategoryForm],
      providers: [
        provideRouter([]),
        { provide: CategoryService, useValue: service },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap(categoryId ? { id: categoryId } : {}) },
          },
        },
      ],
    });
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  }

  async function render() {
    const fixture = TestBed.createComponent(CategoryForm);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    const typeName = (value: string) => {
      const input = element.querySelector<HTMLInputElement>('#name')!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    const submit = async () => {
      element.querySelector('form')!.dispatchEvent(new Event('submit'));
      fixture.changeDetectorRef.markForCheck();
      await fixture.whenStable();
    };

    return { element, typeName, submit };
  }

  const errorText = (element: HTMLElement) => element.querySelector('.field-error')?.textContent;

  beforeEach(() => {
    vi.resetAllMocks();
    service.create.mockReturnValue(of(category));
    service.update.mockReturnValue(of(category));
  });

  it('não envia sem nome', async () => {
    configure(null);
    const { element, submit } = await render();

    await submit();

    expect(service.create).not.toHaveBeenCalled();
    expect(errorText(element)).toContain('Informe o nome');
  });

  it('rejeita nome com mais de 60 caracteres', async () => {
    configure(null);
    const { element, typeName, submit } = await render();
    typeName('x'.repeat(61));

    await submit();

    expect(service.create).not.toHaveBeenCalled();
    expect(errorText(element)).toContain('até 60');
  });

  it('cria a categoria sem espaços nas pontas e volta para a lista', async () => {
    configure(null);
    const { typeName, submit } = await render();
    typeName('  Pets  ');

    await submit();

    expect(service.create).toHaveBeenCalledWith({ name: 'Pets', type: 'EXPENSE' });
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/categorias']);
  });

  it('mostra o erro 409 no campo nome', async () => {
    configure(null);
    service.create.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    const { element, typeName, submit } = await render();
    typeName('Lazer');

    await submit();

    expect(errorText(element)).toContain('Já existe uma categoria');
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
  });

  it('na edição bloqueia o tipo e envia só o nome', async () => {
    configure('1');
    service.get.mockReturnValue(of(category));
    const { element, typeName, submit } = await render();

    expect(element.querySelector<HTMLSelectElement>('#type')?.disabled).toBe(true);

    typeName('Animais');
    await submit();

    expect(service.update).toHaveBeenCalledWith('1', { name: 'Animais' });
  });

  it('não oferece edição de categoria padrão', async () => {
    configure('1');
    service.get.mockReturnValue(of({ ...category, system: true }));
    const { element } = await render();

    expect(element.querySelector('form')).toBeNull();
    expect(element.querySelector('[role=alert]')?.textContent).toContain('padrão');
  });

  it('oferece nova tentativa quando a categoria não carrega', async () => {
    configure('1');
    service.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    const { element } = await render();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Não foi possível');
    expect(element.querySelector('form')).toBeNull();
  });
});
