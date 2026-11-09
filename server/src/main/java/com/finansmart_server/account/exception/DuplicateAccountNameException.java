package com.finansmart_server.account.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class DuplicateAccountNameException extends ErrorResponseException {

	private static final String MESSAGE = "Já existe uma conta ativa com esse nome";

	public DuplicateAccountNameException() {
		super(HttpStatus.CONFLICT, problem(), null);
	}

	private static ProblemDetail problem() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, MESSAGE);
		problem.setProperty("errors", Map.of("name", MESSAGE));
		return problem;
	}

}
