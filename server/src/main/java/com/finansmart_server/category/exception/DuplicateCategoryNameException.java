package com.finansmart_server.category.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class DuplicateCategoryNameException extends ErrorResponseException {

	private static final String MESSAGE = "Já existe uma categoria desse tipo com esse nome";

	public DuplicateCategoryNameException() {
		super(HttpStatus.CONFLICT, problem(), null);
	}

	private static ProblemDetail problem() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, MESSAGE);
		problem.setProperty("errors", Map.of("name", MESSAGE));
		return problem;
	}

}
