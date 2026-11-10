package com.finansmart_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.greaterThan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.finansmart_server.account.*;
import com.finansmart_server.demo.DemoDataSeeder;

@SpringBootTest(properties = {"app.demo.seed-enabled=false", "DB_URL=jdbc:postgresql://localhost/unused",
    "DB_USERNAME=unused", "DB_PASSWORD=unused", "KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/finansmart",
    "CORS_ALLOWED_ORIGINS=http://localhost:4200"})
@ActiveProfiles("demo")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class DemoDataIntegrationTests {
    private static final String USER = "7d9f3c1e-5a2b-4e8f-9c6d-1b2a3c4d5e6f";
    @Autowired DemoDataSeeder seeder;
    @Autowired AccountRepository accounts;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"2026-01-01", "2024-02-29", "2026-11-09"})
    void seedsSixMonthsAndExercisesAllDetectors(String date) throws Exception {
        LocalDate today = LocalDate.parse(date);
        int count = seeder.seed(today);
        assertThat(count).isGreaterThan(150);
        assertThat(accounts.findByUserIdOrderByNameAsc(USER)).hasSize(3);
        // Flush JPA changes via the API queries before querying SQL directly.
        mvc.perform(get("/api/v1/leaks").param("month", today.toString().substring(0, 7)).with(jwt().jwt(t -> t.subject(USER))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subscriptions.total", greaterThan(0.0)))
            .andExpect(jsonPath("$.fees.total", greaterThan(0.0)))
            .andExpect(jsonPath("$.aboveAverage.total", greaterThan(0.0)));
        assertThat(jdbc.queryForObject("select count(distinct date_trunc('month', occurred_on)) from transactions where user_id=?", Integer.class, USER)).isEqualTo(6);
        assertThat(jdbc.queryForObject("select count(*) from budgets where user_id=?", Integer.class, USER)).isEqualTo(8);
        mvc.perform(get("/api/v1/leaks").param("month", today.toString().substring(0, 7)).with(jwt().jwt(t -> t.subject("another-user"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void repeatedStartupPreservesIdsEditsAndOtherUsers() {
        String other = UUID.randomUUID().toString();
        Account personal = accounts.saveAndFlush(new Account(other, "Minha conta", AccountType.WALLET, BigDecimal.TEN));
        seeder.seed(LocalDate.of(2026, 11, 9));
        var first = accounts.findByUserIdOrderByNameAsc(USER);
        first.get(0).archive();
        accounts.saveAndFlush(first.get(0));
        assertThat(seeder.seed(LocalDate.of(2026, 12, 9))).isZero();
        assertThat(accounts.findByUserIdOrderByNameAsc(USER)).extracting(Account::getId)
            .containsExactlyElementsOf(first.stream().map(Account::getId).toList());
        assertThat(accounts.findById(personal.getId())).isPresent();
        assertThat(accounts.findById(first.get(0).getId()).orElseThrow().isArchived()).isTrue();
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void refusesToOverwriteExistingDemoData() {
        Account existing = accounts.saveAndFlush(new Account(USER, "Existente", AccountType.WALLET, BigDecimal.ONE));
        try {
            assertThatThrownBy(() -> seeder.seed(LocalDate.of(2026, 11, 9))).isInstanceOf(IllegalStateException.class);
            assertThat(accounts.findById(existing.getId())).isPresent();
            assertThat(jdbc.queryForObject("select count(*) from demo_seed_runs where user_id=?", Integer.class, USER)).isZero();
        } finally {
            accounts.deleteById(existing.getId());
        }
    }
}