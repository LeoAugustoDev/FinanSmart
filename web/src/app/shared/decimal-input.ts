import { Directive, ElementRef, inject } from '@angular/core';

/**
 * Campos numéricos aceitam notação científica ("1e5") por padrão do navegador.
 * Em valores de dinheiro isso só confunde, então barramos "e" e "+",
 * e também o "-" quando o campo não admite negativos.
 */
@Directive({
  selector: 'input[type=number]',
  host: {
    '(keydown)': 'onKeydown($event)',
    '(beforeinput)': 'onBeforeInput($event)',
    '(paste)': 'onPaste($event)',
  },
})
export class DecimalInput {
  private readonly input = inject<ElementRef<HTMLInputElement>>(ElementRef).nativeElement;

  protected onKeydown(event: KeyboardEvent): void {
    if (event.ctrlKey || event.metaKey || event.altKey) {
      return;
    }
    if (this.isBlocked(event.key)) {
      event.preventDefault();
    }
  }

  // Cobre o que não passa pelo teclado físico: teclado virtual, arrastar e soltar, ditado.
  protected onBeforeInput(event: InputEvent): void {
    if (this.hasBlocked(event.data ?? '')) {
      event.preventDefault();
    }
  }

  protected onPaste(event: ClipboardEvent): void {
    if (this.hasBlocked(event.clipboardData?.getData('text') ?? '')) {
      event.preventDefault();
    }
  }

  private hasBlocked(text: string): boolean {
    return Array.from(text).some((char) => this.isBlocked(char));
  }

  private isBlocked(key: string): boolean {
    if (key === 'e' || key === 'E' || key === '+') {
      return true;
    }
    return key === '-' && !this.allowsNegative();
  }

  private allowsNegative(): boolean {
    const min = this.input.min;
    return min === '' || Number(min) < 0;
  }
}
