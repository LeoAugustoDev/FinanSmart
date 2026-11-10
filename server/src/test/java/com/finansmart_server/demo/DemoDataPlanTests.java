package com.finansmart_server.demo;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DemoDataPlanTests {
    @ParameterizedTest
    @ValueSource(strings = {"2026-01-01", "2024-02-29", "2026-11-09", "2026-12-31"})
    void coversSixMonthsWithoutFutureDates(String date) {
        LocalDate today = LocalDate.parse(date);
        var plan = DemoDataPlan.build(today);
        assertThat(plan.accounts()).hasSize(3);
        assertThat(plan.budgets()).hasSize(8);
        assertThat(plan.entries().stream().map(e -> YearMonth.from(e.date())).distinct()).hasSize(6);
        assertThat(plan.entries()).allSatisfy(e -> {
            assertThat(e.date()).isBeforeOrEqualTo(today);
            assertThat(e.amount()).isPositive();
            assertThat(e.amount().scale()).isLessThanOrEqualTo(2);
        });
        assertThat(DemoDataPlan.build(today).entries()).isEqualTo(plan.entries());
    }
}