package com.finansmart_server.report.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor de um mês comparado com o do mês anterior.
 *
 * @param change diferença entre o mês e o anterior
 * @param changePercent variação sobre o valor anterior, com uma casa decimal;
 * {@code null} quando o mês anterior é zero, porque não há base para o percentual
 */
public record Comparison(BigDecimal current, BigDecimal previous, BigDecimal change, BigDecimal changePercent) {

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	public static Comparison of(BigDecimal current, BigDecimal previous) {
		BigDecimal change = current.subtract(previous);
		// O resultado do mês pode ser negativo; a base do percentual é o valor absoluto
		// para que o sinal acompanhe a direção da mudança.
		BigDecimal changePercent = (previous.signum() == 0) ? null
				: change.multiply(HUNDRED).divide(previous.abs(), 1, RoundingMode.HALF_UP);
		return new Comparison(current, previous, change, changePercent);
	}

}
