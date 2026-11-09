package com.finansmart_server;

import org.springframework.boot.SpringApplication;

public class TestFinansmartServerApplication {

	public static void main(String[] args) {
		SpringApplication.from(FinansmartServerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
