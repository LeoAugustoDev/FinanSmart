package com.finansmart_server.demo;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

/**
 * Carrega os dados de demonstração na primeira inicialização. Só existe com
 * {@code app.demo.seed-enabled=true}, que o perfil demo liga.
 */
@Component
@Profile("demo")
@ConditionalOnProperty(name = "app.demo.seed-enabled", havingValue = "true")
class DemoDataRunner implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(DemoDataRunner.class);

	private final DemoDataSeeder seeder;

	DemoDataRunner(DemoDataSeeder seeder) {
		this.seeder = seeder;
	}

	@Override
	public void run(ApplicationArguments args) {
		int created = this.seeder.seed(LocalDate.now());
		logger.info("Carga demo concluída: {} lançamentos criados", created);
	}

}
