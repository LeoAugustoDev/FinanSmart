package com.finansmart_server.leak.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Vazamentos de um mês.
 *
 * @param total soma dos três detectores; nenhum lançamento entra em mais de um
 */
public record LeakResponse(YearMonth month, BigDecimal total, ChargeGroup subscriptions, ChargeGroup fees,
		OverspendGroup aboveAverage) {
}
