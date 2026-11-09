import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { Account } from '../account.model';
import { AccountService } from '../account.service';
import { AccountForm } from './account-form';

describe('AccountForm', () => {
  const service = { get: vi.fn(), create: vi.fn(), update: vi.fn() };

  const account: Account = {
    id: '1',
    name: 'Nubank',
    type: 'SAVINGS',
    initialBalance: 1500,
    currency: 'BRL',
    archived: false,
    createdAt: '',
    updatedAt: '',
  };

  function configure(accountId: string | null): void {
    TestBed.configureTestingModule({
      imports: [AccountForm],
      providers: [
        provideRouter([]),
        { provide: AccountService, useValue: service },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap(accountId ? { id: accountId } : {}) },
          },
        },
      ],
    });
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  }

  async function render() {
    const fixture = TestBed.createComponent(AccountForm);
    await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;

    const type = (selector: string, value: string) => {
      const input = element.querySelector<HTMLInputElement>(selector)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    const submit = async () => {
      element.querySelector('form')!.dispatchEvent(new Event('submit'));
      fixture.changeDetectorRef.markForCheck();
      await fixture.whenStable();
    };

    return { element, type, submit };
  }

  const errorText = (element: HTMLElement) => element.querySelector('.field-error')?.textContent;

  beforeEach(() => {
    vi.resetAllMocks();
    service.create.mockReturnValue(of(account));
    service.update.mockReturnValue(of(account));
  });

  it('não envia o formulário vazio e mostra os erros', async () => {
    configure(null);
    const { element, submit } = await render();

    await submit();

    expect(service.create).not.toHaveBeenCalled();
    expect(element.querySelectorAll('.field-error').length).toBe(2);
  });

  it('rejeita nome só com espaços', async () => {
    configure(null);
    const { element, type, submit } = await render();
    type('#name', '   ');
    type('#initialBalance', '10');

    await submit();

    expect(service.create).not.toHaveBeenCalled();
    expect(errorText(element)).toContain('Informe o nome');
  });

  it('rejeita nome com mais de 60 caracteres', async () => {
    configure(null);
    const { element, type, submit } = await render();
    type('#name', 'x'.repeat(61));
    type('#initialBalance', '10');

    await submit();

    expect(service.create).not.toHaveBeenCalled();
    expect(errorText(element)).toContain('até 60');
  });

  it('rejeita saldo com mais de 2 casas decimais', async () => {
    configure(null);
    const { element, type, submit } = await render();
    type('#name', 'Nubank');
    type('#initialBalance', '1.999');

    await submit();

    expect(service.create).not.toHaveBeenCalled();
    expect(errorText(element)).toContain('2 casas');
  });

  it('cria a conta sem espaços nas pontas e volta para a lista', async () => {
    configure(null);
    const { type, submit } = await render();
    type('#name', '  Nubank  ');
    type('#initialBalance', '-10.5');

    await submit();

    expect(service.create).toHaveBeenCalledWith({
      name: 'Nubank',
      type: 'CHECKING',
      initialBalance: -10.5,
    });
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/contas']);
  });

  it('mostra o erro 409 no campo nome', async () => {
    configure(null);
    service.create.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    const { element, type, submit } = await render();
    type('#name', 'Nubank');
    type('#initialBalance', '10');

    await submit();

    expect(errorText(element)).toContain('Já existe uma conta');
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
  });

  it('mostra erro geral nas demais falhas', async () => {
    configure(null);
    service.create.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    const { element, type, submit } = await render();
    type('#name', 'Nubank');
    type('#initialBalance', '10');

    await submit();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Não foi possível salvar');
  });

  it('carrega e atualiza uma conta existente', async () => {
    configure('1');
    service.get.mockReturnValue(of(account));
    const { element, type, submit } = await render();

    expect(element.querySelector<HTMLInputElement>('#name')?.value).toBe('Nubank');

    type('#name', 'Nu Conta');
    await submit();

    expect(service.update).toHaveBeenCalledWith('1', {
      name: 'Nu Conta',
      type: 'SAVINGS',
      initialBalance: 1500,
    });
  });

  it('oferece nova tentativa quando a conta não carrega', async () => {
    configure('1');
    service.get.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    const { element } = await render();

    expect(element.querySelector('[role=alert]')?.textContent).toContain('Não foi possível');
    expect(element.querySelector('form')).toBeNull();
  });
});
