package com.finansmart_server.category.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class SystemCategoryModificationException extends ErrorResponseException {

	public SystemCategoryModificationException() {
		super(HttpStatus.FORBIDDEN,
				ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Categorias padrão não podem ser alteradas"),
				null);
	}

}
