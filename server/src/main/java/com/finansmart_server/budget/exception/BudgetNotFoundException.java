package com.finansmart_server.budget.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class BudgetNotFoundException extends ErrorResponseException {

	public BudgetNotFoundException() {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Orçamento não encontrado"),
				null);
	}

}
