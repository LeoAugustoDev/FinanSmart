import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DecimalInput } from './decimal-input';

@Component({
  imports: [DecimalInput],
  template: `
    <input id="positive" type="number" min="0.01" />
    <input id="free" type="number" />
    <input id="text" type="text" />
  `,
})
class Host {}

describe('DecimalInput', () => {
  let element: HTMLElement;

  beforeEach(async () => {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    element = fixture.nativeElement as HTMLElement;
  });

  function press(id: string, key: string, init: KeyboardEventInit = {}): boolean {
    const event = new KeyboardEvent('keydown', { key, cancelable: true, ...init });
    element.querySelector(`#${id}`)!.dispatchEvent(event);
    return event.defaultPrevented;
  }

  it('barra a letra "e" e o sinal de mais', () => {
    expect(press('positive', 'e')).toBe(true);
    expect(press('positive', 'E')).toBe(true);
    expect(press('positive', '+')).toBe(true);
  });

  it('deixa passar dígitos, separadores e teclas de edição', () => {
    for (const key of ['1', ',', '.', 'Backspace', 'Tab', 'ArrowLeft']) {
      expect(press('positive', key)).toBe(false);
    }
  });

  it('barra o sinal de menos só quando o campo não admite negativos', () => {
    expect(press('positive', '-')).toBe(true);
    expect(press('free', '-')).toBe(false);
  });

  it('não interfere em atalhos do teclado', () => {
    expect(press('positive', 'e', { ctrlKey: true })).toBe(false);
  });

  it('não se aplica a campos de texto', () => {
    expect(press('text', 'e')).toBe(false);
  });

  it('barra texto inserido sem passar pelo teclado', () => {
    const blocked = new InputEvent('beforeinput', { data: 'e', cancelable: true });
    const allowed = new InputEvent('beforeinput', { data: '7', cancelable: true });
    const input = element.querySelector('#positive')!;
    input.dispatchEvent(blocked);
    input.dispatchEvent(allowed);

    expect(blocked.defaultPrevented).toBe(true);
    expect(allowed.defaultPrevented).toBe(false);
  });

  it('barra colagem com notação científica', () => {
    const event = new Event('paste', { cancelable: true });
    Object.defineProperty(event, 'clipboardData', { value: { getData: () => '1e5' } });
    element.querySelector('#positive')!.dispatchEvent(event);

    expect(event.defaultPrevented).toBe(true);
  });
});
