package com.finansmart_server.leak.dto;

import java.math.BigDecimal;

/**
 * Cobranças do mês com a mesma descrição, somadas.
 *
 * @param description nulo quando os lançamentos não têm descrição
 * @param recurring se a cobrança se repetiu, com valor parecido, nos dois meses anteriores
 */
public record Charge(String description, String categoryName, BigDecimal amount, boolean recurring) {
}
