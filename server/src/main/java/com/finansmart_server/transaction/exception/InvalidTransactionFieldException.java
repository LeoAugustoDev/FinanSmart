package com.finansmart_server.transaction.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Campo válido em formato, mas recusado pelas regras de negócio. */
public class InvalidTransactionFieldException extends ErrorResponseException {

	public InvalidTransactionFieldException(String field, String message) {
		super(HttpStatus.BAD_REQUEST, problem(field, message), null);
	}

	private static ProblemDetail problem(String field, String message) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
		problem.setProperty("errors", Map.of(field, message));
		return problem;
	}

}
