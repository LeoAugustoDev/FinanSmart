package com.finansmart_server.category.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class CategoryNotFoundException extends ErrorResponseException {

	public CategoryNotFoundException() {
		super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Categoria não encontrada"),
				null);
	}

}
