package com.finansmart_server.leak.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Categoria que gastou acima da própria média.
 *
 * @param average média dos meses com gasto entre os três anteriores
 * @param excess quanto o gasto do mês passou da média
 */
public record Overspend(UUID categoryId, String categoryName, BigDecimal spent, BigDecimal average,
		BigDecimal excess) {
}
