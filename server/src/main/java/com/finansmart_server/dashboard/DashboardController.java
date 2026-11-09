package com.finansmart_server.dashboard;

import java.time.YearMonth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finansmart_server.dashboard.dto.DashboardResponse;

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController {

	private final DashboardService dashboard;

	DashboardController(DashboardService dashboard) {
		this.dashboard = dashboard;
	}

	/** O mês vem do cliente porque "mês atual" depende do fuso horário do usuário. */
	@GetMapping
	DashboardResponse get(@RequestParam(required = false) YearMonth month) {
		return this.dashboard.get((month != null) ? month : YearMonth.now());
	}

}
