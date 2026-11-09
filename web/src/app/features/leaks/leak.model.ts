export interface Charge {
  /** Nulo quando os lançamentos não têm descrição. */
  description: string | null;
  categoryName: string;
  amount: number;
  /** Repetiu-se, com valor parecido, nos dois meses anteriores. */
  recurring: boolean;
}

export interface ChargeGroup {
  total: number;
  items: Charge[];
}

export interface Overspend {
  categoryId: string;
  categoryName: string;
  spent: number;
  /** Média dos meses com gasto entre os três anteriores. */
  average: number;
  excess: number;
}

export interface OverspendGroup {
  total: number;
  items: Overspend[];
}

export interface Leaks {
  month: string;
  total: number;
  subscriptions: ChargeGroup;
  fees: ChargeGroup;
  aboveAverage: OverspendGroup;
}
