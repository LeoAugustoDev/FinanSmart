package com.finansmart_server.leak.dto;

import java.math.BigDecimal;
import java.util.List;

public record OverspendGroup(BigDecimal total, List<Overspend> items) {
}
