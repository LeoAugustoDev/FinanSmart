package com.finansmart_server.transaction.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class TransactionNotFoundException extends ErrorResponseException {

	public TransactionNotFoundException() {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Lançamento não encontrado"),
				null);
	}

}
