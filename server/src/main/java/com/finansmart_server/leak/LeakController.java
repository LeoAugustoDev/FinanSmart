package com.finansmart_server.leak;

import java.time.YearMonth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finansmart_server.leak.dto.LeakResponse;

@RestController
@RequestMapping("/api/v1/leaks")
class LeakController {

	private final LeakService leaks;

	LeakController(LeakService leaks) {
		this.leaks = leaks;
	}

	/** O mês vem do cliente porque "mês atual" depende do fuso horário do usuário. */
	@GetMapping
	LeakResponse detect(@RequestParam(required = false) YearMonth month) {
		return this.leaks.detect((month != null) ? month : YearMonth.now());
	}

}
