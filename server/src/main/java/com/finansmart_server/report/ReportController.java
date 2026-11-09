package com.finansmart_server.report;

import java.time.YearMonth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finansmart_server.report.dto.MonthlyReportResponse;

@RestController
@RequestMapping("/api/v1/reports")
class ReportController {

	private final ReportService reports;

	ReportController(ReportService reports) {
		this.reports = reports;
	}

	/** O mês vem do cliente porque "mês atual" depende do fuso horário do usuário. */
	@GetMapping("/monthly")
	MonthlyReportResponse monthly(@RequestParam(required = false) YearMonth month) {
		return this.reports.monthly((month != null) ? month : YearMonth.now());
	}

}
