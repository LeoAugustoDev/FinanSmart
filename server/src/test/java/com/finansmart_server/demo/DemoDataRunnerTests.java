package com.finansmart_server.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DemoDataRunnerTests {
    private final DemoDataSeeder seeder = mock(DemoDataSeeder.class);
    private final ApplicationContextRunner context = new ApplicationContextRunner()
        .withBean(DemoDataSeeder.class, () -> this.seeder)
        .withUserConfiguration(DemoDataRunner.class);

    @Test void runsOnlyWhenDemoAndEnabled() {
        context.withPropertyValues("spring.profiles.active=demo", "app.demo.seed-enabled=true").run(ctx -> {
            assertThat(ctx).hasSingleBean(DemoDataRunner.class);
            ctx.getBean(DemoDataRunner.class).run(new DefaultApplicationArguments());
            verify(seeder).seed(any(LocalDate.class));
        });
    }

    @Test void disabledPropertySkipsRunner() {
        context.withPropertyValues("spring.profiles.active=demo", "app.demo.seed-enabled=false").run(ctx ->
            assertThat(ctx).doesNotHaveBean(DemoDataRunner.class));
        verifyNoInteractions(seeder);
    }

    @Test void enabledPropertyDoesNotActivateDemoInDev() {
        context.withPropertyValues("spring.profiles.active=dev", "app.demo.seed-enabled=true").run(ctx ->
            assertThat(ctx).doesNotHaveBean(DemoDataRunner.class));
        verifyNoInteractions(seeder);
    }
}