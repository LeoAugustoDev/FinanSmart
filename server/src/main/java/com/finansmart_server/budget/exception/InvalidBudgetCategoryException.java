package com.finansmart_server.budget.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidBudgetCategoryException extends ErrorResponseException {

	public InvalidBudgetCategoryException(String message) {
		super(HttpStatus.BAD_REQUEST, problem(message), null);
	}

	private static ProblemDetail problem(String message) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
		problem.setProperty("errors", Map.of("categoryId", message));
		return problem;
	}

}
