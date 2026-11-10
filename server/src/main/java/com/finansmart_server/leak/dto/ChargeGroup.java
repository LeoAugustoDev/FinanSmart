package com.finansmart_server.leak.dto;

import java.math.BigDecimal;
import java.util.List;

public record ChargeGroup(BigDecimal total, List<Charge> items) {
}
