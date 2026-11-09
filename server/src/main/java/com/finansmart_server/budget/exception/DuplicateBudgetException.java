package com.finansmart_server.budget.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class DuplicateBudgetException extends ErrorResponseException {

	private static final String MESSAGE = "Já existe um orçamento dessa categoria neste mês";

	public DuplicateBudgetException() {
		super(HttpStatus.CONFLICT, problem(), null);
	}

	private static ProblemDetail problem() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, MESSAGE);
		problem.setProperty("errors", Map.of("categoryId", MESSAGE));
		return problem;
	}

}
