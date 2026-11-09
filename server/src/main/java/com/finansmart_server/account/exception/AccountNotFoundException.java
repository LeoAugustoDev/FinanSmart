package com.finansmart_server.account.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class AccountNotFoundException extends ErrorResponseException {

	public AccountNotFoundException() {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Conta não encontrada"),
				null);
	}

}
